package osmDataRepresentation.osm2ownMaps;

import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class PartitionedOsmNodeID2OwnIDMapTest {
    @Test
    public void testPartitionedOsmNodeID2OwnIDMap() {
        LatLonDir.deleteAllLatLonDataFiles("testData");
        de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.PartitionedOsmNodeID2OwnIDMap map = new de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.PartitionedOsmNodeID2OwnIDMap(
                "testData", false);
        for (int i = 0; i < 25; i++) {
            double lat = Math.random() * 180 - 90;
            double lng = Math.random() * 360 - 180;
            int key = (int) (Math.random() * 788823);
            int value = (int) (Math.random() * 788823548);

            map.put(lat, lng, key, value);
            assertTrue(map.get(key) == value);
        }
        LatLonDir.deleteAllLatLonDataFiles("testData");
    }
}
