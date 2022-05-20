package utils

import de.esymetric.jerusalem.utils.BufferedRandomAccessFile
import org.junit.Assert
import org.junit.Test
import java.io.File

class BufferedRandomAccessFileTest {
    @Test
    @Throws(Exception::class)
    fun testBufferedRandomAccessFile() {
        File("testData/braf.data").delete()
        val braf = BufferedRandomAccessFile()
        braf.open("testData/braf.data", "rw")
        for (i in 0..9999) braf.writeInt(-1)
        braf.close()
        braf.open("testData/braf.data", "rw")
        braf.seek(20000)
        for (i in 0..9999) braf.writeInt(-1)
        braf.close()
        Assert.assertEquals(60000, File("testData/braf.data").length())
        braf.open("testData/braf.data", "rw")
        run {
            var i = 11
            while (i < 2000) {
                braf.seek(i)
                braf.writeInt(i)
                i += 10
            }
        }
        run {
            var i = 11
            while (i < 2000) {
                braf.seek(i)
                Assert.assertTrue(braf.readInt() == i)
                i += 10
            }
        }
        run {
            var i = 55
            while (i < 2009) {
                braf.seek(i)
                braf.writeInt(i)
                i += 9
            }
        }
        braf.close()
        braf.open("testData/braf.data", "rw")
        var i = 55
        while (i < 2009) {
            braf.seek(i)
            Assert.assertTrue(braf.readInt() == i)
            i += 9
        }
        braf.close()
        braf.open("testData/braf.data", "rw")

        run {
            var i = 0
            while (i < 65000) {
                braf.seek(i)
                braf.writeUShort(i.toUShort())
                i += 59
            }
        }
        braf.close()
        braf.open("testData/braf.data", "rw")

        i = 0
        while (i < 65000) {
            braf.seek(i)
            Assert.assertEquals(braf.readUShort(), i.toUShort())
            i += 59
        }
        braf.close()
    }
}