
import MetricsMocks.counter
import MetricsMocks.summary
import MetricsMocks.summaryChild
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nhaarman.mockitokotlin2.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.ToleranceMatcher
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.prometheus.client.Counter
import io.prometheus.client.Summary
import kotlinx.coroutines.runBlocking
import org.apache.hadoop.hbase.util.Bytes
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import java.io.IOException
import java.sql.Connection
import java.sql.PreparedStatement
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ListProcessorTest : StringSpec() {

    init {
        "Only commits offsets on success, resets position on failure" {
            val batchTimer = mock<Summary.Timer>()

            val batchSummaryChild = mock<Summary.Child> {
                on { startTimer() } doReturn batchTimer
            }

            val batchSummary = mock<Summary> {
                on { labels(any(), any()) } doReturn batchSummaryChild
            }

            val batchFailuresChild = mock<Counter.Child>()
            val batchFailures = mock<Counter> {
                on { labels(any()) } doReturn batchFailuresChild
            }

            val recordSuccessesChild: Counter.Child = mock()
            val recordSuccesses = mock<Counter> {
                on { labels(any()) } doReturn recordSuccessesChild
            }

            val recordFailuresChild: Counter.Child = mock()
            val recordFailures = mock<Counter> {
                on { labels(any()) } doReturn recordFailuresChild
            }

            val processor = ListProcessor(mock(), Converter(), mock(), mock(), mock(),
                batchSummary, batchFailures, recordSuccesses, recordFailures)

            val hbaseClient = hbaseClient()
            val metadataStoreClient = metadataStoreClient()
            val consumer = kafkaConsumer()
            val s3Service = corporateStorageService()
            val manifestService = manifestService()
            processor.processRecords(hbaseClient, consumer, metadataStoreClient, s3Service, manifestService, messageParser(), consumerRecords())
            verifyBatchSummaryInteractions(batchSummary, batchSummaryChild, batchTimer)
            verifyBatchFailureInteractions(batchFailures, batchFailuresChild)
            verifyRecordsSuccessesInteractions(recordSuccesses, recordSuccessesChild)
            verifyRecordsFailuresInteractions(recordFailures, recordFailuresChild)
            verifyS3Interactions(s3Service)
            verifyHbaseInteractions(hbaseClient)
            verifyMetadataStoreInteractions(metadataStoreClient)
            verifyKafkaInteractions(consumer)
        }
    }

    private fun verifyBatchSummaryInteractions(summary: Summary,
                                               summaryChild: Summary.Child,
                                               summaryTimer: Summary.Timer) {
        val summaryTopicCaptor = argumentCaptor<String>()
        val summaryPartitionCaptor = argumentCaptor<String>()
        verify(summary, times(10)).labels(summaryTopicCaptor.capture(), summaryPartitionCaptor.capture())

        summaryTopicCaptor.allValues.forEachIndexed { index, topic ->
            topic shouldBe "db.database%02d.collection%02d".format(index + 1, index + 1)
        }

        summaryPartitionCaptor.allValues.forEachIndexed { index, partition ->
            partition shouldBe "${10 - (index + 1)}"
        }

        verifyNoMoreInteractions(summary)

        verify(summaryChild, times(10)).startTimer()
        verifyNoMoreInteractions(summaryChild)
        verify(summaryTimer, times(5)).observeDuration()
        verifyNoMoreInteractions(summaryTimer)
    }

    private fun verifyBatchFailureInteractions(failureCounter: Counter, child: Counter.Child) {
        val failedBatchTopicCaptor = argumentCaptor<String>()
        val failedBatchPartitionCaptor = argumentCaptor<String>()
        verify(failureCounter, times(5)).labels(failedBatchTopicCaptor.capture(), failedBatchPartitionCaptor.capture())

        failedBatchTopicCaptor.allValues.forEachIndexed { index, topic ->
            topic shouldBe "db.database%02d.collection%02d".format(index * 2 + 1, index * 2 + 1)
        }

        failedBatchPartitionCaptor.allValues.forEachIndexed { index, partition ->
            partition shouldBe "${9 - index * 2}"
        }

        verifyNoMoreInteractions(failureCounter)
        verify(child, times(5)).inc()
        verifyNoMoreInteractions(child)
    }

    private fun verifyRecordsSuccessesInteractions(recordSuccesses: Counter,
                                                   recordSuccessesChild: Counter.Child) {

        val successTopicCaptor = argumentCaptor<String>()
        val successPartitionCaptor = argumentCaptor<String>()
        verify(recordSuccesses, times(5)).labels(successTopicCaptor.capture(), successPartitionCaptor.capture())

        successTopicCaptor.allValues.forEachIndexed { index, topic ->
            val topicIndex = index * 2 + 2
            topic shouldBe "db.database%02d.collection%02d".format(topicIndex, topicIndex)
        }

        successPartitionCaptor.allValues.forEachIndexed { index, partition ->
            partition shouldBe "${10 - ((index + 1) * 2)}"
        }

        verifyNoMoreInteractions(recordSuccesses)
        argumentCaptor<Double> {
            verify(recordSuccessesChild, times(5)).inc(capture())
            allValues.forEach {
                it shouldBe ToleranceMatcher(100.toDouble(), 0.5)
            }
            verifyNoMoreInteractions(recordSuccessesChild)
        }

    }

    private fun verifyRecordsFailuresInteractions(recordFailures: Counter,
                                                  recordFailuresChild: Counter.Child) {

        val failureTopicCaptor = argumentCaptor<String>()
        val failurePartitionCaptor = argumentCaptor<String>()
        verify(recordFailures, times(5)).labels(failureTopicCaptor.capture(), failurePartitionCaptor.capture())

        failureTopicCaptor.allValues.forEachIndexed { index, topic ->
            val topicIndex = index * 2 + 1
            topic shouldBe "db.database%02d.collection%02d".format(topicIndex, topicIndex)
        }

        failurePartitionCaptor.allValues.forEachIndexed { index, partition ->
            partition shouldBe "${10 - (index * 2 + 1)}"
        }

        verifyNoMoreInteractions(recordFailures)
        argumentCaptor<Double> {
            verify(recordFailuresChild, times(5)).inc(capture())
            allValues.forEach {
                it shouldBe ToleranceMatcher(100.toDouble(), 0.5)
            }
            verifyNoMoreInteractions(recordFailuresChild)
        }
    }

    private suspend fun verifyMetadataStoreInteractions(metadataStoreClient: MetadataStoreClient) {
        val captor = argumentCaptor<List<HbasePayload>>()
        verify(metadataStoreClient, times(10)).recordBatch(captor.capture())
        validateMetadataHbasePayloads(captor)
    }

    private fun verifyS3Interactions(s3Service: CorporateStorageService) = runBlocking {
        val tableCaptor = argumentCaptor<String>()
        val payloadCaptor = argumentCaptor<List<HbasePayload>>()
        verify(s3Service, times(10)).putBatch(tableCaptor.capture(), payloadCaptor.capture())
        validateTableNames(tableCaptor)
        validateHbasePayloads(payloadCaptor)
    }


    private fun verifyHbaseInteractions(hbaseClient: HbaseClient) {
        verifyHBasePuts(hbaseClient)
        verifyNoMoreInteractions(hbaseClient)
    }

    private fun verifyHBasePuts(hbaseClient: HbaseClient) = runBlocking {
        val tableNameCaptor = argumentCaptor<String>()
        val recordCaptor = argumentCaptor<List<HbasePayload>>()
        verify(hbaseClient, times(10)).putList(tableNameCaptor.capture(), recordCaptor.capture())
        validateTableNames(tableNameCaptor)
        validateHbasePayloads(recordCaptor)
    }

    private fun validateHbasePayloads(captor: KArgumentCaptor<List<HbasePayload>>) {
        captor.allValues.size shouldBe 10
        captor.allValues.forEachIndexed { payloadsNo, payloads ->
            payloads.size shouldBe 100
            payloads.forEachIndexed { index, payload ->
                String(payload.key).toInt() shouldBe index + ((payloadsNo) * 100)
                val body = Gson().fromJson(String(payload.body), JsonObject::class.java)
                val putTime = body["put_time"].asJsonPrimitive.asString
                putTime shouldNotBe null
                String(payload.body) shouldBe hbaseBody(index, putTime)
                payload.record.partition() shouldBe (index + 1) % 20
                payload.record.offset() shouldBe ((payloadsNo + 1) * (index + 1)) * 20
            }
        }
    }

    private fun validateMetadataHbasePayloads(captor: KArgumentCaptor<List<HbasePayload>>) {
        captor.allValues.size shouldBe 10
        captor.allValues.forEachIndexed { payloadsNo, payloads ->
            payloads.size shouldBe 100
            payloads.forEachIndexed { index, payload ->
                String(payload.key).toInt() shouldBe (index) + (payloadsNo * 100)
                val body = Gson().fromJson(String(payload.body), JsonObject::class.java)
                val putTime = body["put_time"].asJsonPrimitive.asString
                putTime shouldNotBe null
                String(payload.body) shouldBe hbaseBody(index, putTime)
                payload.record.partition() shouldBe (index + 1) % 20
                payload.record.offset() shouldBe ((payloadsNo + 1) * (index + 1)) * 20
            }
        }
    }

    private fun validateTableNames(tableCaptor: KArgumentCaptor<String>) {
        tableCaptor.allValues.forEachIndexed { index, tableName ->
            tableName shouldBe tableName(index + 1)
        }
    }



    private fun verifyKafkaInteractions(consumer: KafkaConsumer<ByteArray, ByteArray>) {
        verifySuccesses(consumer)
        verifyFailures(consumer)
        verifyNoMoreInteractions(consumer)
    }

    private fun verifyFailures(consumer: KafkaConsumer<ByteArray, ByteArray>) {
        argumentCaptor<Set<TopicPartition>> {
            verify(consumer, times(5)).committed(capture())
            allValues.forEachIndexed { index, topicPartitionSet ->
                val topicNumber = (index * 2 + 1)
                topicPartitionSet shouldContainExactly setOf(TopicPartition(topicName(topicNumber), 10 - topicNumber))
            }
        }

        val positionCaptor = argumentCaptor<Long>()
        val topicPartitionCaptor = argumentCaptor<TopicPartition>()
        verify(consumer, times(5)).seek(topicPartitionCaptor.capture(), positionCaptor.capture())
        topicPartitionCaptor.allValues.zip(positionCaptor.allValues).forEachIndexed { index, pair ->
            val topicNumber = index * 2 + 1
            val topicPartition = pair.first
            val position = pair.second
            val topic = topicPartition.topic()
            val partition = topicPartition.partition()
            topic shouldBe topicName(topicNumber)
            partition shouldBe 10 - topicNumber
            position shouldBe topicNumber * 10
        }
    }

    private fun verifySuccesses(consumer: KafkaConsumer<ByteArray, ByteArray>) {
        val commitCaptor = argumentCaptor<Map<TopicPartition, OffsetAndMetadata>>()
        verify(consumer, times(5)).commitSync(commitCaptor.capture())
        commitCaptor.allValues.forEachIndexed { index, element ->
            val topicNumber = (index + 1) * 2
            element.size shouldBe 1
            val topicPartition = TopicPartition(topicName(topicNumber), 10 - topicNumber)
            element[topicPartition] shouldNotBe null
            element[topicPartition]?.offset() shouldBe (topicNumber * 20 * 100) + 1
        }
    }

    private fun messageParser() =
            mock<MessageParser> {
                val hbaseKeys = (0..1000000).map { Pair("id", Bytes.toBytes("$it")) }
                on { generateKeyFromRecordBody(any()) } doReturnConsecutively hbaseKeys
            }

    private fun kafkaConsumer() =
            mock<KafkaConsumer<ByteArray, ByteArray>> {
                repeat(10) { topicNumber ->
                    on {
                        committed(setOf(TopicPartition(topicName(topicNumber), 10 - topicNumber)))
                    } doReturn mapOf(TopicPartition(topicName(topicNumber), 10 - topicNumber) to OffsetAndMetadata((topicNumber * 10).toLong(), ""))
                }
            }

    private fun hbaseClient() =
        mock<HbaseClient> {
            onBlocking { putList(any(), any()) } doAnswer {
                val tableName = it.getArgument<String>(0)
                val matchResult = Regex("""[13579]$""").find(tableName)
                if (matchResult != null) {
                    throw IOException("Table: '$tableName'.")
                }
            }
        }

    private fun metadataStoreClient(): MetadataStoreClient {
        val statement = mock<PreparedStatement>()
        val connection = mock<Connection> {
            on {prepareStatement(any())} doReturn statement
        }

        val successChild = summaryChild()
        val successTimer = summary(successChild)

        val retryChild = mock<Counter.Child>()
        val retryCounter = counter(retryChild)
        val failureChild = mock<Counter.Child>()
        val failureCounter = counter(failureChild)

        return spy(MetadataStoreClient(connection, successTimer, retryCounter, failureCounter))
    }

    private fun consumerRecords()  =
            ConsumerRecords((1..10).associate { topicNumber ->
                TopicPartition(topicName(topicNumber), 10 - topicNumber) to (1..100).map { recordNumber ->
                    val body = Bytes.toBytes(json(recordNumber))
                    val key = Bytes.toBytes("${topicNumber + recordNumber}")
                    val offset = (topicNumber * recordNumber * 20).toLong()
                    mock<ConsumerRecord<ByteArray, ByteArray>> {
                        on { topic() } doReturn topicName(topicNumber)
                        on { value() } doReturn body
                        on { key() } doReturn key
                        on { offset() } doReturn offset
                        on { partition() } doReturn recordNumber % 20
                    }
                }
            })

    private fun corporateStorageService(): CorporateStorageService = mock { on { runBlocking { putBatch(any(), any()) } } doAnswer { } }
    private fun manifestService(): ManifestService = mock { on { runBlocking { putManifestFile(any()) } } doAnswer { } }
    
    private fun json(id: Any) = """{ "message": { "_id": { "id": "$id" } } }"""
    private fun topicName(topicNumber: Int) = "db.database%02d.collection%02d".format(topicNumber, topicNumber)
    private fun hbaseBody(index: Int, putTime: String = "") =
            """{"message":{"_id":{"id":"${(index % 100) + 1}"},"timestamp_created_from":"epoch"}""" + (if (putTime.isNotBlank()) ""","put_time":"$putTime"""" else "") + "}"
    private fun tableName(tableNumber: Int) =  "database%02d:collection%02d".format(tableNumber, tableNumber)
}
