
import Utility.getISO8601Timestamp
import Utility.metadataStoreConnection
import Utility.sampleQualifiedTableName
import Utility.sendRecord
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ListObjectsV2Request
import com.amazonaws.services.s3.model.ListObjectsV2Result
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.client.Table
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.zip.GZIPInputStream
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds

@Suppress("BlockingMethodInNonBlockingContext")
@ExperimentalTime
class Kafka2hbIntegrationLoadSpec : StringSpec() {

    init {
        "Many messages sent to many topics" {
            publishRecords()
            verifyHbase()
            verifyMetadataStore(TOPIC_COUNT * RECORDS_PER_TOPIC, DB_NAME, COLLECTION_NAME)
            verifyS3()
            verifyManifests()
            verifyMetrics()
        }
    }

    private fun publishRecords() {
        val producer = KafkaProducer<ByteArray, ByteArray>(Config.Kafka.producerProps)
        val converter = Converter()
        logger.info("Starting record producer...")
        repeat(TOPIC_COUNT) { collectionNumber ->
            val topic = topicName(collectionNumber)
            val excludedTopic = excludedTopicName(collectionNumber)
            repeat(RECORDS_PER_TOPIC) { messageNumber ->
                val timestamp = converter.getTimestampAsLong(getISO8601Timestamp())
                logger.debug("Sending record $messageNumber/$RECORDS_PER_TOPIC to kafka topic '$topic'.")
                producer.sendRecord(topic.toByteArray(), recordId(collectionNumber, messageNumber), body(messageNumber), timestamp)
                producer.sendRecord(excludedTopic.toByteArray(), recordId(collectionNumber, messageNumber), body(messageNumber), timestamp)
                logger.debug("Sent record $messageNumber/$RECORDS_PER_TOPIC to kafka topic '$topic' and to excluded topic '$excludedTopic'.")
            }
        }
        logger.info("Started record producer")
    }

    private suspend fun verifyHbase() {
        var waitSoFarSecs = 0
        val shortInterval = 5
        val longInterval = 10
        val expectedTablesSorted = expectedTables.sorted()
        logger.info("Waiting for ${expectedTablesSorted.size} hbase tables to appear; Expecting to create: $expectedTablesSorted")
        HbaseClient.connect().use { hbase ->
            withTimeout(30.minutes) {
                do {
                    val foundTablesSorted = loadTestTables(hbase)
                    logger.info("Waiting for ${expectedTablesSorted.size} hbase tables to appear; Found ${foundTablesSorted.size}; Total of $waitSoFarSecs seconds elapsed")
                    delay(longInterval.seconds)
                    waitSoFarSecs += longInterval
                } while (expectedTablesSorted.toSet() != foundTablesSorted.toSet())


                loadTestTables(hbase).forEach { tableName ->
                    tableName shouldNotContain "excluded"
                    launch (Dispatchers.IO) {

                        hbase.connection.getTable(TableName.valueOf(tableName)).use { table ->
                            do {
                                val foundRecords = recordCount(table)
                                logger.info("Waiting for $RECORDS_PER_TOPIC hbase records to appear in $tableName; Found $foundRecords; Total of $waitSoFarSecs seconds elapsed")
                                delay(shortInterval.seconds)
                                waitSoFarSecs += shortInterval
                            } while (foundRecords < RECORDS_PER_TOPIC)
                        }
                    }
                }
            }
        }
    }

    private fun verifyS3() {
        val contentsList = allArchiveObjectContentsAsJson()
        contentsList.size shouldBe TOPIC_COUNT * RECORDS_PER_TOPIC
        contentsList.forEach {
            it["traceId"].asJsonPrimitive.asString shouldBe "00002222-abcd-4567-1234-1234567890ab"
            val message = it["message"]
            message shouldNotBe null
            message!!.asJsonObject shouldNotBe null
            message.asJsonObject!!["dbObject"] shouldNotBe null
            it["message"]!!.asJsonObject!!["dbObject"]!!.asJsonPrimitive shouldNotBe null
            it["message"]!!.asJsonObject!!["dbObject"]!!.asJsonPrimitive!!.asString shouldBe "bubHJjhg2Jb0uyidkl867gtFkjl4fgh9AbubHJjhg2Jb0uyidkl867gtFkjl4fgh9AbubHJjhg2Jb0uyidkl867gtFkjl4fgh9A"
        }
    }

