package routing;

import de.esymetric.jerusalem.ownDataRepresentation.Node;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport.KML;
import de.esymetric.jerusalem.routing.RoutingType;

import java.io.File;
import java.util.List;
import java.util.Vector;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RouterTest {

    public void makeRoute(de.esymetric.jerusalem.routing.Router router, double lat1, double lng1, double lat2,
                   double lng2, String name, String dataDirectoryPath) {
        for (RoutingType rt : RoutingType.values())
            makeRoute(router, rt.name(), lat1, lng1, lat2, lng2, name,
                    dataDirectoryPath);
    }

    void makeRoute(de.esymetric.jerusalem.routing.Router router, String routingType, double lat1, double lng1,
                   double lat2, double lng2, String name, String dataDirectoryPath) {
        System.out.println("---------------------------------------------");
        System.out
                .println("Computing Route " + name + " (" + routingType + ")");
        System.out.println("---------------------------------------------");

        List<Node> route = router
                .findRoute(routingType, lat1, lng1, lat2, lng2);
        assertNotNull(route);
        if (route == null) {
            System.out.println("ERROR: no route found for " + name + " ("
                    + routingType + ")");
            return;
        }

        System.out.println();

        KML kml = new KML();
        Vector<Position> trackPts = new Vector<Position>();
        for (Node n : route) {
            Position p = new Position();
            p.latitude = n.lat;
            p.longitude = n.lng;
            trackPts.add(p);
        }
        kml.setTrackPositions(trackPts);
        kml.Save(dataDirectoryPath + File.separatorChar + name + "-"
                + routingType + ".kml");

        assertTrue(trackPts.size() > 10);
    }
}
