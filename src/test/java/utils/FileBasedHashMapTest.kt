package utils

import de.esymetric.jerusalem.utils.FileBasedHashMap
import de.esymetric.jerusalem.utils.FileBasedHashMapForLongKeys
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class FileBasedHashMapTest {
    @Test
    fun testFileBasedHashMap() {
        File("testData/fileBasedHashMapTest.data").delete()
        val fbhm = FileBasedHashMap()
        fbhm.open("testData/fileBasedHashMapTest.data", false)
        run {
            var id = 1000000
            while (id > 0) {
                val value = (Math.random() * 788823548).toInt()
                fbhm.put(id, value)
                assertTrue(fbhm[id] == value)
                id -= 111
            }
        }
        run {
            var id = 1000001
            while (id < 1286746) {
                val value = (Math.random() * 788823548).toInt()
                fbhm.put(id, value)
                if (fbhm[id] != value) {
                    fbhm.put(id, value)
                    assertTrue(fbhm[id] == value)
                }
                assertTrue(fbhm[id] == value)
                id += 263
            }
        }
        var id = 2000001
        while (id < 3000000) {
            val key = id
            val value = (Math.random() * 788823548).toInt()
            fbhm.put(key, value)
            assertTrue(fbhm[key] == value)
            id += 555
        }
        fbhm.close()
        File("testData/fileBasedHashMapTest.data").delete()
    }

    @Test
    @Throws(Exception::class)
    fun testFileBasedHashMapForLongKeys() {
        File("testData/fileBasedHashMapTestForLongKeys.data").delete()
        val fbhm = FileBasedHashMapForLongKeys()
        fbhm.open("testData/fileBasedHashMapTestForLongKeys.data", false)
        run {
            var id = 1000000
            while (id > 0) {
                val value = (Math.random() * 788823548).toInt()
                fbhm.put(id.toLong(), value)
                assertTrue(fbhm[id.toLong()] == value)
                id -= 111
            }
        }
        run {
            var id = 1000001
            while (id < 1286746) {
                val value = (Math.random() * 788823548).toInt()
                fbhm.put(id.toLong(), value)
                if (fbhm[id.toLong()] != value) {
                    fbhm.put(id.toLong(), value)
                    assertTrue(fbhm[id.toLong()] == value)
                }
                assertTrue(fbhm[id.toLong()] == value)
                id += 263
            }
        }
        var id = 3000000000L
        while (id < 3300000000L) {
            val key = id
            val value = (Math.random() * 788823548).toInt()
            fbhm.put(key, value)
            assertTrue(fbhm[key] == value)
            id += 23555L
        }
        fbhm.close()
        File("testData/fileBasedHashMapTestForLongKeys.data").delete()
    }
}