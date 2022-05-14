package de.esymetric.jerusalem.ownDataRepresentation.fileSystem

import de.esymetric.jerusalem.ownDataRepresentation.Node
import de.esymetric.jerusalem.ownDataRepresentation.Transition
import de.esymetric.jerusalem.utils.BufferedRandomAccessFile
import de.esymetric.jerusalem.utils.BufferedRandomAccessFileCache
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException

// sentence structure:
// 0 targetNodeID, INT
// 4 origTargetNodeID, INT
// 8 nextTransitionID, INT
// 12 wayCostID, INT
// 16 wayCostLatLonDirKey SHORT
// 18 distance FLOAT

class PartitionedTransitionListFile(
    var dataDirectoryPath: String,
    var readOnly: Boolean
) {
    var currentLatLonDir = LatLonDir(-1000.0, -1000.0)
    var filePath: String? = null
    var raf: BufferedRandomAccessFile? = null
    var rafCache = BufferedRandomAccessFileCache()
    var numberOfTransitions = 0

    init {
        if (readOnly) rafCache.setMaxCacheSize(30)
    }

    fun setMaxFileCacheSize(s: Int) {
        rafCache.setMaxCacheSize(s)
    }

    fun checkAndCreateRandomAccessFile(lat: Double, lng: Double) {
        val newLatLonDir = LatLonDir(lat, lng)
        if (newLatLonDir.equals(currentLatLonDir)) return
        currentLatLonDir = newLatLonDir
        filePath = (currentLatLonDir.makeDir(dataDirectoryPath, !readOnly)
                + File.separatorChar + FILENAME)
        raf = rafCache.getRandomAccessFile(filePath!!, readOnly)
        try {
            val fileLength = raf!!.length()
            numberOfTransitions = (fileLength / SENTENCE_LENGTH).toInt()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun close() {
        rafCache.close()
    }

    fun insertTransition(
        sourceNode: Node, targetNode: Node,
        distanceM: Double, nextTransitionID: Int, wayCostID: Int,
        wayCostLatLonDirKey: Short
    ): Int {
        checkAndCreateRandomAccessFile(sourceNode.lat, sourceNode.lng)
        return try {
            val baos = ByteArrayOutputStream()
            val daos = DataOutputStream(baos)
            val targetNodeID = targetNode.id.toInt() shl 4
            val offsetBits = currentLatLonDir.getOffsetBits(
                targetNode.lat,
                targetNode.lng
            )
            daos.writeInt(targetNodeID or offsetBits)
            daos.writeInt(-1) // origTargetNodeID, for TransitionOptimizer
            daos.writeInt(nextTransitionID)
            daos.writeInt(wayCostID)
            daos.writeShort(wayCostLatLonDirKey.toInt())
            daos.writeFloat(distanceM.toFloat())
            raf!!.seek(numberOfTransitions.toLong() * SENTENCE_LENGTH)
            raf!!.write(baos.toByteArray())
            daos.close()
            baos.close()
            val id = numberOfTransitions
            numberOfTransitions++
            id
        } catch (e: IOException) {
            e.printStackTrace()
            -1
        }
    }

    fun getTransition(
        sourceNode: Node, transitionID: Int, loadOriginalTargetNode: Boolean,
        nlf: PartitionedNodeListFile, wcf: PartitionedWayCostFile?
    ): Transition? {
        checkAndCreateRandomAccessFile(sourceNode.lat, sourceNode.lng)
        return try {
            if (!raf!!.seek(transitionID.toLong() * SENTENCE_LENGTH)) return null
            val t = Transition()
            t.id = transitionID
            var targetNodeID = raf!!.readInt()
            var offsetBits = targetNodeID and 0xF
            targetNodeID = targetNodeID shr 4
            t.targetNode = nlf.getNode(
                LatLonDir(
                    sourceNode.lat,
                    sourceNode.lng, offsetBits
                ), targetNodeID
            )
            if (t.targetNode == null) {
                // DEBUG
                println(
                    "Transition: cannot get target node with id "
                            + targetNodeID
                )
                t.targetNode = nlf.getNode(
                    LatLonDir(
                        sourceNode.lat,
                        sourceNode.lng, offsetBits
                    ), targetNodeID
                )
                return null
            }
            var origTargetNodeID = raf!!.readInt() // TransitionOptimizer backed up node
            if (loadOriginalTargetNode && origTargetNodeID != -1) {
                offsetBits = origTargetNodeID and 0xF
                origTargetNodeID = origTargetNodeID shr 4
                t.origTargetNode = nlf.getNode(
                    LatLonDir(
                        sourceNode.lat,
                        sourceNode.lng, offsetBits
                    ), origTargetNodeID
                )
            }
            t.nextTransitionID = raf!!.readInt()
            val wayCostID = raf!!.readInt()
            val wayCostLatLonDirKey = raf!!.readShort()
            t.distanceM = raf!!.readFloat().toDouble()
            // TODO get waycost from ALL optimized transitions?
            wcf?.readTransitionCost(
                LatLonDir(wayCostLatLonDirKey),
                wayCostID, t
            )
            t
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    val numberOfFileChanges: Int
        get() = rafCache.andClearNumberOfFileChanges

    fun deleteAllTransitionFiles() {
        for (f in File(dataDirectoryPath).listFiles()) if (f.isDirectory && f.name.startsWith("lat_")) for (g in f.listFiles()) if (g.isDirectory && g.name.startsWith(
                "lng_"
            )
        ) for (h in g.listFiles()) if (h.isFile && h.name == FILENAME) h.delete()
    }

    fun updateTransition(
        sourceNode: Node, t: Transition, wcf: PartitionedWayCostFile
    ): Boolean {
        checkAndCreateRandomAccessFile(sourceNode.lat, sourceNode.lng)
        return try {
            raf!!.seek(t.id.toLong() * SENTENCE_LENGTH)
            val targetNode = t.targetNode
            val targetNodeID = targetNode!!.id.toInt() shl 4
            var offsetBits = currentLatLonDir.getOffsetBits(
                targetNode.lat,
                targetNode.lng
            )
            raf!!.writeInt(targetNodeID or offsetBits)
            val origTargetNode = t.origTargetNode
            val origTargetNodeID = origTargetNode!!.id.toInt() shl 4
            offsetBits = currentLatLonDir.getOffsetBits(
                origTargetNode.lat,
                origTargetNode.lng
            )
            raf!!.writeInt(origTargetNodeID or offsetBits)
            raf!!.seek(t.id.toLong() * SENTENCE_LENGTH + 18L)
            raf!!.writeFloat(t.distanceM.toFloat())

            //TODO also update transition costs??

            raf!!.seek(t.id.toLong() * SENTENCE_LENGTH + 12L)
            val wayCostID = raf!!.readInt()
            val wayCostLatLonDirKey = raf!!.readShort()

            wcf.updateWay(LatLonDir(wayCostLatLonDirKey), wayCostID,
                t.costFoot, t.costBike, t.costRacingBike,
                t.costMountainBike, t.costCar, t.costCarShortest
            )

            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        const val SENTENCE_LENGTH = 22L
        const val FILENAME = "transitions.data"
    }
}