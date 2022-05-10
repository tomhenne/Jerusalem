package de.esymetric.jerusalem.rebuilding.optimizer

import de.esymetric.jerusalem.ownDataRepresentation.Node
import de.esymetric.jerusalem.ownDataRepresentation.Transition
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedNodeListFile
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedTransitionListFile

class TransitionsExpander {
    fun expandNodes(nodes: List<Node>, nlf: PartitionedNodeListFile, wlf: PartitionedTransitionListFile): List<Node?> {
        val nnodes: MutableList<Node?> = ArrayList()
        var na: Node? = null
        for (nb in nodes) {
            na?.let { expandNode(it, nb, nnodes, nlf, wlf) }
            na = nb
            nnodes.add(na)
        }
        return nnodes
    }

    private fun expandNode(
        na: Node,
        nb: Node,
        nnodes: MutableList<Node?>,
        nlf: PartitionedNodeListFile,
        wlf: PartitionedTransitionListFile
    ) {
        na.clearTransitionsCache() // clear transitions that do not have origTargetNodes loaded
        val ts = na.listTransitions(true, nlf, wlf)
        for (t in ts) {
            if (t.targetNode == nb) {
                followTransitions(na, t, nnodes, nb, nlf, wlf)
                return
            }
        }
        println("trans not found")
    }

    private fun followTransitions(
        sourceNode: Node?,
        t: Transition,
        nnodes: MutableList<Node?>,
        finalNode: Node,
        nlf: PartitionedNodeListFile,
        wlf: PartitionedTransitionListFile
    ) {
        var targetNode = t.origTargetNode
        if (targetNode == null) targetNode = t.targetNode
        if (targetNode == finalNode) return
        nnodes.add(targetNode)
        val ts = targetNode!!.listTransitionsWithoutSameWayBack(sourceNode, true, nlf, wlf)
        if (ts.size != 1) return
        followTransitions(targetNode, ts[0], nnodes, finalNode, nlf, wlf)
    }
}