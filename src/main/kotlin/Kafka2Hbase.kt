import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.KafkaConsumer
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun main() {
    MetricsClient.startMetricsScheduler()
    runBlocking {
        MetadataStoreClient.connect().use { metadataStore ->
            KafkaConsumer<ByteArray, ByteArray>(Config.Kafka.consumerProps).use { kafka ->
                Shovel(kafka, MetricsClient.k2hbGauge, MetricsClient.maximumLagGauge).shovel(metadataStore,
                    CorporateStorageService.connect(),
                    ManifestService.connect(),
                    Config.Kafka.pollTimeout)
            }
        }
    }
}

