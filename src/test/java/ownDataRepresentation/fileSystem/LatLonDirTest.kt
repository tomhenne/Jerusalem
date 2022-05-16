package ownDataRepresentation.fileSystem

import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir
import junit.framework.Assert.assertNull
import org.junit.Assert
import org.junit.Test

class LatLonDirTest {
    @Test
    @Throws(Exception::class)
    fun testLanLonDir() {
        var lat = -89.0
        while (lat < 89) {
            var lng = -89.0
            while (lng < 89) {
                val ok = LatLonDir(lat, lng).equals(
                    LatLonDir(
                        LatLonDir(lat, lng).shortKey
                    )
                )
                Assert.assertTrue(ok)
                lng += 0.3
            }
            lat += 0.5
        }
        checkOffsetBits(48, 11, 48, 11)
        checkOffsetBits(48, 11, 49, 11)
        checkOffsetBits(48, 11, 49, 12)
        checkOffsetBits(48, 11, 48, 12)
        checkOffsetBits(48, 11, 48, 10)
        checkOffsetBits(48, 11, 47, 10)
        checkOffsetBits(48, 11, 47, 12)
        checkOffsetBits(48, 11, 47, 11)
        checkOffsetBits(48, 11, 49, 10)
        checkOffsetBitsNotNeighbouring(48, 11, 46, 11)
        checkOffsetBitsNotNeighbouring(48, 11, 49, 13)
    }

    private fun checkOffsetBits(lat1: Int, lng1: Int, lat2: Int, lng2: Int) {
        val lld1 = LatLonDir(lat1.toDouble(), lng1.toDouble())
        val offsetBits = lld1.getOffsetBits(lat2.toDouble(), lng2.toDouble())
        val lld3 = LatLonDir(lat1.toDouble(), lng1.toDouble(), offsetBits!!)
        Assert.assertTrue(LatLonDir(lat2.toDouble(), lng2.toDouble()).equals(lld3))
    }

    private fun checkOffsetBitsNotNeighbouring(lat1: Int, lng1: Int, lat2: Int, lng2: Int) {
        val lld1 = LatLonDir(lat1.toDouble(), lng1.toDouble())
        val offsetBits = lld1.getOffsetBits(lat2.toDouble(), lng2.toDouble())
        assertNull(offsetBits)
    }

}