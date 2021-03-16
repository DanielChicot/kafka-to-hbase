
import MetricsMocks.counter
import MetricsMocks.summary
import MetricsMocks.summaryChild
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectRequest
import com.nhaarman.mockitokotlin2.*
import io.kotest.assertions.json.shouldMatchJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.prometheus.client.Counter
import org.apache.hadoop.hbase.util.Bytes
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.io.InputStreamReader
import java.io.LineNumberReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.zip.GZIPInputStream
import kotlin.time.ExperimentalTime

@ExperimentalTime
class CorporateStorageServiceTest : StringSpec() {
    init {
        "Batch puts set request parameters correctly" {
            val amazonS3 = mock<AmazonS3>()

            val successChild = summaryChild()
            val successTimer = summary(successChild)
            val retryChild = mock<Counter.Child>()
            val retriesCounter = counter(retryChild)
            val failureChild = mock<Counter.Child>()
            val failureCounter = counter(failureChild)
            val service = CorporateStorageService(amazonS3, successTimer, retriesCounter, failureCounter)
            val payloads = hbasePayloads()

            service.putBatch("database:collection", payloads)

            verify(successTimer, times(1)).labels(any())
            verify(successChild, times(1)).time(any<Callable<*>>())
            verifyNoMoreInteractions(successTimer)
            verifyZeroInteractions(retriesCounter)
            verifyZeroInteractions(retryChild)
            verifyZeroInteractions(failureCounter)
            verifyZeroInteractions(failureChild)

            argumentCaptor<PutObjectRequest> {
                verify(amazonS3, times(1)).putObject(capture())
                verifyNoMoreInteractions(amazonS3)
                val request = firstValue
                request.bucketName shouldBe "ucarchive"
                request.key shouldBe "ucdata_main/${today()}/database/collection/db.database.collection_10_0-99.jsonl.gz"
                LineNumberReader(InputStreamReader(GZIPInputStream(request.inputStream))).use { lineReader ->
                    lineReader.forEachLine {
                        it shouldMatchJson messageBody(lineReader.lineNumber - 1)
                    }
                }
            }
        }
    }

    private fun hbasePayloads(): List<HbasePayload>
            = List(100) { index ->
                val consumerRecord = mock<ConsumerRecord<ByteArray, ByteArray>> {
                    on { key() } doReturn index.toString().toByteArray()
                    on { topic() } doReturn "db.database.collection"
                    on { offset() } doReturn index.toLong()
                    on { partition() } doReturn 10
                }
                HbasePayload(Bytes.toBytes("key-$index"), messageBody(index).toByteArray(), "testId1", payloadTime(index), "_lastModifiedDateTime", "2020-01-01T00:00:00.000", consumerRecord, 1000L, 2000L)
            }

    private fun messageBody(index: Int) =
        """
        {
            "message": {
                "dbObject": "abcdefghijklmnopqrstuvwxyz" 
            },
            "position": $index 
        }
        """

    private fun today() = dateFormat().format(Date())
    private fun payloadTime(index: Int) = payloadTimestamp(index).time
    private fun payloadTimestamp(index: Int) = dateFormat().parse(payloadDate(index))
    private fun dateFormat() = SimpleDateFormat("yyyy/MM/dd").apply { timeZone = TimeZone.getTimeZone("UTC") }
    private fun payloadDate(index: Int) = "2020/01/%02d".format((index % 20) + 1)
}
