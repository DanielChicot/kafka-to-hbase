import com.beust.klaxon.JsonObject
import io.prometheus.client.Counter
import io.prometheus.client.Summary
import kotlinx.coroutines.*
import org.apache.hadoop.hbase.util.Bytes
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import uk.gov.dwp.dataworks.logging.DataworksLogger
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ListProcessor(validator: Validator,
                    private val converter: Converter,
                    dlqTimer: Summary,
                    dlqRetries: Counter,
                    dlqFailures: Counter,
                    private val batchTimer: Summary,
                    private val batchFailures: Counter,
                    private val recordSuccesses: Counter,
                    private val recordFailures: Counter):
    BaseProcessor(validator, converter, dlqTimer, dlqRetries, dlqFailures) {

    fun processRecords(hbase: HbaseClient,
                       consumer: KafkaConsumer<ByteArray, ByteArray>,
                       metadataClient: MetadataStoreClient,
                       s3Service: CorporateStorageService,
                       manifestService: ManifestService,
                       parser: MessageParser,
                       records: ConsumerRecords<ByteArray, ByteArray>) {
        try {
            runBlocking {
                val successfulPayloads = mutableListOf<HbasePayload>()
                records.partitions().forEach { partition ->
                    val timer = batchTimer.labels(partition.topic(), "${partition.partition()}").startTimer()
                    val partitionRecords = records.records(partition)
                    val payloads = payloads(partitionRecords, parser)
                    textUtils.qualifiedTableName(partition.topic())?.let { table ->
                        coroutineScope {
                            val s3Ok = async { putInS3(s3Service, table, payloads) }
                            val hbaseOk = async { putInHbase(hbase, table, payloads) }
                            val metadataStoreOk = async { putInMetadataStore(metadataClient, payloads) }
                            if (s3Ok.await() && hbaseOk.await() && metadataStoreOk.await()) {
                                val lastPosition = lastPosition(partitionRecords)
                                logger.info("Batch succeeded, committing offset", "topic" to partition.topic(),
                                    "partition" to "${partition.partition()}", "offset" to "$lastPosition")
                                consumer.commitSync(mapOf(partition to OffsetAndMetadata(lastPosition + 1)))
                                logSuccessfulPuts(table, payloads)
                                successfulPayloads.addAll(payloads)
                                timer.observeDuration()
                                recordSuccesses.labels(partition.topic(), "${partition.partition()}")
                                    .inc(payloads.size.toDouble())
                            } else {
                                lastCommittedOffset(consumer, partition)?.let { consumer.seek(partition, it) }
                                logger.error("Batch failed, not committing offset, resetting position to last commit",
                                    "topic" to partition.topic(), "partition" to "${partition.partition()}")
                                batchFailures.labels(partition.topic(), "${partition.partition()}").inc()
                                recordFailures.labels(partition.topic(), "${partition.partition()}")
                                    .inc(payloads.size.toDouble())
                                logFailedPuts(table, payloads)
                            }
                        }
                    }
                }
                putManifest(manifestService, successfulPayloads)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun putInMetadataStore(metadataClient: MetadataStoreClient, payloads: List<HbasePayload>) =
        withContext(Dispatchers.IO) {
            try {
                metadataClient.recordBatch(payloads)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("Failed to put batch into metadatastore", e, "error" to (e.message ?: ""))
                false
            }
        }

    private suspend fun putInS3(s3Service: CorporateStorageService, table: String, payloads: List<HbasePayload>) =
        withContext(Dispatchers.IO) {
            try {
                s3Service.putBatch(table, payloads)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("Failed to put batch into s3", e, "error" to (e.message ?: ""))
                false
            }
        }

    private suspend fun putInHbase(hbase: HbaseClient, table: String, payloads: List<HbasePayload>) =
        withContext(Dispatchers.IO) {
            try {
                hbase.putList(table, payloads)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("Failed to put batch into hbase", e, "error" to (e.message ?: ""))
                false
            }
        }


    private fun logFailedPuts(table: String, payloads: List<HbasePayload>) =
        payloads.forEach {
            logger.error("Failed to put record", "table" to table,
                "key" to textUtils.printableKey(it.key),
                "version" to "${it.version}",
                "version_created_from" to it.versionCreatedFrom,
                "version_raw" to it.versionRaw)
        }

    private suspend fun putManifest(manifestService: ManifestService, payloads: List<HbasePayload>) =
        withContext(Dispatchers.IO) {
            try {
                if (Config.Manifest.writeManifests) {
                    manifestService.putManifestFile(payloads)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("Failed to put manifest file in to s3", e, "error" to (e.message ?: ""))
                false
            }
        }


    private fun logSuccessfulPuts(table: String, payloads: List<HbasePayload>) {
        payloads.forEach {
            logger.info("Put record",
                "table" to table,
                "key" to textUtils.printableKey(it.key),
                "version" to "${it.version}",
                "version_created_from" to it.versionCreatedFrom,
                "version_raw" to it.versionRaw,
                "size" to "${it.record.serializedValueSize()}",
                "time_since_last_modified" to "${(it.putTime - it.timeOnQueue) / 1000}")
        }
    }

    private fun lastCommittedOffset(consumer: KafkaConsumer<ByteArray, ByteArray>, partition: TopicPartition): Long? =
        consumer.committed(setOf(partition))?.get(partition)?.offset()

    private fun lastPosition(partitionRecords: MutableList<ConsumerRecord<ByteArray, ByteArray>>) =
        partitionRecords[partitionRecords.size - 1].offset()

    private suspend fun payloads(records: List<ConsumerRecord<ByteArray, ByteArray>>,
                                 parser: MessageParser): List<HbasePayload> =
        records.mapNotNull { record ->
            recordAsJson(record)?.let { json ->
                val (unformattedId, formattedKey) = parser.generateKeyFromRecordBody(json)
                val qualifiedId = unformattedId ?: ""
                if (formattedKey.isNotEmpty()) hbasePayload(json, qualifiedId, formattedKey, record) else null
            }
        }

    private fun hbasePayload(json: JsonObject,
                             unformattedId: String,
                             formattedKey: ByteArray,
                             record: ConsumerRecord<ByteArray, ByteArray>): HbasePayload {
        val (timestamp, source) = converter.getLastModifiedTimestamp(json)
        val message = json["message"] as JsonObject
        message["timestamp_created_from"] = source
        val putTime = Date()
        json["put_time"] = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(putTime)

        val version = converter.getTimestampAsLong(timestamp)
        val timeOnQueue = json.string("timestamp")?.let { converter.getTimestampAsLong(it) } ?: version

        return HbasePayload(formattedKey, Bytes.toBytes(json.toJsonString()), unformattedId, version, source,
            timestamp, record, putTime.time, timeOnQueue)
    }

    companion object {
        private val textUtils = TextUtils()
        private val logger = DataworksLogger.getLogger(ListProcessor::class)
    }
}
