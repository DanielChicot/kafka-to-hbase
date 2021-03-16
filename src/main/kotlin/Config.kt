
import com.amazonaws.ClientConfiguration
import com.amazonaws.Protocol
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HConstants
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import java.io.File
import java.net.InetAddress
import java.time.Duration
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds

@ExperimentalTime
object Config {
    const val metaDataRefreshKey = "metadata.max.age.ms"
    const val schemaFileProperty = "schema.location"
    const val mainSchemaFile = "business_message.schema.json"
    const val equalitySchemaFile = "equality_message.schema.json"
    const val auditSchemaFile = "audit_message.schema.json"
    const val dataworksRegion = "eu-west-2"

    object Metrics {
        val scrapeInterval: Long = getEnv("K2HB_METRICS_SCRAPE_INTERVAL")?.toLong() ?: 1.minutes.toLongMilliseconds()
        val deleteMetrics = getEnv("K2HB_METRICS_DELETE_METRICS")?.toBoolean() ?: true
        val instanceName = getEnv("K2HB_INSTANCE_NAME") ?: ""
        val pushgateway = getEnv("K2HB_METRICS_PUSHGATEWAY") ?: "pushgateway:9091"
        val pushMetrics = getEnv("K2HB_METRICS_PUSH_METRICS")?.toBoolean() ?: true

        val pushScheduleInitialDelay = getEnv("K2HB_METRICS_SCHEDULE_INITIAL_DELAY_SECONDS")?.run {
            toInt().seconds.inMilliseconds.toLong()
        } ?: 10.seconds.inMilliseconds.toLong()

        val pushSchedulePeriod = getEnv("K2HB_METRICS_SCHEDULE_PERIOD_SECONDS")?.run {
            toInt().seconds.inMilliseconds.toLong()
        } ?: 20.seconds.inMilliseconds.toLong()

    }

    object Shovel {
        val reportFrequency = getEnv("K2HB_KAFKA_REPORT_FREQUENCY")?.toInt() ?: 100
    }

    object Validator {
        var properties = Properties().apply {
            put(schemaFileProperty, getEnv("K2HB_VALIDATOR_SCHEMA") ?: mainSchemaFile)
        }
    }

    object Hbase {
        val config = Configuration().apply {
            set(HConstants.ZOOKEEPER_ZNODE_PARENT, getEnv("K2HB_HBASE_ZOOKEEPER_PARENT") ?: "/hbase")
            set(HConstants.ZOOKEEPER_QUORUM, getEnv("K2HB_HBASE_ZOOKEEPER_QUORUM") ?: "zookeeper")
            setInt("hbase.zookeeper.port", getEnv("K2HB_HBASE_ZOOKEEPER_PORT")?.toIntOrNull() ?: 2181)
            set(HConstants.HBASE_RPC_TIMEOUT_KEY, getEnv("K2HB_HBASE_RPC_TIMEOUT_MILLISECONDS") ?: "1200000")
            set(HConstants.HBASE_CLIENT_OPERATION_TIMEOUT, getEnv("K2HB_HBASE_OPERATION_TIMEOUT_MILLISECONDS") ?: "1800000")
            set(HConstants.HBASE_CLIENT_PAUSE, getEnv("K2HB_HBASE_PAUSE_MILLISECONDS") ?: "50")
            set(HConstants.HBASE_CLIENT_RETRIES_NUMBER, getEnv("K2HB_HBASE_RETRIES") ?: "50")
            set("hbase.client.keyvalue.maxsize", getEnv("K2HB_HBASE_KEYVALUE_MAX_SIZE") ?: "0")
        }