    private fun verifyManifests() {
        val contentsList = allManifestObjectContentsAsString()
        contentsList.forEach {
            val manifestLines = it.split("\n")
            manifestLines.filter(String::isNotBlank).forEach { line ->
                val fields = line.split("|")
                fields.size shouldBe 8
                fields[0] shouldNotBe ""
                fields[1] shouldNotBe ""
                fields[2] shouldNotBe ""
                fields[3] shouldNotBe ""
                fields[4] shouldBe "STREAMED"
                fields[5] shouldBe "K2HB"
                fields[6] shouldNotBe ""
                fields[7] shouldBe "KAFKA_RECORD"
            }
        }
    }

    private fun allArchiveObjectContentsAsJson(): List<JsonObject> =
            objectSummaries(Config.CorporateStorage.archiveBucket, Config.CorporateStorage.archiveDirectory, CorporateStorageService.s3)
                .filter { it.key.endsWith("jsonl.gz") && it.key.contains("load_test") }
                .map(S3ObjectSummary::getKey)
                .map(this@Kafka2hbIntegrationLoadSpec::archiveObjectContents)
                .map(::String)
                .flatMap { it.split("\n") }
                .filter(String::isNotEmpty)
                .map { Gson().fromJson(it, JsonObject::class.java) }

    private fun archiveObjectContents(key: String) =
            GZIPInputStream(CorporateStorageService.s3.getObject(GetObjectRequest(Config.CorporateStorage.archiveBucket, key)).objectContent).use {
                ByteArrayOutputStream().also { output -> it.copyTo(output) }
            }.toByteArray()

    private fun allManifestObjectContentsAsString(): List<String> =
            objectSummaries(Config.Manifest.manifestBucket, Config.Manifest.manifestDirectory, ManifestService.s3)
                .filter { it.key.endsWith("txt") && it.key.contains("load-test") }
                .map(S3ObjectSummary::getKey)
                .map(this@Kafka2hbIntegrationLoadSpec::manifestObjectContents)
                .map(::String)

    private fun manifestObjectContents(key: String) =
            ManifestService.s3.getObject(GetObjectRequest(Config.Manifest.manifestBucket, key))
                .objectContent.use {
                    ByteArrayOutputStream().also { output -> it.copyTo(output) }
                }.toByteArray()

    private fun objectSummaries(s3BucketName: String, s3Prefix: String, s3Connection: AmazonS3): MutableList<S3ObjectSummary> {
        val objectSummaries = mutableListOf<S3ObjectSummary>()
        val request = ListObjectsV2Request().apply {
            bucketName = s3BucketName
            prefix = s3Prefix
        }

        var objectListing: ListObjectsV2Result?

        do {
            objectListing = s3Connection.listObjectsV2(request)
            objectSummaries.addAll(objectListing.objectSummaries)
            request.continuationToken = objectListing.nextContinuationToken
        } while (objectListing != null && objectListing.isTruncated)

        return objectSummaries
    }

    private fun recordCount(table: Table) = table.getScanner(Scan()).count()
    private val expectedTables by lazy { (0..9).map { tableName(it) } }

    private fun loadTestTables(hbase: HbaseClient): List<String> {
        val tables = hbase.connection.admin.listTableNames()
            .map(TableName::getNameAsString)
            .filter(Regex(tableNamePattern())::matches)
            .sorted()
        logger.info("...hbase tables: found ${tables.size}: $tables")
        return tables
    }

    private fun tableName(counter: Int) = sampleQualifiedTableName("$DB_NAME$counter", "$COLLECTION_NAME$counter")
    private fun tableNamePattern() = """$DB_NAME\d+:$COLLECTION_NAME\d+""".replace("-", "_").replace(".", "_")

    private fun topicName(collectionNumber: Int)
            = "db.$DB_NAME$collectionNumber.$COLLECTION_NAME$collectionNumber"

    private fun excludedTopicName(collectionNumber: Int)
            = "db.excluded.$COLLECTION_NAME$collectionNumber"

    private fun recordId(collectionNumber: Int, messageNumber: Int) =
            "key-$messageNumber/$collectionNumber".toByteArray()

