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
// 4 nextTransitionID, INT
// 8 wayCostID, INT
// 12 wayCostLatLonDirKey SHORT
// 14 distance FLOAT

class PartitionedTransitionListFile(
    var dataDirectoryPath: String,
    var readOnly: Boolean
) {
    var currentLatLonDir = LatLonDir(-1000.0, -1000.0)
    var filePath: String? = null
    var raf: BufferedRandomAccessFile? = null
    private var rafCache = BufferedRandomAccessFileCache()
    private var numberOfTransitions = 0

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
            ) ?: return -1
            daos.writeInt(targetNodeID or offsetBits)
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
        sourceNode: Node, transitionID: Int,
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
            t.nextTransitionID = raf!!.readInt()
            val wayCostID = raf!!.readInt()
            val wayCostLatLonDirKey = raf!!.readShort()
            t.distanceM = raf!!.readFloat().toDouble()
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
        for (f in File(dataDirectoryPath).listFiles())
            if (f.isDirectory && f.name.startsWith("lat_"))
                for (g in f.listFiles())
                    if (g.isDirectory && g.name.startsWith("lng_"))
                        for (h in g.listFiles()) if (h.isFile && h.name == FILENAME)
                h.delete()
    }

    companion object {
        const val SENTENCE_LENGTH = 18L
        const val FILENAME = "transitions.data"
    }
}