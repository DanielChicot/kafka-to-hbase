import RetryUtility.retry
import io.prometheus.client.Counter
import io.prometheus.client.Summary
import org.apache.kafka.clients.consumer.ConsumerRecord
import uk.gov.dwp.dataworks.logging.DataworksLogger
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import kotlin.system.measureTimeMillis
import kotlin.time.ExperimentalTime

@ExperimentalTime
open class MetadataStoreClient(private val connection: Connection,
                               private val successes: Summary,
                               private val retries: Counter,
                               private val failures: Counter): AutoCloseable {

    @Synchronized
    open fun recordProcessingAttempt(hbaseId: String, record: ConsumerRecord<ByteArray, ByteArray>, lastUpdated: Long) {
        if (Config.MetadataStore.writeToMetadataStore) {
            val rowsInserted = preparedStatement(hbaseId, lastUpdated, record).executeUpdate()
            logger.info("Recorded processing attempt", "rows_inserted" to "$rowsInserted")
        }
    }

    @Synchronized
    @Throws(SQLException::class)
    open suspend fun recordBatch(payloads: List<HbasePayload>) {
        if (Config.MetadataStore.writeToMetadataStore) {
            if (payloads.isNotEmpty()) {
                retry(successes, retries, failures, {
                    logger.info("Putting batch into metadata store", "size" to "${payloads.size}")
                    val timeTaken = measureTimeMillis {
                        recordProcessingAttemptStatement().use { statement ->
                            payloads.forEach {
                                statement.setString(1, textUtils.printableKey(it.key))
                                statement.setLong(2, it.version)
                                statement.setString(3, it.record.topic())
                                statement.setInt(4, it.record.partition())
                                statement.setLong(5, it.record.offset())
                                statement.addBatch()
                            }
                            statement.executeBatch()

                            if (!connection.autoCommit) {
                                connection.commit()
                            }
                        }
                    }
                    logger.info("Put batch into metadata store",
                        "time_taken" to "$timeTaken",
                        "size" to "${payloads.size}")
                }, payloads[0].record.topic(), "${payloads[0].record.partition()}")
            } else {
                logger.info("Not putting batch into metadata store",
                    "write_to_metadata_store" to "${Config.MetadataStore.writeToMetadataStore}")
            }
        }
    }

    private fun preparedStatement(hbaseId: String, lastUpdated: Long, record: ConsumerRecord<ByteArray, ByteArray>) =
        recordProcessingAttemptStatement().apply {
            setString(1, hbaseId)
            setLong(2, lastUpdated)
            setString(3, record.topic())
            setInt(4, record.partition())
            setLong(5, record.offset())
        }


    private fun recordProcessingAttemptStatement() =
        connection.prepareStatement("""
            INSERT INTO ${Config.MetadataStore.metadataStoreTable} (hbase_id, hbase_timestamp, topic_name, kafka_partition, kafka_offset)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent())


    companion object {
        private val isUsingAWS = Config.MetadataStore.isUsingAWS
        private val secretHelper: SecretHelperInterface = if (isUsingAWS) AWSSecretHelper() else DummySecretHelper()

        fun connect(): MetadataStoreClient {
            val (url, properties) = connectionProperties()
            return MetadataStoreClient(DriverManager.getConnection(url, properties).apply {
                autoCommit = Config.MetadataStore.autoCommit
            }, MetricsClient.metadataStoreSuccesses, MetricsClient.metadataStoreRetries,
                MetricsClient.metadataStoreFailures)
        }

        fun connectionProperties(): Pair<String, Properties> {
            val hostname = Config.MetadataStore.properties["rds.endpoint"]
            val port = Config.MetadataStore.properties["rds.port"]
            val jdbcUrl = "jdbc:mysql://$hostname:$port/${Config.MetadataStore.properties.getProperty("database")}"
            val secretName = Config.MetadataStore.properties.getProperty("rds.password.secret.name")
            val propertiesWithPassword: Properties = Config.MetadataStore.properties.clone() as Properties
            propertiesWithPassword["password"] = secretHelper.getSecret(secretName)
            return Pair(jdbcUrl, propertiesWithPassword)
        }

        val logger = DataworksLogger.getLogger(MetadataStoreClient::class)
        val textUtils = TextUtils()
    }

    override fun close() = connection.close()
}
