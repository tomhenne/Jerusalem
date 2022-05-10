package rebuilding.optimizer

import de.esymetric.jerusalem.rebuilding.optimizer.TransitionsOptimizer
import de.esymetric.jerusalem.routing.Router
import de.esymetric.jerusalem.routing.Router.Companion.debugMode
import de.esymetric.jerusalem.routing.algorithms.TomsAStarStarRouting
import de.esymetric.jerusalem.routing.heuristics.TomsRoutingHeuristics
import org.junit.Test
import routing.RouterTest
import java.io.File
import java.util.*

class TransitionsOptimizerTest {
    var routerTest = RouterTest()
    @Test
    @Throws(Exception::class)
    fun testTransitionsOptimizer() {
        val dataDirectoryPath = "testData"
        File(dataDirectoryPath).mkdirs()
        val to = TransitionsOptimizer(dataDirectoryPath)
        to.optimize(Date())
        to.close()


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
            router, 48.116892, 11.487076, 48.117909, 11.472429,
            "eichenstrasse-opt", dataDirectoryPath
        )
    }
}