package osmDataRepresentation.osm2ownMaps

import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.PartitionedOsmNodeID2OwnIDMap
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir.Companion.deleteAllLatLonDataFiles
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PartitionedOsmNodeID2OwnIDMapTest {
    @Test
    fun testPartitionedOsmNodeID2OwnIDMap() {
        deleteAllLatLonDataFiles("testData")
        val map = PartitionedOsmNodeID2OwnIDMap(
            "testData", false
        )
        for (i in 0..24) {
            val lat = Math.random() * 180 - 90
            val lng = Math.random() * 360 - 180
            val key = (Math.random() * 788823).toInt()
            val value = (Math.random() * 788823548).toInt()
            map.put(lat, lng, key.toLong(), value)
            assertTrue(map[key.toLong()] == value)
        }
        deleteAllLatLonDataFiles("testData")
    }
}