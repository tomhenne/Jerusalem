package utils

import de.esymetric.jerusalem.utils.BufferedRandomAccessFileCache
import org.junit.Assert
import org.junit.Test

class BufferedRandomAccessFileCacheTest {
    @Test
    @Throws(Exception::class)
    fun testBufferedRandomAccessFileCache() {
        val rafCache = BufferedRandomAccessFileCache()
        for (i in 0..30) {
            val raf = rafCache.getRandomAccessFile(
                "testData/rafCache$i.data", false
            )
            for (j in 0..199999) {
                val p = (Math.random() * 20000).toInt()
                raf!!.seek(p)
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
            Assert.assertEquals(333, raf.readInt().toLong())
        }
        rafCache.close()
    }
}