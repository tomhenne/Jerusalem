package ownDataRepresentation.fileSystem

import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedQuadtreeNodeIndexFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PartitionedQuadtreeNodeIndexTest {
    @Test
    @Throws(Exception::class)
    fun testPartionedQuadtreeNodeIndex() {
        val pf = PartitionedQuadtreeNodeIndexFile("testData", false)
        pf.setID(48.11, 11.48, 77)
        assertEquals(77, pf.getID(48.11, 11.48).toLong())
        pf.setID(48.11, 11.48, 55)
        pf.setID(33.11, 22.48, 44)
        assertEquals(55, pf.getID(48.11, 11.48).toLong())
        assertEquals(44, pf.getID(33.11, 22.48).toLong())
        run {
            var lat = 50.0
            while (lat < 50.4) {
                var lng = 11.0
                while (lng < 12) {
                    pf.setID(lat, lng, lat.toInt() * (lng * 100.0).toInt())
                    lng += 0.009
                }
                lat += 0.00001
            }
        }
        run {
            var lat = 50.0
            while (lat < 50.4) {
                var lng = 11.0
                while (lng < 12) {
                    assertEquals((lat.toInt() * (lng * 100.0).toInt()).toLong(), pf.getID(lat, lng).toLong())
                    lng += 0.009
                }
                lat += 0.00001
            }
        }
        run {
            var lat = 50.0
            while (lat < 60) {
                var lng = -120.0
                while (lng < 60) {
                    pf.setID(lat, lng, (lat * lng).toInt())
                    lng += 0.8
                }
                lat += 0.3
            }
        }
        run {
            var lat = 50.0
            while (lat < 60) {
                var lng = -120.0
                while (lng < 60) {
                    assertEquals((lat * lng).toInt().toLong(), pf.getID(lat, lng).toLong())
                    lng += 0.8
                }
                lat += 0.3
            }
        }
        run {
            var lat = 50.0
            while (lat < 60) {
                var lng = -120.0
                while (lng < 60) {
                    pf.setID(lat, lng, (lat * lng).toInt())
                    lng += 0.8
                }
                lat += 0.3
            }
        }
        var lat = 50.0
        while (lat < 60) {
            var lng = -120.0
            while (lng < 60) {
                assertEquals((lat * lng).toInt().toLong(), pf.getID(lat, lng).toLong())
                lng += 0.8
            }
            lat += 0.3
        }
    }
}