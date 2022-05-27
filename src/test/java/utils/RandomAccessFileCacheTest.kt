package utils

import de.esymetric.jerusalem.utils.RandomAccessFileCache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RandomAccessFileCacheTest {
    @Test
    @Throws(Exception::class)
    fun testRandomAccessFileCache() {
        val rafCache = RandomAccessFileCache()
        for (i in 0..30) {
            val raf = rafCache.getRandomAccessFile(
                "testData/rafCache$i.data", false
            )
            for (j in 0..199999) {
                val p = (Math.random() * 20000).toInt()
                raf!!.seek(p.toLong())
                raf.writeInt(j)
            }
            raf!!.seek(999)
            raf.writeInt(333)
        }
        for (i in 10 downTo 1) {
            val raf = rafCache.getRandomAccessFile(
                "testData/rafCache$i.data", false
            )
            raf!!.seek(999)
            assertEquals(333, raf.readInt().toLong())
        }
        rafCache.close()
    }
}