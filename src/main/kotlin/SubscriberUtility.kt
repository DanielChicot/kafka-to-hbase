
import kotlinx.coroutines.delay
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.PartitionInfo
import uk.gov.dwp.dataworks.logging.DataworksLogger
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

object SubscriberUtility {

    @ExperimentalTime
    tailrec suspend fun <T> subscribe(consumer: KafkaConsumer<T, T>, includesRegex: Regex, excludesRegex: Regex? = null) {
        val currentSubscription = consumer.subscription()

        val topics =  with(includedTopics(consumer, includesRegex)) {
            excludesRegex?.let {
                filterNot(excludesRegex::matches)
            } ?: this
        }

        if (topics.toSet() != currentSubscription && topics.isNotEmpty()) {

            topics.minus(currentSubscription).forEach {
                logger.info("New topic found", "topic" to it)
            }

            consumer.subscribe(topics)
            return
        }

        if (currentSubscription.isNotEmpty()) {
            return
        }

        logger.info("No topics and no current subscription trying again")
        delay(1.seconds)
        subscribe(consumer, includesRegex, excludesRegex)
    }

    private fun <T> includedTopics(consumer: KafkaConsumer<T, T>, inclusionRegex: Regex): List<String> =
        consumer.listTopics()
            .map(Map.Entry<String, List<PartitionInfo>>::key)
            .filter(inclusionRegex::matches)

    private val logger = DataworksLogger.getLogger(SubscriberUtility::class)
}
