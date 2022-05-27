package tools

import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position
import de.esymetric.jerusalem.routing.RoutingType
import de.esymetric.jerusalem.tools.CrossroadsFinder
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CrossroadsFinderTest {
    @Test
    @Order(4)
    fun testCrossroadsFinder() {
        val dataDirectoryPath = "testData"
        val crossroadsFinder = CrossroadsFinder(dataDirectoryPath)
        val positions = listOf(
            Position().also {
                it.latitude = 48.115547
                it.longitude = 11.477455
            },
            Position().also {
                it.latitude = 48.116782
                it.longitude = 11.484021
            }
        )
        crossroadsFinder.loadNumberOfCrossroads(positions, RoutingType.bike)

        assertEquals(4, positions[0].nrOfTransitions)
        assertEquals(4, positions[1].nrOfTransitions)
    }
}