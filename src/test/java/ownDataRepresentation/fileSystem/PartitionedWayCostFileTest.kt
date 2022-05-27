package ownDataRepresentation.fileSystem

import de.esymetric.jerusalem.ownDataRepresentation.Transition
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedWayCostFile
import de.esymetric.jerusalem.routing.RoutingHeuristics.Companion.BLOCKED_WAY_COST
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PartitionedWayCostFileTest {

    @Test
    fun testUShortWayCost() {
        val partitionedWayCostFile = PartitionedWayCostFile("testData", false)
        val latLonDir = LatLonDir(48.11, 11.48)
        val id = partitionedWayCostFile.insertWay(latLonDir, 0.0, 1.0, 100.0, 500.0,
            999.0, BLOCKED_WAY_COST)
        val id2 = partitionedWayCostFile.insertWay(latLonDir, 2000.0, 333.0, 100.0, 500.0,
            999.0, BLOCKED_WAY_COST)
        partitionedWayCostFile.close()

        val partitionedWayCostFile2 = PartitionedWayCostFile("testData", true)
        val t = Transition()
        partitionedWayCostFile2.readTransitionCost(latLonDir, id, t)

        assertEquals(0.0, t.costFoot)
        assertEquals(1.0, t.costBike)
        assertEquals(100.0, t.costRacingBike)
        assertEquals(500.0, t.costMountainBike)
        assertEquals(999.0, t.costCar)
        assertEquals(BLOCKED_WAY_COST, t.costCarShortest)

        partitionedWayCostFile2.readTransitionCost(latLonDir, id2, t)

        assertEquals(1000.0, t.costFoot)  // max
        assertEquals(333.0, t.costBike)
        assertEquals(100.0, t.costRacingBike)
        assertEquals(500.0, t.costMountainBike)
        assertEquals(999.0, t.costCar)
        assertEquals(BLOCKED_WAY_COST, t.costCarShortest)

        partitionedWayCostFile2.close()

    }

}