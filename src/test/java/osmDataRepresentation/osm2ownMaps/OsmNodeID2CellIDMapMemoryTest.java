package osmDataRepresentation.osm2ownMaps;

import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.OsmNodeID2CellIDMapMemory;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class OsmNodeID2CellIDMapMemoryTest {
    @Test
    public void testOsmNodeID2CellIDMap() throws Exception {

        String filePath = "testData/osmNodeID2CellIDMap.data";

        OsmNodeID2CellIDMapMemory map = new OsmNodeID2CellIDMapMemory();
        LatLonDir lld = new LatLonDir(48.12345, 11.92737);
        map.put(1234, lld);
        assertTrue(lld.equals(map.get(1234)));

        map.save(filePath);

        map = new OsmNodeID2CellIDMapMemory();
        map.load(filePath);

        assertTrue(lld.equals(map.get(1234)));
    }
}
