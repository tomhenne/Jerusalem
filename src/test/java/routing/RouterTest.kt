package routing

import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position
import de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport.KML
import de.esymetric.jerusalem.routing.Router
import de.esymetric.jerusalem.routing.RoutingType
import org.junit.Assert
import java.io.File
import java.util.*

class RouterTest {
    fun makeRoute(
        router: Router, lat1: Double, lng1: Double, lat2: Double,
        lng2: Double, name: String, dataDirectoryPath: String
    ) {
        for (rt in RoutingType.values()) makeRoute(
            router, rt.name, lat1, lng1, lat2, lng2, name,
            dataDirectoryPath
        )
    }

    fun makeRoute(
        router: Router, routingType: String, lat1: Double, lng1: Double,
        lat2: Double, lng2: Double, name: String, dataDirectoryPath: String
    ) {
        println("---------------------------------------------")
        println("Computing Route $name ($routingType)")
        println("---------------------------------------------")
        val route = router
            .findRoute(routingType, lat1, lng1, lat2, lng2)
        Assert.assertNotNull(route)
        if (route == null) {
            println(
                "ERROR: no route found for " + name + " ("
                        + routingType + ")"
            )
            return
        }
        println()
        val kml = KML()
        val trackPts = Vector<Position>()
        for (n in route) {
            val p = Position()
            p.latitude = n.lat
            p.longitude = n.lng
            trackPts.add(p)
        }
        kml.trackPositions = trackPts
        kml.Save(
            dataDirectoryPath + File.separatorChar + name + "-"
                    + routingType + ".kml"
        )
        Assert.assertTrue(trackPts.size > 10)
    }
}