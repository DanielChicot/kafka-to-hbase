
import MetricsMocks.counter
import MetricsMocks.summary
import MetricsMocks.summaryChild
import RetryUtility.retry
import com.nhaarman.mockitokotlin2.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.prometheus.client.Counter
import io.prometheus.client.Summary
import java.util.concurrent.Callable
import kotlin.time.ExperimentalTime

@ExperimentalTime
class RetryUtilityTest: StringSpec() {
    init {
        "Does not retry on success" {
            verifyMetrics(mock(), 1, 1, 0, 0)
        }

        "Retries until successful" {
            verifyMetrics(mock {
                on { invoke() } doThrow RuntimeException("Error 1") doThrow RuntimeException("Error 2") doAnswer {}
            }, 3, 3, 2, 0)
        }

        "Eventually gives up" {
            verifyMetrics(mock {
                on { invoke() } doThrow RuntimeException("Error")
            }, 3, 3, 3, 1, false)
        }
    }

    private suspend fun verifyMetrics(f: () -> Unit,
                                      functionCalls: Int,
                                      successCalls: Int,
                                      retryCalls: Int,
                                      failureCalls: Int,
                                      succeeds: Boolean = true) {
        val child: Summary.Child = summaryChild()
        val summary = summary(child)
        val retryChild = mock<Counter.Child>()
        val retries = counter(retryChild)
        val failureChild = mock<Counter.Child>()
        val failures = counter(failureChild)

        if (succeeds) {
            retry(summary, retries, failures, f)
        } else {
            shouldThrow<java.lang.RuntimeException> {
                retry(summary, retries, failures, f)
            }
        }

        verify(f, times(functionCalls)).invoke()
        verify(summary, times(successCalls)).labels(any())
        verifyNoMoreInteractions(summary)
        verify(child, times(successCalls)).time(any<Callable<*>>())
        verifyNoMoreInteractions(child)
        verifyCounterCalls(retries, retryChild, retryCalls)
        verifyCounterCalls(failures, failureChild, failureCalls)
    }


    private fun verifyCounterCalls(counter: Counter, child: Counter.Child, expectedCalls: Int) {
        when (expectedCalls) {
            0 -> {
                verifyZeroInteractions(counter)
                verifyZeroInteractions(child)
            }
            else -> {
                verify(counter, times(expectedCalls)).labels(any())
                verifyNoMoreInteractions(counter)
                verify(child, times(expectedCalls)).inc()
                verifyNoMoreInteractions(child)
            }
        }
    }

}
