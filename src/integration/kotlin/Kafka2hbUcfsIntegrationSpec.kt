
import Utility.getISO8601Timestamp
import Utility.getId
import Utility.getS3Client
import Utility.sampleQualifiedTableName
import Utility.sendRecord
import Utility.uniqueTopicName
import Utility.uniqueTopicNameWithDot
import Utility.verifyHbaseRegions
import Utility.verifyMetadataStore
import Utility.waitFor
import Utility.wellFormedValidPayload
import com.beust.klaxon.Klaxon
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.log4j.Logger
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds

@ExperimentalTime
class Kafka2hbUcfsIntegrationSpec : StringSpec() {

    init {
        "UCFS Messages with new identifiers are written to hbase but not to dlq" {

            val hbase = HbaseClient.connect()
            val producer = KafkaProducer<ByteArray, ByteArray>(Config.Kafka.producerProps)
            val parser = MessageParser()
            val converter = Converter()
            val topic = uniqueTopicName()
            val matcher = TextUtils().topicNameTableMatcher(topic)!!
            val namespace = matcher.groupValues[1]
            val tableName = matcher.groupValues[2]
            val qualifiedTableName = sampleQualifiedTableName(namespace, tableName)
            hbase.ensureTable(qualifiedTableName)

            val s3Client = getS3Client()
            val summaries = s3Client.listObjectsV2("kafka2s3", "prefix").objectSummaries
            summaries.forEach { s3Client.deleteObject("kafka2s3", it.key) }
            val summariesManifests = s3Client.listObjectsV2("manifests", "manifest_prefix").objectSummaries
            summariesManifests.forEach { s3Client.deleteObject("manifests", it.key) }

            verifyMetadataStore(0, topic, true)

            val body = wellFormedValidPayload(namespace, tableName)
            val timestamp = converter.getTimestampAsLong(getISO8601Timestamp())
            val (_, hbaseKey) = parser.generateKey(converter.convertToJson(getId().toByteArray()))
            log.info("Sending well-formed record to kafka topic '$topic'.")
            producer.sendRecord(topic.toByteArray(), "key1".toByteArray(), body, timestamp)
            log.info("Sent well-formed record to kafka topic '$topic'.")
            val referenceTimestamp = converter.getTimestampAsLong(getISO8601Timestamp())
            val storedValue = waitFor {
                hbase.getCellBeforeTimestamp(qualifiedTableName, hbaseKey, referenceTimestamp)
            }

            storedValue shouldNotBe null
            val jsonObject = Gson().fromJson(String(storedValue!!), JsonObject::class.java)
            val putTime = jsonObject["put_time"].asJsonPrimitive.asString
            putTime shouldNotBe null
            val expected = Gson().fromJson(String(body), JsonObject::class.java)
            expected.addProperty("put_time", putTime)
            String(storedValue) shouldBe expected.toString()

            val summaries1 = s3Client.listObjectsV2("kafka2s3", "prefix").objectSummaries
            summaries1.size shouldBe 0
            val summariesManifests1 = s3Client.listObjectsV2("manifests", "manifest_prefix").objectSummaries
            summariesManifests1.size shouldBe 1

            verifyHbaseRegions(qualifiedTableName, regionReplication, regionServers)
            verifyMetadataStore(1, topic, true)
        }

        "UCFS Messages on the agentToDoArchive topic are written to agentToDo" {
            val hbase = HbaseClient.connect()
            val producer = KafkaProducer<ByteArray, ByteArray>(Config.Kafka.producerProps)
            val parser = MessageParser()
            val converter = Converter()
            val topic = "db.agent-core.agentToDoArchive"
            val qualifiedTableName = "agent_core:agentToDo"

            verifyMetadataStore(0, topic, true)

            hbase.ensureTable(qualifiedTableName)
            val body = wellFormedValidPayload("agent-core", "agentToDoArchive")
            val timestamp = converter.getTimestampAsLong(getISO8601Timestamp())
            val (_, hbaseKey) = parser.generateKey(converter.convertToJson(getId().toByteArray()))
            log.info("Sending well-formed record to kafka topic '$topic'.")
            producer.sendRecord(topic.toByteArray(), "key1".toByteArray(), body, timestamp)
            log.info("Sent well-formed record to kafka topic '$topic'.")
            val referenceTimestamp = converter.getTimestampAsLong(getISO8601Timestamp())
            val storedValue = waitFor { hbase.getCellBeforeTimestamp(qualifiedTableName, hbaseKey, referenceTimestamp) }

            val jsonObject = Gson().fromJson(String(storedValue!!), JsonObject::class.java)
            val putTime = jsonObject["put_time"].asJsonPrimitive.asString
            putTime shouldNotBe null
            val expected = Gson().fromJson(String(body), JsonObject::class.java)
            expected.addProperty("put_time", putTime)
            String(storedValue) shouldBe expected.toString()
            verifyMetadataStore(1, topic, true)
        }

        "UCFS Messages with previously received identifiers are written as new versions to hbase but not to dlq" {
            val s3Client = getS3Client()
            val summaries = s3Client.listObjectsV2("kafka2s3", "prefix").objectSummaries
            summaries.forEach { s3Client.deleteObject("kafka2s3", it.key) }

            val hbase = HbaseClient.connect()
            val producer = KafkaProducer<ByteArray, ByteArray>(Config.Kafka.producerProps)
            val parser = MessageParser()
            val converter = Converter()
            val topic = uniqueTopicName()
            val matcher = TextUtils().topicNameTableMatcher(topic)!!
            val (_, hbaseKey) = parser.generateKey(converter.convertToJson(getId().toByteArray()))
            val namespace = matcher.groupValues[1]
            val tableName = matcher.groupValues[2]
            val qualifiedTableName = sampleQualifiedTableName(namespace, tableName)
            hbase.ensureTable(qualifiedTableName)
            val kafkaTimestamp1 = converter.getTimestampAsLong(getISO8601Timestamp())
            val body1 = wellFormedValidPayload(namespace, tableName)
            hbase.putRecord(qualifiedTableName, hbaseKey, kafkaTimestamp1, body1)

            verifyMetadataStore(0, topic, true)

            val referenceTimestamp = converter.getTimestampAsLong(getISO8601Timestamp())

            val body2 = wellFormedValidPayload(namespace, tableName)

            val kafkaTimestamp2 = converter.getTimestampAsLong(getISO8601Timestamp())
            producer.sendRecord(topic.toByteArray(), "key2".toByteArray(), body2, kafkaTimestamp2)

            val summaries1 = s3Client.listObjectsV2("kafka2s3", "prefix").objectSummaries
            summaries1.size shouldBe 0

            val storedNewValue =
                waitFor { hbase.getCellAfterTimestamp(qualifiedTableName, hbaseKey, referenceTimestamp) }
            val jsonObject = Gson().fromJson(String(storedNewValue!!), JsonObject::class.java)
            val putTime = jsonObject["put_time"].asJsonPrimitive.asString
            putTime shouldNotBe null
            val expected = Gson().fromJson(String(body2), JsonObject::class.java)
            expected.addProperty("put_time", putTime)
            String(storedNewValue) shouldBe expected.toString()

            val storedPreviousValue =
                waitFor { hbase.getCellBeforeTimestamp(qualifiedTableName, hbaseKey, referenceTimestamp) }
            String(storedPreviousValue!!) shouldBe String(body1)

            verifyMetadataStore(1, topic, true)
        }

        "UCFS Malformed json messages are written to dlq topic" {
            val s3Client = getS3Client()

            val converter = Converter()
            val topic = uniqueTopicName()
            val body = "junk".toByteArray()
            val timestamp = converter.getTimestampAsLong(getISO8601Timestamp())
            val producer = KafkaProducer<ByteArray, ByteArray>(Config.Kafka.producerProps)
            producer.sendRecord(topic.toByteArray(), "key3".toByteArray(), body, timestamp)
            val malformedRecord = MalformedRecord("key3", String(body), "Invalid json")
            val expected = Klaxon().toJsonString(malformedRecord)

            verifyMetadataStore(0, topic, true)

            withTimeout(15.minutes) {
                while (true) {
                    try {
                        val s3Object = s3Client.getObject(
                                "kafka2s3",
                                "prefix/test-dlq-topic/${SimpleDateFormat(dateFormat).format(Date())}/key3"
                        ).objectContent
                        val actual = s3Object.bufferedReader().use(BufferedReader::readText)
                        actual shouldBe expected
                        break
                    }
                    catch (e: Exception) {
                        delay(5.seconds)
                    }
                }
            }

            verifyMetadataStore(0, topic, true)
        }

        "UCFS Invalid json messages as per the schema are written to dlq topic" {
            val s3Client = getS3Client()
            val converter = Converter()
            val topic = uniqueTopicName()
            val body = """{ "key": "value" } """.toByteArray()
            val timestamp = converter.getTimestampAsLong(getISO8601Timestamp())
            val producer = KafkaProducer<ByteArray, ByteArray>(Config.Kafka.producerProps)
            producer.sendRecord(topic.toByteArray(), "key4".toByteArray(), body, timestamp)
            val key = "prefix/test-dlq-topic/${SimpleDateFormat(dateFormat).format(Date())}/key4"
            log.info("key: $key")
            withTimeout(15.minutes) {
                while (true) {
                    try {
                        val s3Object = s3Client.getObject(
                                "kafka2s3",
                                "prefix/test-dlq-topic/${SimpleDateFormat(dateFormat).format(Date())}/key4"
                        ).objectContent
                        val actual = s3Object.bufferedReader().use(BufferedReader::readText)
                        val malformedRecord = MalformedRecord(
                                "key4", String(body),
                                "Invalid schema for key4:$topic:9:0: Message failed schema validation: '#: 6 schema violations found'."
                        )
                        val expected = Klaxon().toJsonString(malformedRecord)
                        actual shouldBe expected
                        break
                    }
                    catch (e: Exception) {
                        // try again
                    }
                }
            }

            verifyMetadataStore(0, topic, true)
        }

        "UCFS Messages with a dot in the collection.name are written to hbase but not to dlq" {
            val hbase = HbaseClient.connect()
            val producer = KafkaProducer<ByteArray, ByteArray>(Config.Kafka.producerProps)
            val parser = MessageParser()
            val converter = Converter()
            val topic = uniqueTopicNameWithDot()
            val matcher = TextUtils().topicNameTableMatcher(topic)!!
            val namespace = matcher.groupValues[1]
            val tableName = matcher.groupValues[2]
            val qualifiedTableName = sampleQualifiedTableName(namespace, tableName)
            hbase.ensureTable(qualifiedTableName)

            val s3Client = getS3Client()
            val summaries = s3Client.listObjectsV2("kafka2s3", "prefix").objectSummaries
            summaries.forEach { s3Client.deleteObject("kafka2s3", it.key) }
            verifyMetadataStore(0, topic, true)

            val body = wellFormedValidPayload(namespace, tableName)
            val timestamp = converter.getTimestampAsLong(getISO8601Timestamp())
            val (_, hbaseKey) = parser.generateKey(converter.convertToJson(getId().toByteArray()))
            log.info("Sending well-formed record to kafka topic '$topic'.")
            producer.sendRecord(topic.toByteArray(), "key1".toByteArray(), body, timestamp)
            log.info("Sent well-formed record to kafka topic '$topic'.")
            val referenceTimestamp = converter.getTimestampAsLong(getISO8601Timestamp())

            val storedValue = withTimeout(3.minutes) {
                var cell: ByteArray? = hbase.getCellBeforeTimestamp(qualifiedTableName, hbaseKey, referenceTimestamp)
                while (cell == null) {
                    delay(2.seconds)
                    cell = hbase.getCellBeforeTimestamp(qualifiedTableName, hbaseKey, referenceTimestamp)
                    log.info("qualifiedTableName: '$qualifiedTableName'.")
                    log.info("hbaseKey: '${String(hbaseKey)}'.")
                    log.info("referenceTimestamp: '$referenceTimestamp'.")
                    log.info("cell: '$cell'.")
                }
                cell
            }

            log.info("storedValue: $storedValue")
            val jsonObject = Gson().fromJson(String(storedValue), JsonObject::class.java)
            val putTime = jsonObject["put_time"].asJsonPrimitive.asString
            putTime shouldNotBe null
            val expected = Gson().fromJson(String(body), JsonObject::class.java)
            expected.addProperty("put_time", putTime)
            String(storedValue) shouldBe expected.toString()

            val summaries1 = s3Client.listObjectsV2("kafka2s3", "prefix").objectSummaries
            summaries1.size shouldBe 0
            verifyMetadataStore(1, topic, true)
        }
    }
  
    private val log = Logger.getLogger(Kafka2hbUcfsIntegrationSpec::class.toString())
    private val dateFormat = "YYYY-MM-dd"
    private val regionReplication = 3
    private val regionServers = 2
}

