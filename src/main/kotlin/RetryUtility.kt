
import io.prometheus.client.Counter
import io.prometheus.client.Summary
import kotlinx.coroutines.delay
import uk.gov.dwp.dataworks.logging.DataworksLogger
import kotlin.time.ExperimentalTime

@ExperimentalTime
object RetryUtility {

    suspend fun <T> retry(timer: Summary, retryCounter: Counter, failureCounter: Counter, f: () -> T,
                          vararg labelValues: String): T {

        suspend fun go(attempts: Int): T =
            try {
                timer.labels(*labelValues).time(f)
            } catch (e: Exception) {
                logger.warn("Retryable function failed",
                    "attempt_number" to "$attempts",
                    "max_attempts" to "${Config.Retry.maxAttempts}",
                    "retry_delay" to "${attemptDelay(attempts)}",
                    "error_message" to "${e.message}")

                retryCounter.labels(*labelValues).inc()
                delay(attemptDelay(attempts))

                if (attempts > Config.Retry.maxAttempts - 1) {
                    failureCounter.labels(*labelValues).inc()
                    throw e
                }
                go(attempts + 1)
            }

        return go(1)
    }

    private fun attemptDelay(attempt: Int): Long =
        if (attempt == 0) {
            Config.Retry.initialBackoff
        } else {
            (Config.Retry.initialBackoff * attempt * Config.Retry.backoffMultiplier.toFloat()).toLong()
        }

    private val logger = DataworksLogger.getLogger(RetryUtility::class)
}
