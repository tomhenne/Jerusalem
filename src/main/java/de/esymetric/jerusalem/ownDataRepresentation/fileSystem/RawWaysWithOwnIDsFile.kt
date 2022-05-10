package de.esymetric.jerusalem.ownDataRepresentation.fileSystem

import de.esymetric.jerusalem.osmDataRepresentation.OSMWay
import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.MemoryArrayOsmNodeID2OwnIDMap
import de.esymetric.jerusalem.ownDataRepresentation.Node
import de.esymetric.jerusalem.rebuilding.Rebuilder
import de.esymetric.jerusalem.routing.RoutingHeuristics
import de.esymetric.jerusalem.utils.MemoryEfficientLongToIntMap
import de.esymetric.jerusalem.utils.Utils
import java.io.*
import java.util.*

class RawWaysWithOwnIDsFile(var dataDirectoryPath: String, readOnly: Boolean) {
    var filePath: String? = null
    var readOnly = false
    var currentLatLonDir = LatLonDir(-1000.0, -1000.0)
    fun writeWay(
        way: OSMWay,
        osmID2ownIDMap: MemoryArrayOsmNodeID2OwnIDMap,
        findNodesNodesCache: MemoryEfficientLongToIntMap
    ): Boolean {
        val lld = LatLonDir(way.getLatLonDirID(osmID2ownIDMap))
        openOutputStream(lld)
        try {
            daos!!.writeInt(way.wayCostIDForward)
            daos!!.writeInt(way.wayCostIDBackward)
            for (osmNodeID in way.nodes!!) {
                val nodeID = findNodesNodesCache[osmNodeID!!]
                if (nodeID == -1) {
                    println("RawWaysFile: Error - node ID is -1")
                    continue
                }
                daos!!.writeInt(nodeID)
                daos!!.writeShort(osmID2ownIDMap.getShortCellID(osmNodeID).toInt())
            }
            daos!!.writeInt(-1)
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun readWay(way: OSMWay, lld: LatLonDir, nlf: PartitionedNodeListFile?): MutableList<Node>? {
        val nodes: MutableList<Node> = LinkedList()
        openInputStream(lld)
        if (dain == null) return null
        try {
            if (dain!!.available() <= 0) return null
            way.wayCostIDForward = dain!!.readInt()
            way.wayCostIDBackward = dain!!.readInt()
            while (true) {
                val node = Node()
                node.id = dain!!.readInt().toLong()
                if (node.id == -1L) break
                val latLonDirKey = dain!!.readShort()
                node.loadByID(LatLonDir(latLonDirKey), nlf)
                nodes.add(node)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        return nodes
    }

    var daos: DataOutputStream? = null
    fun openOutputStream(newLatLonDir: LatLonDir) {
        if (daos != null && newLatLonDir.equals(currentLatLonDir)) return
        if (daos != null) try {
            daos!!.close()
        } catch (e1: IOException) {
            e1.printStackTrace()
        }
        currentLatLonDir = newLatLonDir
        filePath = (currentLatLonDir.makeDir(dataDirectoryPath, !readOnly)
                + File.separatorChar + FILENAME)
        try {
            daos = DataOutputStream(
                BufferedOutputStream(
                    FileOutputStream(filePath, true)
                )
            )
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    var dain: DataInputStream? = null

    init {
        this.readOnly = readOnly
    }

    fun openInputStream(newLatLonDir: LatLonDir): Boolean {
        if (dain != null && newLatLonDir.equals(currentLatLonDir)) return true
        if (dain != null) try {
            dain!!.close()
        } catch (e1: IOException) {
            e1.printStackTrace()
        }
        currentLatLonDir = newLatLonDir
        filePath = (currentLatLonDir.makeDir(dataDirectoryPath, !readOnly)
                + File.separatorChar + FILENAME)
        if (!File(filePath).exists()) return false
        dain = try {
            DataInputStream(
                BufferedInputStream(
                    FileInputStream(filePath)
                )
            )
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun close() {
        if (dain != null) try {
            dain!!.close()
        } catch (e1: IOException) {
            e1.printStackTrace()
        }
        if (daos != null) try {
            daos!!.close()
        } catch (e1: IOException) {
            e1.printStackTrace()
        }
        dain = null
        daos = null
        currentLatLonDir = LatLonDir(-1000.0, -1000.0)
    }

    fun buildTransitions(
        startTime: Date,
        nlf: PartitionedNodeListFile,
        wlf: PartitionedTransitionListFile,
        routingHeuristics: RoutingHeuristics,
        osmID2ownIDMap: MemoryArrayOsmNodeID2OwnIDMap?
    ) {
        nlf.setMaxFileCacheSize(8)
        // ein Weg kann Nodes aus mehreren benachbarten Sektoren enthalten
        // daher m�ssen diese auch gebuffert werden
        val files = File(dataDirectoryPath).listFiles()
        Arrays.sort(files)
        for (f in files) if (f.isDirectory && f.name.startsWith("lat_")) {
            for (g in f.listFiles()) if (g != null && g.isDirectory
                && g.name.startsWith("lng_")
            ) {
                val list = g.listFiles()
                if (list == null) {
                    println(
                        "Cannot list files in "
                                + g.path
                    )
                    continue
                }
                for (h in list) if (h != null && h.isFile
                    && h.name == FILENAME
                ) {
                    val dirLatInt: Int = f.name
                        .replace("lat_", "").toInt() - LatLonDir.Companion.LAT_OFFS.toInt()
                    val dirLngInt: Int = g.name
                        .replace("lng_", "").toInt() - LatLonDir.Companion.LNG_OFFS.toInt()
                    println(
                        "\n"
                                + Utils.formatTimeStopWatch(
                            Date()
                                .time
                                    - startTime.time
                        )
                                + " building transitions for lat="
                                + dirLatInt + " lng=" + dirLngInt
                    )
                    val lld = LatLonDir(
                        dirLatInt.toDouble(),
                        dirLngInt.toDouble()
                    )
                    while (true) {
                        val way = OSMWay()
                        val nodes = readWay(way, lld, nlf) ?: break
                        if (nodes.size != 0) {
                            insertTransitions(way, nodes, nlf, wlf, routingHeuristics, lld)
                        }
                    }
                    close()
                }
            }
            Rebuilder.cleanMem(startTime)
        }
        println(
            Utils.formatTimeStopWatch(
                Date().time
                        - startTime.time
            )
                    + " finished building transitions"
        )
    }

    fun deleteFiles(startTime: Date) {
        println(
            Utils.formatTimeStopWatch(
                Date().time
                        - startTime.time
            )
                    + " deleting rawWays.data files"
        )
        val files = File(dataDirectoryPath).listFiles()
        Arrays.sort(files)
        for (f in files) if (f.isDirectory && f.name.startsWith("lat_")) {
            for (g in f.listFiles()) if (g != null && g.isDirectory
                && g.name.startsWith("lng_")
            ) {
                val list = g.listFiles()
                if (list == null) {
                    println(
                        "Cannot list files in "
                                + g.path
                    )
                    continue
                }
                for (h in list) if (h != null && h.isFile
                    && h.name == FILENAME
                ) {
                    h.delete()
                }
            }
        }
    }

    private fun insertTransitions(
        way: OSMWay,
        wayNodes: MutableList<Node>,
        nlf: PartitionedNodeListFile,
        wlf: PartitionedTransitionListFile,
        routingHeuristics: RoutingHeuristics,
        lld: LatLonDir
    ) {
        for (i in 0 until wayNodes.size - 1) {
            val nodeA = wayNodes[i]
            val nodeB = wayNodes[i + 1]
            val latLonDirKey = lld.shortKey
            if (way.wayCostIDForward != -1) {
                nodeA.addTransition(
                    nodeB, nlf, wlf, way.wayCostIDForward,
                    latLonDirKey,
                    routingHeuristics, false
                )
            }
            if (way.wayCostIDBackward != -1) {
                nodeB.addTransition(
                    nodeA, nlf, wlf, way.wayCostIDBackward,
                    latLonDirKey,
                    routingHeuristics, true
                )
            }
        }
        wayNodes.clear()
    }

    companion object {
        // Aufbau 1 Datensatz: int wayCostIDForward, int wayCostIDBackward, int
        // ownNodeID1, short latLonDirKey1,  ..., int onwNodeIDN, short latLonDirKeyN, -1
        const val FILENAME = "rawWays.data"
    }
}