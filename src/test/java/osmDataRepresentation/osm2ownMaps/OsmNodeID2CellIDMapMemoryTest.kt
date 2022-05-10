package osmDataRepresentation.osm2ownMaps

import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.OsmNodeID2CellIDMapMemory
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir
import org.junit.Assert
import org.junit.Test

class OsmNodeID2CellIDMapMemoryTest {
    @Test
    @Throws(Exception::class)
    fun testOsmNodeID2CellIDMap() {
        val filePath = "testData/osmNodeID2CellIDMap.data"
        var map = OsmNodeID2CellIDMapMemory()
        val lld = LatLonDir(48.12345, 11.92737)
        map.put(1234, lld)
        Assert.assertTrue(lld.equals(map[1234]))
        map.save(filePath)
        map = OsmNodeID2CellIDMapMemory()
        map.load(filePath)
        Assert.assertTrue(lld.equals(map[1234]))
    }
}