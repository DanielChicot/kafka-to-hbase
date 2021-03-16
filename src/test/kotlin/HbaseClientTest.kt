
import MetricsMocks.counter
import MetricsMocks.summary
import MetricsMocks.summaryChild
import com.nhaarman.mockitokotlin2.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.prometheus.client.Counter
import org.apache.hadoop.hbase.HColumnDescriptor
import org.apache.hadoop.hbase.HTableDescriptor
import org.apache.hadoop.hbase.NamespaceDescriptor
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.Admin
import org.apache.hadoop.hbase.client.Connection
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.client.Table
import org.apache.hadoop.hbase.io.compress.Compression.Algorithm
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Callable
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Suppress("BlockingMethodInNonBlockingContext")
class HbaseClientTest: StringSpec() {
    init {
        "Does not retry on success" {

            val table = mock<Table> {
                on { exists(any()) } doReturn true
            }

            val connection = mock<Connection> {
                on { admin } doReturn adm
                on { getTable(TableName.valueOf(qualifiedTableName)) } doReturn table
            }

            val successChild = summaryChild()
            val successTimer = summary(successChild)
            val retryChild = mock<Counter.Child>()
            val retriesCounter = counter(retryChild)
            val failureChild = mock<Counter.Child>()
            val failureCounter = counter(failureChild)
            with(HbaseClient(connection,
                columnFamily,
                columnQualifier,
                regionReplication,
                successTimer,
                retriesCounter,
                failureCounter)) {
                putList(qualifiedTableName, hbasePayloads())
            }

            verify(successTimer, times(1)).labels(any())
            verifyNoMoreInteractions(successTimer)
            verify(successChild, times(1)).time(any<Callable<*>>())
            verifyNoMoreInteractions(successChild)
            verifyZeroInteractions(retriesCounter)
            verifyZeroInteractions(failureCounter)
            verify(table, times(1)).put(any<List<Put>>())
            verify(table, times(1)).close()
        }

        "Retries until successful put" {
            val table = mock<Table> {
                on { put(any<List<Put>>()) } doThrow IOException(errorMessage) doAnswer {}

                on { exists(any()) } doReturn true
            }

            val connection = mock<Connection> {
                on { admin } doReturn adm
                on { getTable(TableName.valueOf(qualifiedTableName)) } doReturn table
            }

            val successChild = summaryChild()
            val successTimer = summary(successChild)
            val retriesChild = mock<Counter.Child>()
            val retriesCounter = counter(retriesChild)
            val failureChild = mock<Counter.Child>()
            val failureCounter = counter(failureChild)
            with(HbaseClient(connection,
                columnFamily,
                columnQualifier,
                regionReplication,
                successTimer,
                retriesCounter,
                failureCounter)) {
                putList(qualifiedTableName, hbasePayloads())
            }

            verify(table, times(2)).put(any<List<Put>>())
            verify(table, times(2)).close()
            verifyNoMoreInteractions(table)
            verify(successTimer, times(2)).labels(any())
            verifyNoMoreInteractions(successTimer)
            verify(successChild, times(2)).time(any<Callable<*>>())
            verifyNoMoreInteractions(successChild)
            verifyNoMoreInteractions(successTimer)

            verify(retriesCounter, times(1)).labels(any())
            verifyNoMoreInteractions(retriesCounter)
            verify(retriesChild, times(1)).inc()
            verifyNoMoreInteractions(retriesChild)
            verifyNoMoreInteractions(failureCounter)
            verifyZeroInteractions(failureCounter)
        }

        "Fails after max tries" {

            val maxRetries = 3
            val expectedRetryMaxAttempts = Config.Retry.maxAttempts

            val table = mock<Table> {
                on { put(any<List<Put>>()) } doThrow IOException(errorMessage)
            }

            val connection = mock<Connection> {
                on { admin } doReturn adm
                on { getTable(TableName.valueOf(qualifiedTableName)) } doReturn table
            }

            val successChild = summaryChild()
            val successTimer = summary(successChild)
            val retriesChild = mock<Counter.Child>()
            val retriesCounter = counter(retriesChild)
            val failureChild = mock<Counter.Child>()
            val failureCounter = counter(failureChild)
            val exception = shouldThrow<IOException> {
                with(HbaseClient(connection, columnFamily, columnQualifier, regionReplication,
                    successTimer, retriesCounter, failureCounter)) {
                    putList(qualifiedTableName, hbasePayloads())
                }
            }

            exception.message shouldBe errorMessage
            verify(table, times(maxRetries)).put(any<List<Put>>())
            verify(table, times(maxRetries)).close()
            verify(table, times(expectedRetryMaxAttempts)).put(any<List<Put>>())
            verifyNoMoreInteractions(table)
            verify(successTimer, times(3)).labels(any())
            verifyNoMoreInteractions(successTimer)
            verify(retriesCounter, times(3)).labels(any())
            verify(retriesChild, times(3)).inc()
            verifyNoMoreInteractions(retriesCounter)
            verifyNoMoreInteractions(retriesChild)
            verify(failureCounter, times(1)).labels(any())
            verify(failureChild, times(1)).inc()
            verifyNoMoreInteractions(failureCounter)
            verifyNoMoreInteractions(failureChild)
        }

        "Table not created" {
            val connection = mock<Connection> {
                on { admin } doReturn adm
            }

            val hbaseClient = HbaseClient(connection, "cf".toByteArray(), "record".toByteArray(), 2, mock(), mock(), mock())
            hbaseClient.ensureTable("$namespace:$tableQualifier")
            verify(adm, times(0)).createNamespace(any())
            verify(adm, times(0)).createTable(any(), any())
        }

        "Namespace and table created" {
            val connection = mock<Connection> {
                on { admin } doReturn adm
            }

            val dataFamily = "cf".toByteArray()
            val dataQualifier = "record".toByteArray()
            val hbaseRegionReplication = 3
            val hbaseClient = HbaseClient(connection, dataFamily, dataQualifier, hbaseRegionReplication, mock(), mock(), mock())
            val newNamespace = "ns2"
            val newTableQualifier = "table2"
            val newQualifiedTableName = "$newNamespace:$newTableQualifier"
            val splits = RegionKeySplitter.calculateSplits(2).toTypedArray()

            hbaseClient.ensureTable(newQualifiedTableName)

            verify(adm, times(1)).createNamespace(any())

            val tableDescriptor = HTableDescriptor(TableName.valueOf(newQualifiedTableName)).apply {
                addFamily(HColumnDescriptor(dataFamily)
                    .apply {
                        maxVersions = Int.MAX_VALUE
                        minVersions = 1
                        compressionType = Algorithm.GZ
                        compactionCompressionType = Algorithm.GZ
                    })
                regionReplication = hbaseRegionReplication
            }

            val splitsCaptor = argumentCaptor<Array<ByteArray>>()
            argumentCaptor<HTableDescriptor> {
                verify(adm, times(1)).createTable(capture(), splitsCaptor.capture())
                firstValue shouldBe tableDescriptor
                splitsCaptor.firstValue shouldBe splits
            }

        }

        "Namespace not created but table is created" {
            val newNamespaceDescriptor = mock<NamespaceDescriptor> {
                on { name } doReturn namespace
            }

            val newAdm = mock<Admin> {
                on { listNamespaceDescriptors() } doReturn arrayOf(newNamespaceDescriptor)
                on { listTableNames() } doReturn arrayOf(tableName)
            }

            val connection = mock<Connection> {
                on { admin } doReturn newAdm
            }

            val dataFamily = "cf".toByteArray()
            val dataQualifier = "record".toByteArray()
            val hbaseRegionReplication = 3
            val hbaseClient = HbaseClient(connection, dataFamily, dataQualifier, hbaseRegionReplication, mock(), mock(), mock())
            val newTableQualifier = "table2"
            val newQualifiedTableName = "$namespace:$newTableQualifier"
            val splits = RegionKeySplitter.calculateSplits(2).toTypedArray()
            hbaseClient.ensureTable(newQualifiedTableName)

            verify(newAdm, times(0)).createNamespace(any())

            val tableDescriptor = HTableDescriptor(TableName.valueOf(newQualifiedTableName)).apply {
                addFamily(HColumnDescriptor(dataFamily)
                    .apply {
                        maxVersions = Int.MAX_VALUE
                        minVersions = 1
                        compressionType = Algorithm.GZ
                        compactionCompressionType = Algorithm.GZ
                    })
                regionReplication = hbaseRegionReplication
            }

            val splitsCaptor = argumentCaptor<Array<ByteArray>>()
            argumentCaptor<HTableDescriptor> {
                verify(newAdm, times(1)).createTable(capture(), splitsCaptor.capture())
                firstValue shouldBe tableDescriptor
                splitsCaptor.firstValue shouldBe splits
            }
        }
    }

    private val columnFamily = "cf".toByteArray()
    private val columnQualifier = "record".toByteArray()
    private val tableQualifier = "table"
    private val regionReplication = 3
    private val namespace = "ns"
    private val qualifiedTableName = "$namespace:$tableQualifier"
    private val tableName: TableName =
        TableName.valueOf(ByteBuffer.wrap(namespace.toByteArray()), ByteBuffer.wrap(tableQualifier.toByteArray()))
    private val errorMessage = "ERROR"
    private val namespaceDescriptor = mock<NamespaceDescriptor> {
        on { name } doReturn namespace
    }

    private val adm = mock<Admin> {
        on { listNamespaceDescriptors() } doReturn arrayOf(namespaceDescriptor)
        on { listTableNames() } doReturn arrayOf(tableName)
    }

    private fun hbasePayloads(): List<HbasePayload> =
        listOf(HbasePayload(ByteArray(1), ByteArray(1), "id", 0L, "versionCreatedFrom", "versionRaw", consumerRecord(), 0L, 0L))

    private fun consumerRecord(): ConsumerRecord<ByteArray, ByteArray> =
            mock {
                on { topic() } doReturn ""
                on { partition() } doReturn 1
            }
}