    private fun body(recordNumber: Int) = """{
        "traceId": "00002222-abcd-4567-1234-1234567890ab",
        "unitOfWorkId": "00002222-abcd-4567-1234-1234567890ab",
        "@type": "V4",
        "version": "core-X.release_XXX.XX",
        "timestamp": "2018-12-14T15:01:02.000+0000",
        "message": {
            "@type": "MONGO_UPDATE",
            "collection": "$COLLECTION_NAME",
            "db": "$DB_NAME",
            "_id": {
                "id": "$DB_NAME/$COLLECTION_NAME/$recordNumber"
            },
            "_lastModifiedDateTime": "${getISO8601Timestamp()}",
            "encryption": {
                "encryptionKeyId": "cloudhsm:1,2",
                "encryptedEncryptionKey": "bHJjhg2Jb0uyidkl867gtFkjl4fgh9Ab",
                "initialisationVector": "kjGyvY67jhJHVdo2",
                "keyEncryptionKeyId": "cloudhsm:1,2"
            },
            "dbObject": "bubHJjhg2Jb0uyidkl867gtFkjl4fgh9AbubHJjhg2Jb0uyidkl867gtFkjl4fgh9AbubHJjhg2Jb0uyidkl867gtFkjl4fgh9A",
            "timestamp_created_from": "_lastModifiedDateTime"
        }
    }""".toByteArray()


    private fun verifyMetadataStore(expectedCount: Int, database: String, collection: String) =
            metadataStoreConnection().use { connection ->
                connection.createStatement().use { statement ->
                    val results =
                            statement.executeQuery("SELECT count(*) FROM ucfs WHERE topic_name like '%$database%' and topic_name like '%$collection%'")
                    results.next() shouldBe true
                    val count = results.getLong(1)
                    count shouldBe expectedCount.toLong()
                }
            }

    private suspend fun verifyMetrics() {
        verifyMetricNames()
        validateMetric("""k2hb_gauge{job="k2hb", instance="k2hb-integration-test-container"}""", "1")
        validateMetric("""sum(k2hb_record_successes_total{topic=~"db.load-test.+"})""", "10000")
    }

    private suspend fun verifyMetricNames() {
        val response = client.get<JsonObject>("http://prometheus:9090/api/v1/targets/metadata")
        val metricNames = response["data"].asJsonArray
            .map(JsonElement::getAsJsonObject)
            .filter {
                it["target"].asJsonObject["job"].asJsonPrimitive.asString == "pushgateway"
            }
            .map {
                it["metric"].asJsonPrimitive.asString
            }
            .filterNot {
                it.startsWith("go_") || it.startsWith("process_") ||
                        it.startsWith("pushgateway_") || it.startsWith("push_")
            }

        metricNames shouldContainAll listOf("k2hb_batch_timer",
            "k2hb_batch_timer_created",
            "k2hb_dlq_summary",
            "k2hb_dlq_summary_created",
            "k2hb_gauge",
            "k2hb_hbase_successes",
            "k2hb_hbase_successes_created",
            "k2hb_manifest_successes",
            "k2hb_manifest_successes_created",
            "k2hb_maximum_lag",
            "k2hb_metadatastore_successes",
            "k2hb_metadatastore_successes_created",
            "k2hb_record_successes_created",
            "k2hb_record_successes_total",
            "k2hb_s3_successes",
            "k2hb_s3_successes_created",
            "logback_appender_created",
            "logback_appender_total")
    }

    private suspend fun validateMetric(resource: String, expected: String) {
        val response = client.get<JsonObject>("http://prometheus:9090/api/v1/query?query=${URLEncoder.encode(resource, "ASCII")}")
        val results = response["data"].asJsonObject["result"].asJsonArray
        results.size() shouldBe 1
        val result = results[0].asJsonObject["value"].asJsonArray[1].asJsonPrimitive.asString
        result shouldBe expected
    }

    companion object {
        private const val TOPIC_COUNT = 10
        private const val RECORDS_PER_TOPIC = 1_000
        private const val DB_NAME = "load-test-database"
        private const val COLLECTION_NAME = "load-test-collection"
        private val logger = LoggerFactory.getLogger(Kafka2hbIntegrationLoadSpec::class.java)
        val client = HttpClient {
            install(JsonFeature) {
                serializer = GsonSerializer {
                    setPrettyPrinting()
                }
            }
        }
    }
}


