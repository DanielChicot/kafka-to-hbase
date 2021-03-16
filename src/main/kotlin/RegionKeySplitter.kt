
import uk.gov.dwp.dataworks.logging.DataworksLogger

object RegionKeySplitter {
    fun calculateSplits(numberOfRegions: Int): List<ByteArray> {
        // We expect to make one less Split than the number of Regions required.
        // Each split will be offset, for example;
        // If the total bytes was 100 and we wanted 4 Regions, we would want 3  evenly spaced and sized splits:
        //   20->39, 40->59, 60->79

        logger.info("Calculating split", "regions" to "$numberOfRegions")

        // We use the first two bytes of the keys, so here need 256^2
        val totalBytes = 256 * 256
        val widthPerSplit = totalBytes / numberOfRegions
        var remainder = totalBytes % numberOfRegions
        val positions = mutableListOf<Int>()
        var previous = 0

        for (split in 0 until numberOfRegions - 1) {
            val next = previous + widthPerSplit + (if (remainder-- > 0) 1 else 0)
            positions.add(next)
            previous = next
        }

        return positions.map { bytePosition ->
            //this is the start position and end position of each slice, which should be contiguous
            byteArrayOf((bytePosition / 256).toByte(), (bytePosition % 256).toByte())
        }
    }

    val logger = DataworksLogger.getLogger(RegionKeySplitter::class)
}
