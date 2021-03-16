import com.nhaarman.mockitokotlin2.mock
import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ShovelTest : StringSpec() {

    init {
        "batchCount is a multiple of reportFrequency" {
            val batchCount = 100
            val isMultiple = Shovel(mock(), mock(), mock())
                .batchCountIsMultipleOfReportFrequency(batchCount)

            assertTrue(isMultiple)
        }

        "batchCount is not a multiple of reportFrequency" {
            val batchCount = 101
            val isMultiple = Shovel(mock(), mock(), mock())
                .batchCountIsMultipleOfReportFrequency(batchCount)

            assertFalse(isMultiple)
        }
    }
}
