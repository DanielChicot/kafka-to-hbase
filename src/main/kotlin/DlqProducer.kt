import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import kotlin.time.ExperimentalTime

@ExperimentalTime
object DlqProducer {
    val instance: Producer<ByteArray, ByteArray> by lazy {
        KafkaProducer(Config.Kafka.producerProps)
    }
}
