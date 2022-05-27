package de.esymetric.jerusalem.main

import de.esymetric.jerusalem.osmDataRepresentation.OSMDataReader
import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.PartitionedOsmNodeID2OwnIDMap
import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position
import de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport.KML
import de.esymetric.jerusalem.rebuilding.Rebuilder
import de.esymetric.jerusalem.routing.Router
import de.esymetric.jerusalem.routing.RoutingType
import de.esymetric.jerusalem.routing.algorithms.TomsAStarStarRouting
import de.esymetric.jerusalem.routing.heuristics.TomsRoutingHeuristics
import de.esymetric.jerusalem.tools.CrossroadsFinder
import de.esymetric.jerusalem.utils.Utils
import org.apache.tools.bzip2.CBZip2InputStream
import java.io.*
import java.util.*

object Jerusalem {
    @JvmStatic
    fun main(args: Array<String>) {
        println("JERUSALEM 0.95 Java Enabled Routing Using Speedy Algorithms for Largely Extended Maps (jerusalem.gps-sport.net) based on OSM (OpenStreetMap.org)")
        if (args.isEmpty()) {
            printUsage()
            return
        }
        val command = args[0]
        val maxExecutionTimeS = 120
        val dataDirectoryPath = (System.getProperty("user.dir")
                + File.separatorChar + "jerusalemData")
        var tempDirectoryPath = dataDirectoryPath
        File(dataDirectoryPath).mkdir()

        if ("clean" == command) {
            // remove temp files from osm 2 own id map
            if (args.size < 2) {
                printUsage()
                return
            }
            tempDirectoryPath = (args[1] + File.separatorChar
                    + "jerusalemTempData")
            File(tempDirectoryPath).mkdirs()
            val poniom = PartitionedOsmNodeID2OwnIDMap(
                tempDirectoryPath, true
            )
            poniom.deleteLatLonTempFiles()
            poniom.close()
            return
        }
        if ("rebuildIndex" == command) {
            if (args.size < 2) {
                printUsage()
                return
            }
            tempDirectoryPath = (args[1] + File.separatorChar
                    + "jerusalemTempData")
            val rebuilder = Rebuilder(
                dataDirectoryPath,
                tempDirectoryPath, TomsRoutingHeuristics(), true,
                false, true
            )
            rebuilder.makeQuadtreeIndex()
            // do NOT close Rebuilder rebuilder.close();
            return
        }
        if ("rebuildTransitions" == command) {
            if (args.size < 2) {
                printUsage()
                return
            }
            tempDirectoryPath = (args[1] + File.separatorChar
                    + "jerusalemTempData")
            val rebuilder = Rebuilder(
                dataDirectoryPath,
                tempDirectoryPath, TomsRoutingHeuristics(), true,
                false, false
            )
            rebuilder.buildTransitions(true)
            // do NOT close Rebuilder rebuilder.close();
            return
        }
        if ("rebuild" == command || "rebuildWays" == command) {
            val rebuildOnlyWays = "rebuildWays" == command
            if (args.size < 3) {
                printUsage()
                return
            }
            var filePath: String? = args[1]
            if ("-" != filePath) {
                println("Rebuilding $filePath")
            } else {
                println("Rebuilding from stdin")
                filePath = null
            }
            tempDirectoryPath = (args[2] + File.separatorChar
                    + "jerusalemTempData")
            File(tempDirectoryPath).mkdirs()
            val startTime = Date()
            println("start date $startTime")
            try {
                val rebuilder = Rebuilder(
                    dataDirectoryPath,
                    tempDirectoryPath, TomsRoutingHeuristics(),
                    rebuildOnlyWays, rebuildOnlyWays, false
                )
                if (filePath != null) {
                    val fis: InputStream = FileInputStream(filePath)
                    fis.read()
                    fis.read()
                    val bzis = CBZip2InputStream(fis)
                    val osmdr = OSMDataReader(
                        bzis, rebuilder,
                        rebuildOnlyWays
                    )
                    osmdr.read(startTime)
                    bzis.close()
                    fis.close()
                } else {
                    val osmdr = OSMDataReader(
                        System.`in`,
                        rebuilder, rebuildOnlyWays
                    )
                    osmdr.read(startTime)
                }
                rebuilder.finishProcessingAndClose()

            } catch (e: IOException) {
                e.printStackTrace()
            }

            println("finish date " + Date())
            println(
                "required time "
                        + Utils.formatTimeStopWatch(
                    Date().time
                            - startTime.time
                )
            )
            return
        }

        // routing
        if ("route" == command || "routeVerbose" == command || "routeWithTransitions" == command) {
            if (args.size < 6) {
                printUsage()
                return
            }
            val router = Router(
                dataDirectoryPath,
                TomsAStarStarRouting(), TomsRoutingHeuristics(),
                maxExecutionTimeS
            )
            Router.debugMode = "routeVerbose" == command
            val outputTransitions = "routeWithTransitions" == command
            val routingType = args[1]
            val lat1 = args[2].toDouble()
            val lng1 = args[3].toDouble()
            val lat2 = args[4].toDouble()
            val lng2 = args[5].toDouble()
            val route = router.findRoute(
                routingType, lat1, lng1, lat2,
                lng2
            )
            router.close()
            if (route != null) {
                for (n in route) {
                    val sb = StringBuilder()
                    sb.append(n.lat).append(',').append(n.lng)
                    if (outputTransitions) {
                        sb.append(',').append(n.numberOfTransitionsIfTransitionsAreLoaded)
                    }
                    println(sb.toString())
                }
                if (args.size > 6) {
                    val filename = args[6]
                    val kml = KML()
                    val trackPts = Vector<Position>()
                    for (n in route) {
                        val p = Position()
                        p.latitude = n.lat
                        p.longitude = n.lng
                        trackPts.add(p)
                    }
                    kml.trackPositions = trackPts
                    kml.save(
                        dataDirectoryPath + File.separatorChar + filename
                                + "-" + routingType + ".kml"
                    )
                }
            }
            return
        }

        // find crossroads
        if ("findCrossroads" == command) {
            if (args.size < 2) {
                printUsage()
                return
            }
            val routingType = args[1]
            val isr = InputStreamReader(System.`in`)
            val lnr = LineNumberReader(isr)
            val positions: MutableList<Position> = ArrayList()
            while (true) {
                var line: String? = null
                try {
                    line = lnr.readLine()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                if (line == null || "eof" == line) break
                val lp = line.split(",").toTypedArray()
                if (lp.size < 3) {
                    continue
                }
                val p = Position()
                p.latitude = lp[0].toDouble()
                p.longitude = lp[1].toDouble()
                p.altitude = lp[2].toDouble()
                positions.add(p)
            }
            val cf = CrossroadsFinder(dataDirectoryPath)
            cf.loadNumberOfCrossroads(positions, RoutingType.valueOf(routingType))
            for (p in positions) {
                val sb = StringBuilder()
                sb.append(p.latitude).append(',').append(p.longitude).append(',').append(p.altitude).append(',')
                    .append(p.nrOfTransitions.toInt())
                println(sb.toString())
            }
            return
        }

        // find crossroads test
        if ("findCrossroadsTest" == command) {
            if (args.size < 2) {
                printUsage()
                return
            }
            val routingType = args[1]
            val positions: MutableList<Position> = ArrayList()
            var pt = Position()
            pt.latitude = 48.116915489240476
            pt.longitude = 11.48764371871948
            pt.altitude = 600.0
            positions.add(pt)
            pt = Position()
            pt.latitude = 48.15
            pt.longitude = 11.55
            pt.altitude = 660.0
            positions.add(pt)
            val cf = CrossroadsFinder(dataDirectoryPath)
            cf.loadNumberOfCrossroads(positions, RoutingType.valueOf(routingType))
            for (p in positions) {
                val sb = StringBuilder()
                sb.append(p.latitude).append(',').append(p.longitude).append(',').append(p.altitude).append(',')
                    .append(p.nrOfTransitions.toInt())
                println(sb.toString())
            }
            return
        }

        // test routing
        if ("routingTest" == command) {
            val startTime = Date()
            println("routing test start date $startTime")
            val router = Router(
                dataDirectoryPath,
                TomsAStarStarRouting(), TomsRoutingHeuristics(),
                maxExecutionTimeS
            )
            Router.debugMode = true
            testRoute(
                router, 48.116915489240476, 11.48764371871948, 48.219297,
                11.372824, "hadern-a8", dataDirectoryPath
            )
            testRoute(
                router, 48.116915489240476, 11.48764371871948,
                48.29973956844243, 10.97055673599243, "hadern-kissing",
                dataDirectoryPath
            )
            testRoute(
                router, 48.125166, 11.451445, 48.12402, 11.515946, "a96",
                dataDirectoryPath
            )
            testRoute(
                router, 48.125166, 11.451445, 48.103516, 11.501441,
                "a96_Fuerstenrieder", dataDirectoryPath
            )
            testRoute(
                router, 48.09677, 11.323729, 48.393707, 11.841116,
                "autobahn", dataDirectoryPath
            )
            testRoute(
                router, 48.107891, 11.461865, 48.099986, 11.511051,
                "durch-waldfriedhof", dataDirectoryPath
            )
            testRoute(
                router, 48.107608, 11.461648, 48.108656, 11.477371,
                "grosshadern-fussweg", dataDirectoryPath
            )
            testRoute(
                router, 48.275653, 11.786957, 48.106514, 11.449685,
                "muenchen-quer", dataDirectoryPath
            )
            testRoute(
                router, 48.073606, 11.38175, 48.065548, 11.327763,
                "gauting-unterbrunn", dataDirectoryPath
            )
            testRoute(
                router, 48.073606, 11.38175, 48.152888, 11.346259,
                "gauting-puchheim", dataDirectoryPath
            )
            testRoute(
                router, 48.073606, 11.38175, 48.365138, 11.583881,
                "gauting-moosanger", dataDirectoryPath
            )
            testRoute(
                router, 47.986073, 11.326733, 48.230162, 11.717434,
                "starnberg-ismaning", dataDirectoryPath
            )
            router.close()
            println("finish date " + Date())
            println(
                "required time "
                        + Utils.formatTimeStopWatch(
                    Date().time
                            - startTime.time
                )
            )
        }
    }

    private fun printUsage() {
        println("java -jar Jerusalem.jar route foot|bike|racingBike|mountainBike|car|carShortest <latitude1> <longitude1> <latitude2> <longitude2> [<output-file-base-name>]")
        println("java -jar Jerusalem.jar rebuild <source-filepath>|- <temp-filepath>")
        println("java -jar Jerusalem.jar rebuildIndex <temp-filepath>")
        println("java -jar Jerusalem.jar rebuildTransitions <temp-filepath>")
        println("java -jar Jerusalem.jar clean <temp-filepath>")
        println("java -jar Jerusalem.jar routingTest")
        println("java -jar Jerusalem.jar findCrossroads foot|bike|racingBike|mountainBike|car|carShortest")
        println("java -jar Jerusalem.jar findCrossroadsTest foot|bike|racingBike|mountainBike|car|carShortest")
    }

    private fun testRoute(
        router: Router, lat1: Double, lng1: Double, lat2: Double,
        lng2: Double, name: String, dataDirectoryPath: String
    ) {
        for (rt in RoutingType.values()) testRoute(
            router, rt.name, lat1, lng1, lat2, lng2, name,
            dataDirectoryPath
        )
    }

    fun testRoute(
        router: Router, routingType: String, lat1: Double,
        lng1: Double, lat2: Double, lng2: Double, name: String,
        dataDirectoryPath: String
    ) {
        println("---------------------------------------------")
        println("Computing Route $name ($routingType)")
        println("---------------------------------------------")
        val route = router
            .findRoute(routingType, lat1, lng1, lat2, lng2)
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
        kml.save(
            dataDirectoryPath + File.separatorChar + name + "-"
                    + routingType + ".kml"
        )
    }
}