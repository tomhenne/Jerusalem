package de.esymetric.jerusalem.ownDataRepresentation

import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedNodeListFile
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedTransitionListFile
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedWayCostFile
import de.esymetric.jerusalem.ownDataRepresentation.geoData.GPSMath.calculateDistance
import de.esymetric.jerusalem.routing.RoutingHeuristics
import de.esymetric.jerusalem.routing.RoutingType

class Node : Comparable<Node>, Cloneable {
    var id // used for ownID, but in Rebuilder also for osmID
            : Long = 0
	var lat = 0.0
	var lng = 0.0

	var transitionID = 0

    // routing
    var totalCost // real cost + estimated cost
            = 0.0
    var realCostSoFar // real cost from the start node to this node
            = 0.0
    var predecessor: Node? = null
    private var transitions: MutableList<Transition>? = null
    private var latLonDirKey: Short = 0

    override fun equals(obj: Any?): Boolean {
        return if (obj is Node) {
            val n = obj
            n.id == id &&  // ID is not unique any more ...
                    n.uID == uID
        } else false
    }

    fun setLatLonDirKey(llk: Short) {
        latLonDirKey = llk
    }

    private var uidInternal: Long = 0
    val uID: Long
        get() {
            if (uidInternal != 0L) return uidInternal
            uidInternal = id shl 32
            val fileID = (lat + LAT_OFFS).toInt() shl 16 or (lng + LNG_OFFS).toInt()
            uidInternal = uidInternal or fileID.toLong()
            return uidInternal
        }

    public override fun clone(): Any {
        val n = Node()
        n.id = id
        n.uidInternal = uidInternal
        n.lat = lat
        n.lng = lng
        n.transitionID = transitionID
        n.latLonDirKey = latLonDirKey
        return n
    }

    // isOriginalDirection: on a one-way street cars and bikes cannot go in the
    // opposite direction
    fun addTransition(
        targetNode: Node, nlf: PartitionedNodeListFile,
        wlf: PartitionedTransitionListFile, wayCostID: Int,
        wayCostLatLonDirKey: Short, heuristics: RoutingHeuristics?,
        isOriginalDirection: Boolean
    ) {
        val distanceM = calculateDistance(
            lat, lng, targetNode.lat,
            targetNode.lng
        )
        transitionID = wlf.insertTransition(
            this, targetNode, distanceM,
            transitionID, wayCostID, wayCostLatLonDirKey
        )
        nlf.setTransitionID(lat, lng, id.toInt(), transitionID) // first
        // transition
        // for this node
    }

    val numberOfTransitionsIfTransitionsAreLoaded: Int
        get() = if (transitions == null) -1 else transitions!!.size

    fun listTransitions(
        nlf: PartitionedNodeListFile,
        wlf: PartitionedTransitionListFile,
        wcf: PartitionedWayCostFile? = null
    ): List<Transition> {
        if (transitions != null) return transitions!! // return cached version
        transitions = ArrayList()
        if (transitionID == -1) return transitions!!
        var tID = transitionID
        while (true) {
            val t = wlf.getTransition(this, tID, nlf, wcf) ?: break
            // bug in data
            transitions!!.add(t)
            if (t.nextTransitionID == -1) break
            if (t.nextTransitionID == tID) break // bug in data
            tID = t.nextTransitionID
        }
        return transitions!!
    }

    fun listTransitionsWithoutSameWayBack(
        predecessor: Node?,
        nlf: PartitionedNodeListFile,
        wlf: PartitionedTransitionListFile,
        wcf: PartitionedWayCostFile? = null
    ): MutableList<Transition> {
        if (transitions == null) listTransitions(nlf, wlf, wcf)
        val l: MutableList<Transition> = ArrayList()
        for (t in transitions!!)
            if (t.targetNode != predecessor) l.add(t)
        return l
    }

    override fun toString(): String {
        return "$id $lat $lng f=$totalCost"
    }

    override fun compareTo(o: Node): Int {
        if (equals(o)) return 0
        if (totalCost < o.totalCost) return -1 else if (totalCost > o.totalCost) return 1
        return if (uID < o.uID) -1 else 1
    }

    fun getRemainingCost(
        target: Node, type: RoutingType?,
        heuristics: RoutingHeuristics
    ): Double {
        val d = calculateDistance(lat, lng, target.lat, target.lng)
        return d * heuristics.estimateRemainingCost(type) / 1000.0
    }

    fun remainingCost(): Double {
        return totalCost - realCostSoFar
    }

    fun loadByID(lld: LatLonDir, nlf: PartitionedNodeListFile) {
        nlf.getNode(this, lld, id.toInt())
    }

    fun isLoaded() = lat != 0.0 || lng != 0.0

    fun findConnectedMasterNodes(nlf: PartitionedNodeListFile, wlf: PartitionedTransitionListFile): List<Node> {
        val nodes: MutableList<Node> = ArrayList()
        val ts = listTransitions(nlf, wlf)
        if (ts.size > 2) {  // is master node
            nodes.add(this)
            return nodes
        }

        for (t in ts) nodes.add(searchMasterNode(t.targetNode!!, this, nlf, wlf))

        return nodes
    }

    fun searchMasterNode(
        n: Node,
        comingFromNode: Node?,
        nlf: PartitionedNodeListFile,
        wlf: PartitionedTransitionListFile
    ): Node {
        val ts = n.listTransitionsWithoutSameWayBack(comingFromNode, nlf, wlf)
        return if (ts.size == 1) searchMasterNode(ts[0].targetNode!!, n, nlf, wlf) else n
    }

    companion object {
        const val LAT_OFFS = 90.0
        const val LNG_OFFS = 180.0
    }
}