package de.esymetric.jerusalem.tools

import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.*
import de.esymetric.jerusalem.ownDataRepresentation.geoData.GPSMath
import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position
import de.esymetric.jerusalem.routing.NearestNodeFinder
import de.esymetric.jerusalem.routing.RoutingType

class CrossroadsFinder(dataDirectoryPath: String) {
    private val MAX_DISTANCE_TO_NODE = 10.0
    private var nrf: NearestNodeFinder = NearestNodeFinder()
    private var nif: NodeIndexFile
    private var nlf: PartitionedNodeListFile
    private var wlf: PartitionedTransitionListFile
    private var wcf: PartitionedWayCostFile

    init {
        nif = PartitionedQuadtreeNodeIndexFile(
            dataDirectoryPath, true
        )
        nlf = PartitionedNodeListFile(dataDirectoryPath, true)
        wlf = PartitionedTransitionListFile(dataDirectoryPath, true)
        wcf = PartitionedWayCostFile(dataDirectoryPath, true)
    }

    fun close() {
        nlf.close()
        wlf.close()
        wcf.close()
    }

    fun loadNumberOfCrossroads(positions: List<Position>, routingType: RoutingType) {
        for (p in positions) {
            val n = nrf.findNearestNode(p.latitude, p.longitude, nif, nlf, wlf, wcf, routingType) ?: continue
            val d = GPSMath.calculateDistance(
                p.latitude, p.longitude, n.lat,
                n.lng
            )
            if (d <= MAX_DISTANCE_TO_NODE) p.nrOfTransitions = n.listTransitions( nlf, wlf).size.toByte()
        }
    }
}