package ownDataRepresentation.fileSystem;

import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedQuadtreeNodeIndexFile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PartitionedQuadtreeNodeIndexTest {

    @Test
    public void testPartionedQuadtreeNodeIndex() throws Exception {
        PartitionedQuadtreeNodeIndexFile pf = new PartitionedQuadtreeNodeIndexFile("testData", false, true);

        pf.setID(48.11,  11.48, 77);
        assertEquals(77, pf.getID(48.11,  11.48));


        pf.setID(48.11,  11.48, 55);
        pf.setID(33.11,  22.48, 44);

        assertEquals(55, pf.getID(48.11,  11.48));
        assertEquals(44, pf.getID(33.11,  22.48));


        for( double lat = 50.0; lat < 50.4; lat += 0.00001)
            for( double lng = 11.0; lng < 12; lng += 0.009)
                pf.setID(lat,  lng, (int)lat * (int)(lng * 100.0));

        for( double lat = 50.0; lat < 50.4; lat += 0.00001)
            for( double lng = 11.0; lng < 12; lng += 0.009) {
                assertEquals((int)lat * (int)(lng * 100.0), pf.getID(lat,  lng ));
            }

        for( double lat = 50.0; lat < 60; lat += 0.3)
            for( double lng = -120.0; lng < 60; lng += 0.8)
                pf.setID(lat,  lng, (int)(lat * lng));

        for( double lat = 50.0; lat < 60; lat += 0.3)
            for( double lng = -120.0; lng < 60; lng += 0.8)
                assertEquals((int)(lat * lng), pf.getID(lat,  lng ));

        for( double lat = 50.0; lat < 60; lat += 0.3)
            for( double lng = -120.0; lng < 60; lng += 0.8)
                pf.setID(lat,  lng, (int)(lat * lng));

        for( double lat = 50.0; lat < 60; lat += 0.3)
            for( double lng = -120.0; lng < 60; lng += 0.8)
                assertEquals((int)(lat * lng), pf.getID(lat,  lng ));
    }

}
