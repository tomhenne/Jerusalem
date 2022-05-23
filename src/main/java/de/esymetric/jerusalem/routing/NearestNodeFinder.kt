package de.esymetric.jerusalem.routing

import de.esymetric.jerusalem.ownDataRepresentation.Node
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.*
import de.esymetric.jerusalem.ownDataRepresentation.geoData.GPSMath.calculateDistance

class NearestNodeFinder {
    fun findNearestNode(
        lat: Double,
        lng: Double,
        nif: NodeIndexFile,
        nlf: PartitionedNodeListFile,
        wlf: PartitionedTransitionListFile,
        wcf: PartitionedWayCostFile?,
        type: RoutingType
    ): Node? {
        for (radius in 0 until MAX_SEARCH_RADIUS) {
            val n = findNearestNode(lat, lng, nif, nlf, wlf, wcf, type, radius)
            if (n != null) return n
        }
        return null
    }

    private fun findNearestNode(
        lat: Double,
        lng: Double,
        nif: NodeIndexFile,
        nlf: PartitionedNodeListFile,
        wlf: PartitionedTransitionListFile,
        wcf: PartitionedWayCostFile?,
        type: RoutingType,
        radius: Int
    ): Node? {
        val list = nif.getIDPlusSourroundingIDs(lat, lng, radius)
        var nearestNode: Node? = null
        var nearestNodeDistance = Double.MAX_VALUE
        for (nind in list) {
            run {
                val n = nlf.getNode(
                    LatLonDir(nind.latInt.toDouble(), nind.lngInt.toDouble()),
                    nind.id
                ) // this is new and hopefully finds all nodes
                if (n != null && n.transitionID != -1) { // must have transitions :-)

                    // must have transitions for this routingType!!
                    val transitions = n.listTransitions(nlf, wlf, wcf)
                    var hasSuitableTransitions = false
                    for (t in transitions) {
                        if (t.getCost(type) != RoutingHeuristics.BLOCKED_WAY_COST) {
                            hasSuitableTransitions = true
                            break
                        }
                    }
                    if (hasSuitableTransitions) {
                        val d = calculateDistance(
                            lat, lng, n.lat,
                            n.lng
                        )
                        if (d < nearestNodeDistance) {
                            nearestNodeDistance = d
                            nearestNode = n
                        }
                    }
                }
            }
        }
        return nearestNode
    }

    companion object {
        const val MAX_SEARCH_RADIUS = 5 // was 3 !!!
    }
}