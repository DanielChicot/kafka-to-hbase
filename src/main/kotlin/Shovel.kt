
import io.prometheus.client.Gauge
import org.apache.kafka.clients.consumer.KafkaConsumer
import sun.misc.Signal
import uk.gov.dwp.dataworks.logging.DataworksLogger
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis
import kotlin.time.ExperimentalTime

@ExperimentalTime
class Shovel(private val consumer: KafkaConsumer<ByteArray, ByteArray>,
             private val k2hbGauge: Gauge,
             private val maximumLagGauge: Gauge) {

    @ExperimentalTime
    suspend fun shovel(metadataClient: MetadataStoreClient,
                       corporateStorageService: CorporateStorageService,
                       manifestService: ManifestService,
                       pollTimeout: Duration) {
        k2hbGauge.inc()
        listOf("INT", "TERM").forEach(::handleSignal)
        val parser = MessageParser()
        val validator = Validator()
        val converter = Converter()
        val listProcessor =
            ListProcessor(validator, converter, MetricsClient.dlqTimer,
                MetricsClient.dlqRetries, MetricsClient.dlqFailures,
                MetricsClient.batchTimer, MetricsClient.batchFailures,
                MetricsClient.recordSuccesses, MetricsClient.recordFailures)

        var batchCount = 0

        logger.info("Subscription regexes",
            "includes_regex" to Config.Kafka.topicRegex.pattern,
            "excludes_regex" to Config.Kafka.topicExclusionRegexText)

        while (!closed.get()) {

            SubscriberUtility.subscribe(consumer, Config.Kafka.topicRegex, Config.Kafka.topicExclusionRegex)

            logger.info("Polling", "timeout" to "$pollTimeout")

            val records = consumer.poll(pollTimeout)

            if (records.count() > 0) {
                HbaseClient.connect().use { hbase ->
                    val timeTaken = measureTimeMillis {
                        listProcessor.processRecords(hbase, consumer, metadataClient, corporateStorageService,
                            manifestService, parser, records)
                    }
                    logger.info("Processed batch", "time_taken" to "$timeTaken", "size" to "${records.count()}")
                }
            }

            if (batchCountIsMultipleOfReportFrequency(batchCount++)) {
                printLogs(consumer)
            }
        }
    }

    private fun handleSignal(signalName: String) {
        logger.info("Setting up $signalName handler.")
        Signal.handle(Signal(signalName)) {
            logger.info("Signal received, cancelling job.", "signal" to "$it")
            k2hbGauge.dec()
            closed.set(true)
            consumer.wakeup()
            MetricsClient.pushFinalMetrics()
        }
    }

    private fun printLogs(consumer: KafkaConsumer<ByteArray, ByteArray>) {
        consumer.metrics().filter { it.key.group() == "consumer-fetch-manager-metrics" }
            .filter { it.key.name() == "records-lag-max" }
            .mapNotNull { it.value }
            .forEach { metric ->
                val max = metric.metricValue() as Double
                if (!max.isNaN()) {
                    metric.metricName().tags().takeIf { tags ->
                        tags.containsKey("topic") && tags.containsKey("partition")
                    } ?.let { tags ->
                        logger.info("Max record lag", "lag" to "$max",
                            "topic" to "${tags["topic"]}", "partition" to "${tags["partition"]}")
                        maximumLagGauge.labels(tags["topic"], tags["partition"]).set(max)
                    }
                }
            }

        consumer.listTopics()
            .filter { (topic, _) -> Config.Kafka.topicRegex.matches(topic) }
            .forEach { (topic, _) ->
                logger.info("Subscribed to topic", "topic_name" to topic)
            }
    }

    fun batchCountIsMultipleOfReportFrequency(batchCount: Int): Boolean =
        (batchCount % Config.Shovel.reportFrequency) == 0

    companion object {
        private val logger = DataworksLogger.getLogger(Shovel::class)
        private val closed: AtomicBoolean = AtomicBoolean(false)
    }
}
