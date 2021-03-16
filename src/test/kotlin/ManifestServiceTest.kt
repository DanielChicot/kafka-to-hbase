
import MetricsMocks.counter
import MetricsMocks.summary
import MetricsMocks.summaryChild
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectRequest
import com.nhaarman.mockitokotlin2.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.prometheus.client.Counter
import io.prometheus.client.Summary
import org.apache.hadoop.hbase.util.Bytes
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.io.InputStreamReader
import java.io.LineNumberReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ManifestServiceTest : StringSpec() {
    init {
        "Put manifest file set request parameters correctly" {
            val amazonS3 = mock<AmazonS3>()
            val successChild = summaryChild()
            val successTimer = summary(successChild)
            val retryChild = mock<Counter.Child>()
            val retriesCounter = counter(retryChild)
            val failureChild = mock<Counter.Child>()
            val failureCounter = counter(failureChild)
            val service = ManifestService(amazonS3, successTimer, retriesCounter, failureCounter)
            val payloads = hbasePayloads()
            service.putManifestFile(payloads)
            verifyMetricsInteractions(successTimer, successChild, retriesCounter, failureCounter)
            argumentCaptor<PutObjectRequest> {
                verify(amazonS3, times(1)).putObject(capture())
                verifyNoMoreInteractions(amazonS3)
                val request = firstValue
                request.bucketName shouldBe "manifests"
                request.key shouldBe "streaming/db.database-one.collection_one_10_1-db.database-one.collection_one_10_100.txt"
                val lineReader = LineNumberReader(InputStreamReader(request.inputStream))

                var lineCount = 0
                lineReader.forEachLine {
                    it shouldBe manifestBody(lineReader.lineNumber).replace('\n', ' ')
                    lineCount++
                }
                lineCount shouldBe payloads.count()
            }
        }

        "Put manifest file set user metadata correctly" {
            val amazonS3 = mock<AmazonS3>()
            val successChild = summaryChild()
            val successes = summary(successChild)
            val retryChild = mock<Counter.Child>()
            val retriesCounter = counter(retryChild)
            val failureChild = mock<Counter.Child>()
            val failureCounter = counter(failureChild)
            val service = ManifestService(amazonS3, successes, retriesCounter, failureCounter)
            val payloads = hbasePayloads()
            service.putManifestFile(payloads)
            verifyMetricsInteractions(successes, successChild, retriesCounter, failureCounter)
            argumentCaptor<PutObjectRequest> {
                verify(amazonS3, times(1)).putObject(capture())
                verifyNoMoreInteractions(amazonS3)
            }
        }
    }

    private fun verifyMetricsInteractions(summary: Summary,
                                          child: Summary.Child,
                                          retryCounter: Counter,
                                          failureCounter: Counter) {
        verify(summary, times(1)).labels(any())
        verifyNoMoreInteractions(summary)
        verify(child, times(1)).time(any<Callable<*>>())
        verifyZeroInteractions(retryCounter)
        verifyZeroInteractions(failureCounter)
    }

    private fun hbasePayloads(): List<HbasePayload>
            = (1..100).map { index ->
                val consumerRecord = mock<ConsumerRecord<ByteArray, ByteArray>> {
                    on { key() } doReturn index.toString().toByteArray()
                    on { topic() } doReturn "db.database-one.collection_one"
                    on { offset() } doReturn index.toLong()
                    on { partition() } doReturn 10
                }
                if(index % 2 == 0) {
                    HbasePayload(Bytes.toBytes("key-$index"), messageBody(index).toByteArray(), "{\"id\":\"id-$index\"}", payloadTime(index), "_lastModifiedDateTime", "2020-01-01T00:00:00.000", consumerRecord, 1000L, 2000L)
                } else {
                    HbasePayload(Bytes.toBytes("key-$index"), messageBody(index).toByteArray(), "id-$index", payloadTime(index), "_lastModifiedDateTime", "2020-01-01T00:00:00.000", consumerRecord, 1000L, 2000L)
                }
            }

    private fun messageBody(index: Int) =
        """
        {
            "message": {
                "dbObject": "abcdefghijklmnopqrstuvwxyz" 
            },
            "position": $index 
        }
        """.trimIndent()

    private fun manifestBody(index: Int) =
        if(index % 2 == 0) {
            """
            id-$index|${payloadTime(index)}|database-one|collection_one|STREAMED|K2HB|"{""id"":""id-$index""}"|KAFKA_RECORD
            """.trimIndent()
        } else {
            """
            id-$index|${payloadTime(index)}|database-one|collection_one|STREAMED|K2HB|id-$index|KAFKA_RECORD
            """.trimIndent()
        }


    private fun payloadTime(index: Int) = payloadTimestamp(index).time
    private fun payloadTimestamp(index: Int) = dateFormat().parse(payloadDate(index))
    private fun dateFormat() = SimpleDateFormat("yyyy/MM/dd").apply { timeZone = TimeZone.getTimeZone("UTC") }
    private fun payloadDate(index: Int) = "2020/01/%02d".format((index % 20) + 1)
}
