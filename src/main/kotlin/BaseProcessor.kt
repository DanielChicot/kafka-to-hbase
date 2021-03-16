
import Config.Kafka.dlqTopic
import RetryUtility.retry
import com.beust.klaxon.Klaxon
import io.prometheus.client.Counter
import io.prometheus.client.Summary
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import uk.gov.dwp.dataworks.logging.DataworksLogger
import kotlin.time.ExperimentalTime

@ExperimentalTime
open class BaseProcessor(private val validator: Validator, private val converter: Converter,
                         private val successes: Summary,
                         private val retries: Counter,
                         private val failures: Counter) {

    open suspend fun recordAsJson(record: ConsumerRecord<ByteArray, ByteArray>) =
        try {
            converter.convertToJson(record.value()).let { json ->
                validator.validate(json.toJsonString())
                json
            }
        } catch (e: IllegalArgumentException) {
            logger.warn("Could not parse message body", "record" to getDataStringForRecord(record))
            sendMessageToDlq(record, "Invalid json")
            null
        } catch (e: InvalidMessageException) {
            logger.warn("Schema validation error", "record" to getDataStringForRecord(record), "message" to "${e.message}")
            sendMessageToDlq(record, "Invalid schema for ${getDataStringForRecord(record)}: ${e.message}")
            null
        }

    open suspend fun sendMessageToDlq(record: ConsumerRecord<ByteArray, ByteArray>, reason: String) {
        val body = record.value()
        val originalTopic = record.topic()
        val originalOffset = record.offset()
        val recordKey: ByteArray? = if (record.key() != null) record.key() else null
        val stringKey = if (recordKey != null) String(recordKey) else "UNKNOWN"
        logger.warn("Error processing record, sending to dlq", "reason" to reason, "key" to stringKey)

        try {
            retry(successes, retries, failures, {
                val malformedRecord = MalformedRecord(stringKey, String(body), reason)
                val jsonString = Klaxon().toJsonString(malformedRecord)
                val producerRecord =
                    ProducerRecord(dlqTopic, null, null, recordKey, jsonString.toByteArray(), null)

                logger.info("Sending message to dlq", "key" to stringKey, "original_topic" to originalTopic,
                    "original_offset" to "$originalOffset")
                val metadata = DlqProducer.instance.send(producerRecord)?.get()
                logger.info("Sent message to dlq", "key" to stringKey, "dlq_topic" to "${metadata?.topic()}",
                    "dlq_offset" to "${metadata?.offset()}")
            }, record.topic(), "${record.partition()}")
        } catch (e: Exception) {
            logger.error(
                "Error sending message to dlq",
                "key" to String(record.key()), "topic" to record.topic(), "offset" to "${record.offset()}"
            )
            throw DlqException("Exception while sending message to DLQ : $e")
        }
    }

    private fun getDataStringForRecord(record: ConsumerRecord<ByteArray, ByteArray>) =
        "${String(record.key() ?: ByteArray(0))}:${record.topic()}:${record.partition()}:${record.offset()}"

    companion object {
        private val logger = DataworksLogger.getLogger(BaseProcessor::class)
    }
}
