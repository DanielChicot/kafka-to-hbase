import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions.assertEquals

class RegionKeySplitterTest : StringSpec({
    "Splits are equally sized" {
        for (regionCount in 1..10_000) {
            RegionKeySplitter.calculateSplits(regionCount).let { splits ->
                assertEquals(regionCount - 1, splits.size)
                gaps(splits).let { gaps ->
                    gaps.indices.forEach { index ->
                        assertEquals(expectedSize(regionCount, index), gaps[index])
                    }
                }
            }
        }
    }
})

private fun expectedSize(regionCount: Int, index: Int) =
    minimumSize(regionCount) + if (index < remainder(regionCount)) 1 else 0

private fun gaps(splits: List<ByteArray>) =
    splits.indices.map {
        val position1 = if (it == 0) 0 else position(splits[it - 1])
        val position2 = position(splits[it])
        position2 - position1
    }

private fun remainder(regionCount: Int): Int = keyspaceSize % regionCount
private fun minimumSize(regionCount: Int) = keyspaceSize / regionCount
private fun position(digits: ByteArray): Int = toUnsigned(digits[0]) * 256 + toUnsigned(digits[1])
private fun toUnsigned(byte: Byte): Int = if (byte < 0) (byte + 256) else byte.toInt()

private const val keyspaceSize = 256 * 256

