import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import io.prometheus.client.Summary
import io.prometheus.client.exporter.PushGateway
import uk.gov.dwp.dataworks.logging.DataworksLogger
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.time.ExperimentalTime

@ExperimentalTime
object MetricsClient {

    fun startMetricsScheduler() {
        if (Config.Metrics.pushMetrics) {
            metricsScheduler
        }
    }

    fun pushFinalMetrics() {
        metricsScheduler.cancel()
        pushMetrics()
        if (Config.Metrics.deleteMetrics) {
            logger.info("Waiting for metric collection before deletion",
                "scrape_interval" to "${Config.Metrics.scrapeInterval}",
                *metricsGroupingKeyPairs())
            Thread.sleep(Config.Metrics.scrapeInterval)
            deleteMetrics()
        }
    }

    val k2hbGauge: Gauge by lazy {
        gauge("k2hb_gauge", "How many instances are running")
    }

    val recordSuccesses: Counter by lazy {
        counter("k2hb_record_successes", "The count of successfully processed kafka messages","topic", "partition")
    }

    val recordFailures: Counter by lazy {
        counter("k2hb_record_failures", "The count of failed kafka messages", "topic", "partition")
    }

    val batchTimer: Summary by lazy {
        timer("k2hb_batch_timer", "Times and counts batches read from kafka", "topic", "partition")
    }

    val batchFailures: Counter by lazy {
        counter("k2hb_batch_failures", "The number of failed batch attempts", "topic", "partition")
    }

    val dlqTimer: Summary by lazy {
        timer("k2hb_dlq_summary", "Times and counts messages put on the dlq", "topic", "partition")
    }

    val dlqRetries: Counter by lazy {
        counter("k2hb_dlq_retries", "The number of dlq retry attempts", "topic", "partition")
    }

    val dlqFailures: Counter by lazy {
        counter("k2hb_dlq_failures", "The number of dlq failed attempts", "topic", "partition")
    }

    val corporateStorageSuccesses: Summary by lazy {
        timer("k2hb_s3_successes", "Times and counts batches objects placed into s3", "topic", "partition")
    }

    val corporateStorageRetries: Counter by lazy {
        counter("k2hb_s3_retries", "The number of s3 put retries", "topic", "partition")
    }

    val corporateStorageFailures: Counter by lazy {
        counter("k2hb_s3_failures", "The number of s3 put fails", "topic", "partition")
    }

    val hbaseSuccesses: Summary by lazy {
        timer("k2hb_hbase_successes", "Times and counts batches placed into hbase", "topic", "partition")
    }

    val hbaseRetries: Counter by lazy {
        counter("k2hb_hbase_retries", "The number of hbase put retries", "topic", "partition")
    }

    val hbaseFailures: Counter by lazy {
        counter("k2hb_hbase_failures", "The number of hbase put fails", "topic", "partition")
    }

    val manifestSuccesses: Summary by lazy {
        timer("k2hb_manifest_successes", "Times and counts manifests placed into s3", "topic", "partition")
    }

    val manifestRetries: Counter by lazy {
        counter("k2hb_manifest_retries", "The number of manifest put object retries", "topic", "partition")
    }

    val manifestFailures: Counter by lazy {
        counter("k2hb_manifest_failures", "The number of manifest put object fails", "topic", "partition")
    }

    val metadataStoreSuccesses: Summary by lazy {
        timer("k2hb_metadatastore_successes",
            "Times and counts metadatastore batches placed into rds",
            "topic",
            "partition")
    }

    val metadataStoreRetries: Counter by lazy {
        counter("k2hb_metadatastore_retries", "The number of manifest put object retries", "topic", "partition")
    }

    val metadataStoreFailures: Counter by lazy {
        counter("k2hb_metadatastore_failures", "The number of manifest put object fails", "topic", "partition")
    }

    val maximumLagGauge: Gauge by lazy {
        gauge("k2hb_maximum_lag", "The current maximum lag of each topic, partition","topic", "partition")
    }

    private fun counter(name: String, help: String, vararg labels: String): Counter =
        with(Counter.build()) {
            name(name)
            labelNames(*labels)
            help(help)
            register()
        }

    private fun gauge(name: String, help: String, vararg labels: String): Gauge =
        with(Gauge.build()) {
            name(name)
            labelNames(*labels)
            help(help)
            register()
        }

    private fun timer(name: String, help: String, vararg labels: String): Summary =
        with(Summary.build()) {
            name(name)
            labelNames(*labels)
            help(help)
            register()
        }

    private fun groupingKey() = mapOf("instance" to Config.Metrics.instanceName,
        "subscription" to Config.Kafka.topicRegex.pattern)

    private fun metricsGroupingKeyPairs(): Array<Pair<String, String>> =
        groupingKey().entries.map { (k, v) -> Pair(k, v) }.toTypedArray()

    private val pushGateway: PushGateway by lazy {
        PushGateway(Config.Metrics.pushgateway)
    }

    private fun pushMetrics() {
        pushGateway.push(CollectorRegistry.defaultRegistry, "k2hb", groupingKey())
        logger.info("Pushed metrics", *metricsGroupingKeyPairs())
    }

    private val metricsScheduler: Timer by lazy {
        fixedRateTimer("Push metrics", daemon = true,
            Config.Metrics.pushScheduleInitialDelay,
            Config.Metrics.pushSchedulePeriod) {
            pushMetrics()
        }
    }

    private fun deleteMetrics() {
        pushGateway.delete("htme", groupingKey())
        logger.info("Deleted metrics",
            "scrape_interval" to "${Config.Metrics.scrapeInterval}", *metricsGroupingKeyPairs())
    }

    private val logger = DataworksLogger.getLogger(MetricsClient::class)
}
