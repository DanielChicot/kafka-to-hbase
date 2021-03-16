
import MetricsMocks.counter
import MetricsMocks.summary
import MetricsMocks.summaryChild
import com.nhaarman.mockitokotlin2.*
import io.kotest.core.spec.style.StringSpec
import io.prometheus.client.Counter
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.sql.Connection
import java.sql.PreparedStatement
import java.util.concurrent.Callable
import kotlin.time.ExperimentalTime


@ExperimentalTime
class MetadataStoreClientTest: StringSpec() {

    init {
        "Batch insert with autocommit" {
            testAutoCommit(true)
        }

        "Batch insert without autocommit" {
            testAutoCommit(false)
        }

        "Single insert" {
            val statement = mock<PreparedStatement>()
            val sql = insertSql()

            val connection = mock<Connection> {
                on { prepareStatement(sql) } doReturn statement
            }

            val client = MetadataStoreClient(connection, mock(), mock(), mock())
            val partition = 1
            val offset = 2L
            val id = "ID"
            val topic = "db.database.collection"

            val record: ConsumerRecord<ByteArray, ByteArray> = mock {
                on { topic() } doReturn topic
                on { partition() } doReturn partition
                on { offset() } doReturn offset
            }

            val lastUpdated = 1L
            client.recordProcessingAttempt(id, record, lastUpdated)
            verify(connection, times(1)).prepareStatement(sql)
            verifyNoMoreInteractions(connection)
            verify(statement, times(1)).setString(1, id)
            verify(statement, times(1)).setLong(2, lastUpdated)
            verify(statement, times(1)).setString(3, topic)
            verify(statement, times(1)).setInt(4, partition)
            verify(statement, times(1)).setLong(5, offset)
            verify(statement, times(1)).executeUpdate()
            verifyNoMoreInteractions(statement)
        }
    }


    private suspend fun testAutoCommit(autoCommit: Boolean) {
        val statement = mock<PreparedStatement>()
        val sql = insertSql()

        val connection = mock<Connection> {
            on { prepareStatement(sql) } doReturn statement
            on { getAutoCommit() } doReturn autoCommit
        }

        val successChild = summaryChild()
        val successTimer = summary(successChild)
        val retryChild = mock<Counter.Child>()
        val retriesCounter = counter(retryChild)
        val failureChild = mock<Counter.Child>()
        val failureCounter = counter(failureChild)
        val client = MetadataStoreClient(connection, successTimer, retriesCounter, failureCounter)

        val payloads = (1..100).map { payloadNumber ->
            val record: ConsumerRecord<ByteArray, ByteArray> = mock {
                on { topic() } doReturn "db.database.collection$payloadNumber"
                on { partition() } doReturn payloadNumber
                on { offset() } doReturn payloadNumber.toLong()
            }
            HbasePayload("key-$payloadNumber".toByteArray(), "body-$payloadNumber".toByteArray(), "id-$payloadNumber",
                payloadNumber.toLong(), "_lastModifiedDateTime", "2020-01-01T00:00:00.000", record, 1000L, 2000L)
        }

        client.recordBatch(payloads)
        verify(successTimer, times(1)).labels(any())
        verify(successChild, times(1)).time(any<Callable<*>>())
        verifyNoMoreInteractions(successTimer)
        verifyZeroInteractions(retriesCounter)
        verifyZeroInteractions(failureCounter)
        verify(connection, times(1)).prepareStatement(sql)
        verify(connection, times(1)).autoCommit
        if (!autoCommit) {
            verify(connection, times(1)).commit()
        }
        verifyNoMoreInteractions(connection)

        val textUtils = TextUtils()
        for (i in 1..100) {
            verify(statement, times(1)).setString(1, textUtils.printableKey("key-$i".toByteArray()))
            verify(statement, times(1)).setLong(2, i.toLong())
            verify(statement, times(1)).setString(3, "db.database.collection$i")
            verify(statement, times(1)).setInt(4, i)
            verify(statement, times(1)).setLong(5, i.toLong())
        }
        verify(statement, times(100)).addBatch()
        verify(statement, times(1)).executeBatch()
        verify(statement, times(1)).close()
        verifyNoMoreInteractions(statement)
    }

    private fun insertSql(): String {
        return """
            INSERT INTO ucfs (hbase_id, hbase_timestamp, topic_name, kafka_partition, kafka_offset)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
    }
}