        val columnFamily = getEnv("K2HB_HBASE_COLUMN_FAMILY") ?: "cf"
        val columnQualifier = getEnv("K2HB_HBASE_COLUMN_QUALIFIER") ?: "record"
        val regionReplication: Int = getEnv("K2HB_HBASE_REGION_REPLICATION")?.toInt() ?: 3
        var DEFAULT_QUALIFIED_TABLE_PATTERN = """^\w+\.([-\w]+)\.([-.\w]+)$"""
        var qualifiedTablePattern = getEnv("K2HB_QUALIFIED_TABLE_PATTERN") ?: DEFAULT_QUALIFIED_TABLE_PATTERN
        val regionSplits = getEnv("K2HB_HBASE_REGION_SPLITS") ?: "2"
    }

    object Kafka {
        val consumerProps = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getEnv("K2HB_KAFKA_BOOTSTRAP_SERVERS") ?: "kafka:9092")
            put(ConsumerConfig.GROUP_ID_CONFIG, getEnv("K2HB_KAFKA_CONSUMER_GROUP") ?: "test")
            put(ConsumerConfig.CLIENT_ID_CONFIG, "${InetAddress.getLocalHost().hostName}-${System.currentTimeMillis()}")
            addSslConfig(this)
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer::class.java)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, getEnv("K2HB_KAFKA_META_REFRESH_MS") ?: "10000")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
            put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, getEnv("K2HB_KAFKA_MAX_POLL_RECORDS") ?: 500)
            val pollInterval = getEnv("K2HB_KAFKA_MAX_POLL_INTERVAL_MS")
            if (pollInterval != null) {
                put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, pollInterval.toInt())
            }
            put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, getEnv("K2HB_KAFKA_MAX_FETCH_BYTES") ?: 100_000_000)
            put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, getEnv("K2HB_KAFKA_MAX_PARTITION_FETCH_BYTES") ?: 100_000_000)
        }

        val producerProps = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getEnv("K2HB_KAFKA_BOOTSTRAP_SERVERS") ?: "kafka:9092")
            addSslConfig(this)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer::class.java)
            put(metaDataRefreshKey, getEnv("K2HB_KAFKA_META_REFRESH_MS") ?: "10000")
        }

        private fun addSslConfig(properties: Properties) {
            val insecure = getEnv("K2HB_KAFKA_INSECURE") ?: "true"
            if (insecure != "true") {
                properties.apply {
                    put("security.protocol", "SSL")
                    put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, getEnv("K2HB_TRUSTSTORE_PATH"))
                    put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, getEnv("K2HB_TRUSTSTORE_PASSWORD"))
                    put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, getEnv("K2HB_KEYSTORE_PATH"))
                    put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, getEnv("K2HB_KEYSTORE_PASSWORD"))
                    put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, getEnv("K2HB_PRIVATE_KEY_PASSWORD"))
                }
            }
        }

        val pollTimeout: Duration = getEnv("K2HB_KAFKA_POLL_TIMEOUT")?.toDuration() ?: Duration.ofSeconds(3)
        val topicRegex = Regex(getEnv("K2HB_KAFKA_TOPIC_REGEX") ?: """^(db[.]{1}[-\w]+[.]{1}[-\w]+)$""")

        val topicExclusionRegexText = getEnv("K2HB_KAFKA_TOPIC_EXCLUSION_REGEX") ?: "NOT_SET"
        val topicExclusionRegex: Regex? = if(topicExclusionRegexText != "NOT_SET") Regex(topicExclusionRegexText) else null

        val dlqTopic = getEnv("K2HB_KAFKA_DLQ_TOPIC") ?: "test-dlq-topic"

    }

    object MetadataStore {
        val writeToMetadataStore = (getEnv("K2HB_WRITE_TO_METADATA_STORE") ?: "true").toBoolean()
        val metadataStoreTable = getEnv("K2HB_METADATA_STORE_TABLE") ?: "ucfs"
        val autoCommit = (getEnv("K2HB_METADATA_STORE_AUTO_COMMIT") ?: "false").toBoolean()

        private val useAwsSecretsString = getEnv("K2HB_USE_AWS_SECRETS") ?: "true"

        val isUsingAWS = useAwsSecretsString == "true"

        val properties = Properties().apply {
            put("user", getEnv("K2HB_RDS_USERNAME") ?: "k2hbwriter")
            put("rds.password.secret.name", getEnv("K2HB_RDS_PASSWORD_SECRET_NAME") ?: "password")
            put("database", getEnv("K2HB_RDS_DATABASE_NAME") ?: "metadatastore")
            put("rds.endpoint", getEnv("K2HB_RDS_ENDPOINT") ?: "127.0.0.1")
            put("rds.port", getEnv("K2HB_RDS_PORT") ?: "3306")
            put("use.aws.secrets", getEnv("K2HB_USE_AWS_SECRETS") ?: "true")

            if (isUsingAWS) {
                put("ssl_ca_path", getEnv("K2HB_RDS_CA_CERT_PATH") ?: "/certs/AmazonRootCA1.pem")
                put("ssl_ca", readFile(getProperty("ssl_ca_path")))
                put("ssl_verify_cert", true)
            }
        }
    }

    object SecretManager {
        val properties = Properties().apply {
            put("region", getEnv("SECRET_MANAGER_REGION") ?: dataworksRegion)
        }
    }

    object AwsS3 {
        private val maxS3Connections: Int = (getEnv("K2HB_AWS_S3_MAX_CONNECTIONS") ?: "2000").toInt()
        private val useLocalStack = (getEnv("K2HB_AWS_S3_USE_LOCALSTACK") ?: "false").toBoolean()
        private val region = getEnv("K2HB_AWS_S3_REGION") ?: dataworksRegion

        private const val localstackServiceEndPoint = "http://aws-s3:4566/"
        private const val localstackAccessKey = "AWS_ACCESS_KEY_ID"
        private const val localstackSecretKey = "AWS_SECRET_ACCESS_KEY"

        val s3: AmazonS3 by lazy {
            if (useLocalStack) {
                AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(localstackServiceEndPoint, dataworksRegion))
                    .withClientConfiguration(ClientConfiguration().apply {
                        withProtocol(Protocol.HTTP)
                        maxConnections = maxS3Connections
                    })
                    .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials(localstackAccessKey, localstackSecretKey)))
                    .withPathStyleAccessEnabled(true)
                    .disableChunkedEncoding()
                    .build()
            }
            else {
                AmazonS3ClientBuilder.standard()
                    .withCredentials(DefaultAWSCredentialsProviderChain())
                    .withRegion(region)
                    .withClientConfiguration(ClientConfiguration().apply {
                        maxConnections = maxS3Connections
                    })
                    .build()
            }
        }
    }

    object CorporateStorage {
        val archiveBucket = getEnv("K2HB_AWS_S3_ARCHIVE_BUCKET") ?: "ucarchive"
        val archiveDirectory = getEnv("K2HB_AWS_S3_ARCHIVE_DIRECTORY") ?: "ucdata_main"
    }

    object Manifest {
        val manifestBucket = getEnv("K2HB_AWS_S3_MANIFEST_BUCKET") ?: "manifests"
        val manifestDirectory = getEnv("K2HB_AWS_S3_MANIFEST_DIRECTORY") ?: "streaming"
        val writeManifests = (getEnv("K2HB_AWS_S3_WRITE_MANIFESTS") ?: "true").toBoolean()
    }

    object Retry {
        val maxAttempts: Int = getEnv("K2HB_RETRY_MAX_ATTEMPTS")?.toInt() ?: 3
        val initialBackoff: Long = getEnv("K2HB_RETRY_INITIAL_BACKOFF")?.toLong() ?: 10000
        val backoffMultiplier: Long = getEnv("K2HB_RETRY_BACKOFF_MULTIPLIER")?.toLong() ?: 2
    }

    fun getEnv(envVar: String): String? {
        val value = System.getenv(envVar)
        return if (value.isNullOrEmpty()) null else value
    }

    fun String.toDuration(): Duration = Duration.parse(this)
    fun readFile(fileName: String): String = File(fileName).readText(Charsets.UTF_8)

}
