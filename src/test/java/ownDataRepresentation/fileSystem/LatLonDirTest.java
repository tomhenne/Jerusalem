package ownDataRepresentation.fileSystem;

import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class LatLonDirTest {

    @Test
    public void testLanLonDir() throws Exception {
        for (double lat = -89; lat < 89; lat += 0.5)
            for (double lng = -89; lng < 89; lng += 0.3) {
                boolean ok = new LatLonDir(lat, lng).equals(new LatLonDir(
                        new LatLonDir(lat, lng).getShortKey()));
                assertTrue(ok);
                // System.out.println("" + lat + " " + lng + " " + ok);
            }

        checkOffsetBits(48, 11, 48, 11);

        checkOffsetBits(48, 11, 49, 11);
        checkOffsetBits(48, 11, 49, 12);
        checkOffsetBits(48, 11, 48, 12);
        checkOffsetBits(48, 11, 48, 10);

        checkOffsetBits(48, 11, 47, 10);
        checkOffsetBits(48, 11, 47, 12);
        checkOffsetBits(48, 11, 47, 11);
        checkOffsetBits(48, 11, 49, 10);

    }


    private void checkOffsetBits(int lat1, int lng1, int lat2, int lng2) {
        LatLonDir lld1 = new LatLonDir(lat1, lng1);
        int offsetBits = lld1.getOffsetBits(lat2, lng2);
        LatLonDir lld3 = new LatLonDir(lat1, lng1, offsetBits);
        assertTrue(new LatLonDir(lat2, lng2).equals(lld3));
    }

}
