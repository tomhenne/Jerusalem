package de.esymetric.jerusalem.rebuilding

import de.esymetric.jerusalem.osmDataRepresentation.OSMDataReader.OSMDataReaderListener
import de.esymetric.jerusalem.osmDataRepresentation.OSMNode
import de.esymetric.jerusalem.osmDataRepresentation.OSMWay
import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.MemoryArrayOsmNodeID2OwnIDMap
import de.esymetric.jerusalem.ownDataRepresentation.Transition
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.*
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir.Companion.deleteAllLatLonDataFiles
import de.esymetric.jerusalem.routing.RoutingHeuristics
import de.esymetric.jerusalem.routing.RoutingType
import de.esymetric.jerusalem.utils.BufferedRandomAccessFile
import de.esymetric.jerusalem.utils.MemoryEfficientLongToIntMap
import de.esymetric.jerusalem.utils.Utils
import java.util.*

class Rebuilder(
    dataDirectoryPath: String,
    tempDirectoryPath: String?,
    private val routingHeuristics: RoutingHeuristics,
    doNotDeleteFiles: Boolean,
    private val jumpOverNodes: Boolean,
    private val readOnly: Boolean
) : OSMDataReaderListener {
    private val nif: NodeIndexFile
    private val nlf: PartitionedNodeListFile
    private val wlf: PartitionedTransitionListFile
    private val wcf: PartitionedWayCostFile
    private var osmNodeID2OwnIDMap: MemoryArrayOsmNodeID2OwnIDMap?
    private val rawWaysFile: RawWaysWithOwnIDsFile
    private val startTime: Date
    private var countNodes: Long = 0
    private var countWays: Long = 0
    private var highestNodeID: Long = 0
    private var lowestNodeID = Long.MAX_VALUE
    fun cleanMem() {
        cleanMem(startTime)
    }

    fun finishProcessingAndClose() {

        // process remaining ways still in the queue
        processWaysCache()

        // free mem
        waysCacheSize = 0

        // info
        cleanMem()
        println(
            "\n"
                    + Utils.formatTimeStopWatch(
                Date().time
                        - startTime.time
            ) + " SUMMARY:"
        )
        println(
            Utils.formatTimeStopWatch(
                Date().time
                        - startTime.time
            )
                    + " "
                    + countNodes
                    + " nodes read, "
                    + countWays
                    + " ways read"
        )

        // close
        osmNodeID2OwnIDMap!!.close()
        osmNodeID2OwnIDMap = null // free memory
        cleanMem()
        rawWaysFile.close() // close for writing and flush
        wcf.close()

        // create transitions
        if (!readOnly) buildTransitions()
        nlf.close()
        wlf.close()
        rawWaysFile.close()

        // finally make quadtree index
        if (!readOnly) makeQuadtreeIndex()

        // close
        nif.close()
    }

    private var lastTime = System.currentTimeMillis()
    private fun timespan(): Long {
        val time = System.currentTimeMillis()
        val delta = time - lastTime
        lastTime = time
        return delta
    }

    private var timeInsertNewNode: Long = 0
    private var timePutOsm2OwnIDMap: Long = 0
    private var nodesCache: Array<OSMNode?>? = arrayOfNulls(MAX_NODE_CACHE_SIZE)
    private var nodesCacheSize = 0
    override fun foundNode(node: OSMNode?) {
        if (jumpOverNodes) return
        nodesCache!![nodesCacheSize++] = node
        if (nodesCacheSize >= MAX_NODE_CACHE_SIZE) processNodesCache()
    }

    private class OSMNodeByDirComparator : Comparator<OSMNode?> {
        override fun compare(o1: OSMNode?, o2: OSMNode?): Int {
            val k1 = o1?.getLatLonDirKey() ?: Int.MIN_VALUE
            val k2 = o2?.getLatLonDirKey() ?: Int.MIN_VALUE
            return if (k1 == k2) 0 else if (k1 > k2) 1 else -1
        }
    }

    private class OSMNodeByOSMIDComparator : Comparator<OSMNode?> {
        override fun compare(o1: OSMNode?, o2: OSMNode?): Int {
            val k1 = o1?.id ?: Long.MIN_VALUE
            val k2 = o2?.id ?: Long.MIN_VALUE
            return if (k1 == k2) 0 else if (k1 > k2) 1 else -1
        }
    }

    fun processNodesCache() {
        Arrays.sort(nodesCache, 0, nodesCacheSize, OSMNodeByDirComparator())
        timespan()
        for (i in 0 until nodesCacheSize) {
            val n = nodesCache!![i]
            insertNewNode(n)
        }
        timeInsertNewNode += timespan()
        Arrays.sort(nodesCache, 0, nodesCacheSize, OSMNodeByOSMIDComparator()) //mod!!!!
        for (i in 0 until nodesCacheSize) {
            val node = nodesCache!![i]
            osmNodeID2OwnIDMap!!.put(node!!.lat, node.lng, node.id, node.ownID)
        }
        timePutOsm2OwnIDMap += timespan()
        countNodes += nodesCacheSize.toLong()
        for (i in 0 until nodesCacheSize) nodesCache!![i] = null
        nodesCacheSize = 0
        System.gc()
        println(
            "\n" + Utils.formatTimeStopWatch(
                Date().time
                        - startTime.time
            )
                    + " nd#"
                    + countNodes
                    + " ndh"
                    + highestNodeID
                    + " ndl"
                    + lowestNodeID
                    + " ona"
                    + osmNodeID2OwnIDMap!!.getNumberOfUsedArrays()
                    + "/"
                    + osmNodeID2OwnIDMap!!.getMaxNumberOfArrays()
                    + " ons"
                    + osmNodeID2OwnIDMap!!.getEstimatedMemorySizeMB().toInt() + "mb"
                    + " mem"
                    + Utils.memInfoStr()
                    + " t_ins"
                    + timeInsertNewNode
                    + " t_put" + timePutOsm2OwnIDMap
        )
        timeInsertNewNode = 0L
        timePutOsm2OwnIDMap = 0L
        lowestNodeID = Long.MAX_VALUE
    }

    private fun insertNewNode(node: OSMNode?) {
        if (node!!.id > highestNodeID) highestNodeID = node.id
        if (node.id < lowestNodeID) lowestNodeID = node.id
        node.ownID = nlf.insertNewNodeStreamAppend(node.lat, node.lng)
    }

    private var waysCache: Array<OSMWay?> = arrayOfNulls(MAX_NEW_WAY_QUEUE_SIZE)
    private var waysCacheSize = 0
    override fun foundWay(way: OSMWay?) {
        if (countWays == 0L && nodesCache != null) {
            processNodesCache() // process remaining
            nodesCache = null // free mem
            nodesCacheSize = 0
            osmNodeID2OwnIDMap!!.setReadOnly()
            nodesCache = null
            nlf.close() // switch from stream access to RandomAccessFile access, no need to reopen
            osmNodeID2OwnIDMap!!.persistCellMap(startTime)
        }
        waysCache!![waysCacheSize++] = way
        if (waysCacheSize >= MAX_NEW_WAY_QUEUE_SIZE) processWaysCache()
    }

    private fun processWaysCache() {
        Arrays.sort(waysCache, 0, waysCacheSize, OSMWayByDirComparator())
        timespan()
        for (i in 0 until waysCacheSize) {
            val w = waysCache!![i]
            calculateCost(w)
        }
        timeCalcAndInsertCost += timespan()
        for (i in 0 until waysCacheSize) {
            val w = waysCache!![i]
            prepareNodes(w)
        }
        timePrepareNodes += timespan()
        translateNodesOSMID2OWNID()
        for (i in 0 until waysCacheSize) {
            val w = waysCache!![i]
            rawWaysFile.writeWay(w!!, osmNodeID2OwnIDMap!!, findNodesNodesCache)
        }
        findNodesNodesCache.clear()
        for (i in 0 until waysCacheSize) {
            waysCache!![i] = null
        }
        waysCacheSize = 0
        waysCache = arrayOfNulls(MAX_NEW_WAY_QUEUE_SIZE)
        cleanMem()
        println(
            "\n" + Utils.formatTimeStopWatch(
                Date().time
                        - startTime.time
            )
                    + " wd# "
                    + countWays
                    + " nfc "
                    + nlf.fileChangesWithNewFileCreation
                    + "/"
                    + nlf.getFileChanges()
                    + " wfc "
                    + wlf.numberOfFileChanges
                    + " arl "
                    + osmNodeID2OwnIDMap!!.getAvgGetAccessNumberOfReads()
                    + " braf:"
                    + BufferedRandomAccessFile.getShortInfoAndResetCounters()
                    + " t_prn "
                    + timePrepareNodes
                    + " t_o2o "
                    + timeOsm2Own
                    + " t_gno "
                    + timeGetNodes
                    + " t_cos "
                    + timeCalcAndInsertCost
                    + " t_prt "
                    + timePrepareTransitions
                    + " t_tra "
                    + timeInsertTransitions + " mem " + Utils.memInfoStr()
        )
        timePrepareNodes = 0L
        timeOsm2Own = 0L
        timeGetNodes = 0L
        timeCalcAndInsertCost = 0L
        timeInsertTransitions = 0L
        timePrepareTransitions = 0L
    }

    private inner class OSMWayByDirComparator : Comparator<OSMWay?> {
        override fun compare(o1: OSMWay?, o2: OSMWay?): Int {
            val k1 = o1?.getLatLonDirID(osmNodeID2OwnIDMap!!) ?: Short.MIN_VALUE
            val k2 = o2?.getLatLonDirID(osmNodeID2OwnIDMap!!) ?: Short.MIN_VALUE
            return if (k1 == k2) 0 else if (k1 > k2) 1 else -1
        }
    }

    private var timeOsm2Own: Long = 0
    private var timeGetNodes: Long = 0
    private var timePrepareNodes: Long = 0
    private var timeCalcAndInsertCost: Long = 0
    private var timePrepareTransitions: Long = 0
    private var timeInsertTransitions: Long = 0
    var findNodesNodesCache = MemoryEfficientLongToIntMap()

    init {
        startTime = Date()
        println(
            Utils.formatTimeStopWatch(
                Date().time
                        - startTime.time
            )
                    + " Starting REBUILD "
        )
        println(
            Utils.formatTimeStopWatch(
                Date().time
                        - startTime.time
            )
                    + " Deleting files"
        )
        nif = PartitionedQuadtreeNodeIndexFile(
            dataDirectoryPath!!, readOnly,
            !readOnly
        )
        nlf = PartitionedNodeListFile(dataDirectoryPath, readOnly)
        osmNodeID2OwnIDMap = MemoryArrayOsmNodeID2OwnIDMap(
            tempDirectoryPath!!, MAX_OSM2OWN_MAP_CACHE_SIZE, readOnly
        )
        cleanMem()
        if (!doNotDeleteFiles) {
            deleteAllLatLonDataFiles(dataDirectoryPath)
            deleteAllLatLonDataFiles(tempDirectoryPath)
        }
        wlf = PartitionedTransitionListFile(dataDirectoryPath, readOnly)
        wcf = PartitionedWayCostFile(dataDirectoryPath, readOnly)
        rawWaysFile = RawWaysWithOwnIDsFile(tempDirectoryPath, readOnly)

        // rebuildOnlyWays preparations
        if (jumpOverNodes) {
            println(
                Utils.formatTimeStopWatch(
                    Date().time
                            - startTime.time
                )
                        + " Rebuilding only ways: deleting transition files\n"
            )
            wlf.deleteAllTransitionFiles()
            println(
                Utils.formatTimeStopWatch(
                    Date().time
                            - startTime.time
                )
                        + " Rebuilding only ways: deleting wayCost files\n"
            )
            wcf.deleteAllWayCostFiles()
            println(
                Utils.formatTimeStopWatch(
                    Date().time
                            - startTime.time
                )
                        + " Rebuilding only ways: deleting rawWays.data files\n"
            )
            rawWaysFile.deleteFiles(startTime)
            println(
                Utils.formatTimeStopWatch(
                    Date().time
                            - startTime.time
                )
                        + " Rebuilding only ways: resetting transition IDs in nodes files\n"
            )
            nlf.resetTransitionIDsInAllFilesBuffered(startTime)
            println(
                Utils.formatTimeStopWatch(
                    Date().time
                            - startTime.time
                )
                        + " Rebuilding only ways: loading osm2own ID memory map\n"
            )
            osmNodeID2OwnIDMap!!.loadExistingOsm2OwnIDIntoMemory(startTime)
            println(
                Utils.formatTimeStopWatch(
                    Date().time
                            - startTime.time
                )
                        + " Rebuilding only ways: skipping nodes ...\n"
            )
            print(
                Utils.formatTimeStopWatch(
                    Date().time
                            - startTime.time
                )
                        + " free memory: " + Utils.memInfoStr()
            )
            System.gc()
            println(" >>> " + Utils.memInfoStr())
        }
        println(
            Utils.formatTimeStopWatch(
                Date().time
                        - startTime.time
            )
                    + " Info on read nodes and ways:\n"
        )
        println("nd#: number of nodes read")
        println("ndh: highest osm node ID read (ever)")
        println("ndl: lowest osm node ID read (in this cycle)")
        println("ona: number of memory arrays used by osm to own map")
        println("ons: estimated byte size (mb) of osm to own map")
        println("braf: number of BufferedRandomAccessFile file reads/writes, number of open files, total size of open files")
        println("mem: free and total memory\n")
        println("wa#: number of ways read")
        println("wfc: number of file changes for transition list")
        println("tcs: transitions cache size")
        println("arl: average number of file reads required to get an nodeID from a OSM ID (but only for the current FileBasedHashMap)")
    }

    fun prepareNodes(way: OSMWay?) {
        // neue Nodes erzeugen und das LatLonDir setzen f�r die Sortierung
        for (osmID in way!!.nodes!!) {
            findNodesNodesCache.put(osmID, -1)
        }
    }

    fun translateNodesOSMID2OWNID() {
        // die OSM ID in die OWN ID �bersetzen
        val foundNodes = findNodesNodesCache.keys()
        Arrays.sort(foundNodes)
        timespan()
        for (osmID in foundNodes) {
            val nodeID = osmNodeID2OwnIDMap!![osmID] // translate osm ID to
            // own ID
            if (nodeID == -1) println("Rebuilder Error: cannot get own ID for OSM ID $osmID")
            findNodesNodesCache.put(osmID, nodeID)
        }
        timeOsm2Own += timespan()
    }

    private fun calculateCost(way: OSMWay?) {
        // berechnet die Kostenfaktoren, nicht die absoluten Kosten
        countWays++
        val wayTags: Map<String, String>? = way!!.tags
        val lld = LatLonDir(way.getLatLonDirID(osmNodeID2OwnIDMap!!))
        val forward = Transition()
        forward.costFoot = routingHeuristics.calculateCost(
            RoutingType.foot,
            wayTags, true
        )
        forward.costBike = routingHeuristics.calculateCost(
            RoutingType.bike,
            wayTags, true
        )
        forward.costRacingBike = routingHeuristics.calculateCost(
            RoutingType.racingBike, wayTags, true
        )
        forward.costMountainBike = routingHeuristics.calculateCost(
            RoutingType.mountainBike, wayTags, true
        )
        forward.costCar = routingHeuristics.calculateCost(
            RoutingType.car,
            wayTags, true
        )
        forward.costCarShortest = routingHeuristics.calculateCost(
            RoutingType.carShortest, wayTags, true
        )
        var wayCostIDForward = -1
        if (!forward.isAllBlocked) wayCostIDForward = wcf.insertWay(
            lld, forward.costFoot,
            forward.costBike, forward.costRacingBike,
            forward.costMountainBike, forward.costCar,
            forward.costCarShortest
        )
        val backward = Transition()
        backward.costFoot = routingHeuristics.calculateCost(
            RoutingType.foot,
            wayTags, false
        )
        backward.costBike = routingHeuristics.calculateCost(
            RoutingType.bike,
            wayTags, false
        )
        backward.costRacingBike = routingHeuristics.calculateCost(
            RoutingType.racingBike, wayTags, false
        )
        backward.costMountainBike = routingHeuristics.calculateCost(
            RoutingType.mountainBike, wayTags, false
        )
        backward.costCar = routingHeuristics.calculateCost(
            RoutingType.car,
            wayTags, false
        )
        backward.costCarShortest = routingHeuristics.calculateCost(
            RoutingType.carShortest, wayTags, false
        )
        var wayCostIDBackward = -1
        if (!backward.isAllBlocked) wayCostIDBackward = wcf.insertWay(
            lld, backward.costFoot,
            backward.costBike, backward.costRacingBike,
            backward.costMountainBike, backward.costCar,
            backward.costCarShortest
        )
        way.wayCostIDForward = wayCostIDForward
        way.wayCostIDBackward = wayCostIDBackward
        way.tags!!.clear()
        way.tags = null // not needed after that point
    }

    fun makeQuadtreeIndex() {
        if (nif is PartitionedQuadtreeNodeIndexFile) {
            nif.makeQuadtreeIndex(startTime, nlf)
        }
    }

    fun buildTransitions() {
        rawWaysFile.buildTransitions(startTime, nlf, wlf, routingHeuristics, osmNodeID2OwnIDMap)
    }

    companion object {
        /*
     * Version mit 4-stufigem Prozess 27.03.2013
     *
     * Zahlen vom Mac:
     *
     * REBUILD Oberbayern: 19:08
     * REBUILD Index: 0:29
     * RoutingTest: 36 sec
     *
     * Planet: ca. 21:30 h (20:00 nodes + ways + transitions + 1:22 index) 66 GB Speicherbedarf
     * RoutingTest: 28 sec
     *
     * RoutingTest mit Optimierung und Erkennung der 2 Target-Masternodes: 16 sec
     */
        /*
     * Version mit 4-stufigem Prozess 26.03.2013
     *
     * Zahlen vom Mac:
     *
     * REBUILD Oberbayern: 19:29
     * RoutingTest: 29 sec
     *
     * REBUILD Bayern1: 18:11
     * RoutingTest: 6 sec unvollst�ndig aber wohl korrekt ...
     *
     */
        /*
     * Version mit BufferedRandomAccessFile (5 St�ck jeweils)
     *
     * Zahlen vom Mac:
     *
     * REBUILD Bavaria: 24:43 REBUILDWAYS Bavaria is 11:50
     *
     * Zahlen vom Server:
     *
     * wd# 100000 200000 900000 4:12 4:36 9:07
     *
     * ETA: 14 Tage
     */
        /*
     * Version ohne BufferedRandomAccessFile, mit eigener Queue f�r Transitions
     * und mit Cache f�r findNodes()
     *
     * Zahlen vom Mac:
     *
     * REBUILD Bavaria: 42:53 REBUILDWAYS Bavaria is 30:55
     *
     * Zahlen vom Server:
     *
     * wd# 100000 200000 900000 14000000
     *
     * 4:06 4:29 9:07 19:37
     *
     * ETA: 14 Tage
     */
        /*
     * Version mit Partitionierung von "findNodes" in mehrere
     * Verarbeitungsschritte (inkl. Sortierung beim Zugriff)
     *
     * Zahlen vom Mac:
     *
     * REBUILD Bavaria: 24:43 REBUILDWAYS Bavaria is 10:38
     *
     * Zahlen vom Server:
     *
     * wd# 100000 200000 900000 14000000
     *
     *      4:14   4:28    6:46    15:23
     *
     * REBUILD Planet: ca. 7 Tage REBUILDWAYS Planet: 4 Tage 1:24
     */
        /* Oberbayern 05 / 22
     *
     * 24:01 Minuten
     *
     * 18512751 nodes read, 2785394 ways read
     *
     */
        const val MAX_NODE_CACHE_SIZE = 8000000
        const val MAX_NEW_WAY_QUEUE_SIZE = 1500000
        const val MAX_OSM2OWN_MAP_CACHE_SIZE = 1
        fun cleanMem(startTime: Date) {
            print(
                Utils.formatTimeStopWatch(
                    Date().time
                            - startTime.time
                )
                        + "free memory: " + Utils.memInfoStr()
            )
            System.gc()
            println(" >>> " + Utils.memInfoStr())
        }
    }
}