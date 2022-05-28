package rebuilding

import de.esymetric.jerusalem.osmDataRepresentation.OSMDataReader
import de.esymetric.jerusalem.rebuilding.Rebuilder
import de.esymetric.jerusalem.routing.Router
import de.esymetric.jerusalem.routing.Router.Companion.debugMode
import de.esymetric.jerusalem.routing.algorithms.TomsAStarStarRouting
import de.esymetric.jerusalem.routing.heuristics.TomsRoutingHeuristics
import de.esymetric.jerusalem.utils.BufferedRandomAccessFile.Companion.shortInfoAndResetCounters
import org.apache.tools.bzip2.CBZip2InputStream
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import routing.RouterTest
import java.io.File
import java.io.FileInputStream
import java.util.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RebuilderTest {
    private var routerTest = RouterTest()

    private val osmTestFilePath = "osmData/munich-hadern-excerpt.osm.bz2"

    @Test
    @Order(1)
    @Throws(Exception::class)
    fun testRebuildAndRouting() {
        val dataDirectoryPath = "testData"
        File(dataDirectoryPath).mkdirs()
        val tempDirectoryPath = "tempData"
        File(tempDirectoryPath).mkdirs()
        val rebuilder = Rebuilder(
            dataDirectoryPath, tempDirectoryPath,
            TomsRoutingHeuristics(), false, false, false
        )
        val fis = FileInputStream(osmTestFilePath)
        fis.read()
        fis.read()
        val bzis = CBZip2InputStream(fis)
        val reader = OSMDataReader(bzis, rebuilder, false)
        reader.read(Date())
        bzis.close()
        fis.close()
        rebuilder.finishProcessingAndClose()

        val rebuilder2 = Rebuilder(
            dataDirectoryPath, tempDirectoryPath,
            TomsRoutingHeuristics(), true, false, false
        )
        rebuilder2.buildTransitions()
        println(
            "BRAF-INFO: " +
                    shortInfoAndResetCounters
        )

        var router = Router(
            dataDirectoryPath,
            TomsAStarStarRouting(), TomsRoutingHeuristics(), 120
        )

        routerTest.makeRoute(
            router, 48.116892, 11.487076, 48.117909, 11.472429,
            "eichenstrasse", dataDirectoryPath
        )

        // test

        router = Router(
            dataDirectoryPath,
            TomsAStarStarRouting(), TomsRoutingHeuristics(), 120
        )
        debugMode = true
        routerTest.makeRoute(
            router, 48.116892, 11.487076, 48.117909, 11.472429,
            "eichenstrasse", dataDirectoryPath
        )

    }

    @Test
    @Order(2)
    @Throws(Exception::class)
    fun testRebuildOnlyWays() {
        val dataDirectoryPath = "testData"
        val tempDirectoryPath = "tempData"

        // rebuild only ways
        val rebuilder = Rebuilder(
            dataDirectoryPath, tempDirectoryPath,
            TomsRoutingHeuristics(), true, true, false
        )
        val fis = FileInputStream(osmTestFilePath)
        fis.read()
        fis.read()
        val bzis = CBZip2InputStream(fis)
        val reader = OSMDataReader(bzis, rebuilder, true)
        reader.read(Date())
        bzis.close()
        fis.close()
        rebuilder.finishProcessingAndClose()


        // test
        val router = Router(
            dataDirectoryPath,
            TomsAStarStarRouting(), TomsRoutingHeuristics(), 120
        )
        debugMode = true
        routerTest.makeRoute(
            router, 48.107891, 11.461865, 48.099986, 11.511051,
            "durch-waldfriedhof", dataDirectoryPath
        )
        routerTest.makeRoute(
            router, 48.107608, 11.461648, 48.108656, 11.477371,
            "grosshadern-fussweg", dataDirectoryPath
        )
        routerTest.makeRoute(
            router, 48.107608, 11.461648, 48.108656, 11.477371,
            "grosshadern-fussweg", dataDirectoryPath
        )
        routerTest.makeRoute(
            router, 48.116892, 11.487076, 48.117909, 11.472429,
            "eichenstrasse", dataDirectoryPath
        )
    }

    @Test
    @Order(3)
    @Throws(Exception::class)
    fun testRebuildTransitions() {
        val dataDirectoryPath = "testData"
        File(dataDirectoryPath).mkdirs()
        val tempDirectoryPath = "tempData"
        File(tempDirectoryPath).mkdirs()

        // rebuild transitions

        val rebuilder = Rebuilder(
            dataDirectoryPath, tempDirectoryPath,
            TomsRoutingHeuristics(), true, false, false
        )
        rebuilder.buildTransitions()
        println(
            "BRAF-INFO: " +
                    shortInfoAndResetCounters
        )

        var router = Router(
            dataDirectoryPath,
            TomsAStarStarRouting(), TomsRoutingHeuristics(), 120
        )

        routerTest.makeRoute(
            router, 48.116892, 11.487076, 48.117909, 11.472429,
            "eichenstrasse", dataDirectoryPath
        )

        // test

        router = Router(
            dataDirectoryPath,
            TomsAStarStarRouting(), TomsRoutingHeuristics(), 120
        )
        debugMode = true
        routerTest.makeRoute(
            router, 48.116892, 11.487076, 48.117909, 11.472429,
            "eichenstrasse", dataDirectoryPath
        )

    }
}