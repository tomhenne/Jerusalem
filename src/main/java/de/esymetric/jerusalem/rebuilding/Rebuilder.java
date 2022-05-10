package de.esymetric.jerusalem.rebuilding;

import de.esymetric.jerusalem.osmDataRepresentation.OSMDataReader.OSMDataReaderListener;
import de.esymetric.jerusalem.osmDataRepresentation.OSMNode;
import de.esymetric.jerusalem.osmDataRepresentation.OSMWay;
import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.MemoryArrayOsmNodeID2OwnIDMap;
import de.esymetric.jerusalem.ownDataRepresentation.Transition;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.*;
import de.esymetric.jerusalem.routing.RoutingHeuristics;
import de.esymetric.jerusalem.routing.RoutingType;
import de.esymetric.jerusalem.utils.BufferedRandomAccessFile;
import de.esymetric.jerusalem.utils.MemoryEfficientLongToIntMap;
import de.esymetric.jerusalem.utils.Utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;

public class Rebuilder implements OSMDataReaderListener {

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

    final static int MAX_NODE_CACHE_SIZE = 8000000;
    final static int MAX_NEW_WAY_QUEUE_SIZE = 1500000;
    final static int MAX_OSM2OWN_MAP_CACHE_SIZE = 1;

    private NodeIndexFile nif;
    private PartitionedNodeListFile nlf;
    private PartitionedTransitionListFile wlf;
    private PartitionedWayCostFile wcf;
    private MemoryArrayOsmNodeID2OwnIDMap osmNodeID2OwnIDMap;
    private RawWaysWithOwnIDsFile rawWaysFile;
    private RoutingHeuristics routingHeuristics;
    private Date startTime;

    private long countNodes, countWays;
    private long highestNodeID = 0, lowestNodeID = Long.MAX_VALUE;

    private boolean jumpOverNodes, readOnly;

    public Rebuilder(String dataDirectoryPath, String tempDirectoryPath,
                     RoutingHeuristics routingHeuristics, boolean doNotDeleteFiles, boolean jumpOverNodes, boolean readOnly) {
        this.routingHeuristics = routingHeuristics;
        this.jumpOverNodes = jumpOverNodes;
        this.readOnly = readOnly;

        startTime = new Date();

        System.out.println(Utils.formatTimeStopWatch(new Date().getTime()
                - startTime.getTime())
                + " Starting REBUILD ");

        System.out.println(Utils.formatTimeStopWatch(new Date().getTime()
                - startTime.getTime())
                + " Deleting files");

        nif = new PartitionedQuadtreeNodeIndexFile(dataDirectoryPath, readOnly,
                !readOnly);

        nlf = new PartitionedNodeListFile(dataDirectoryPath, readOnly);

        osmNodeID2OwnIDMap = new MemoryArrayOsmNodeID2OwnIDMap(
                tempDirectoryPath, MAX_OSM2OWN_MAP_CACHE_SIZE, readOnly);

        cleanMem();

        if (!doNotDeleteFiles) {
            LatLonDir.deleteAllLatLonDataFiles(dataDirectoryPath);
            LatLonDir.deleteAllLatLonDataFiles(tempDirectoryPath);
        }

        wlf = new PartitionedTransitionListFile(dataDirectoryPath, readOnly);

        wcf = new PartitionedWayCostFile(dataDirectoryPath, readOnly);

        rawWaysFile = new RawWaysWithOwnIDsFile(tempDirectoryPath, readOnly);

        // rebuildOnlyWays preparations


        if (jumpOverNodes) {
            System.out.println(Utils.formatTimeStopWatch(new Date().getTime()
                    - startTime.getTime())
                    + " Rebuilding only ways: deleting transition files\n");
            wlf.deleteAllTransitionFiles();

            System.out.println(Utils.formatTimeStopWatch(new Date().getTime()
                    - startTime.getTime())
                    + " Rebuilding only ways: deleting wayCost files\n");
            wcf.deleteAllWayCostFiles();

            System.out.println(Utils.formatTimeStopWatch(new Date().getTime()
                    - startTime.getTime())
                    + " Rebuilding only ways: deleting rawWays.data files\n");
            rawWaysFile.deleteFiles(startTime);

            System.out
                    .println(Utils.formatTimeStopWatch(new Date().getTime()
                            - startTime.getTime())
                            + " Rebuilding only ways: resetting transition IDs in nodes files\n");
            nlf.resetTransitionIDsInAllFilesBuffered(startTime);

            System.out.println(Utils.formatTimeStopWatch(new Date().getTime()
                    - startTime.getTime())
                    + " Rebuilding only ways: loading osm2own ID memory map\n");
            osmNodeID2OwnIDMap.loadExistingOsm2OwnIDIntoMemory(startTime);

            System.out.println(Utils.formatTimeStopWatch(new Date().getTime()
                    - startTime.getTime())
                    + " Rebuilding only ways: skipping nodes ...\n");
            System.out.print(Utils.formatTimeStopWatch(new Date().getTime()
                    - startTime.getTime())
                    + " free memory: " + Utils.memInfoStr());
            System.gc();
            System.out.println(" >>> " + Utils.memInfoStr());

        }


        System.out.println(Utils.formatTimeStopWatch(new Date().getTime()
                - startTime.getTime())
                + " Info on read nodes and ways:\n");
        System.out.println("nd#: number of nodes read");
        System.out.println("ndh: highest osm node ID read (ever)");
        System.out.println("ndl: lowest osm node ID read (in this cycle)");
        System.out
                .println("ona: number of memory arrays used by osm to own map");
        System.out
                .println("ons: estimated byte size (mb) of osm to own map");
        System.out
                .println("braf: number of BufferedRandomAccessFile file reads/writes, number of open files, total size of open files");
        System.out.println("mem: free and total memory\n");

        System.out.println("wa#: number of ways read");
        System.out.println("wfc: number of file changes for transition list");
        System.out.println("tcs: transitions cache size");
        System.out
                .println("arl: average number of file reads required to get an nodeID from a OSM ID (but only for the current FileBasedHashMap)");
    }

    void cleanMem() {
        cleanMem(startTime);
    }

    public static void cleanMem(Date startTime) {
        System.out.print(Utils.formatTimeStopWatch(new Date().getTime()
                - startTime.getTime())
                + "free memory: " + Utils.memInfoStr());
        System.gc();
        System.out.println(" >>> " + Utils.memInfoStr());

    }

    public void finishProcessingAndClose() {

        // process remaining ways still in the queue

        processWaysCache();

        // free mem

        waysCache = null;
        waysCacheSize = 0;

        // info

        cleanMem();
        System.out.println("\n"
                + Utils.formatTimeStopWatch(new Date().getTime()
                - startTime.getTime()) + " SUMMARY:");

        System.out
                .println(Utils.formatTimeStopWatch(new Date().getTime()
                        - startTime.getTime())
                        + " "
                        + countNodes
                        + " nodes read, "
                        + countWays
                        + " ways read");

        // close

        osmNodeID2OwnIDMap.close();
        osmNodeID2OwnIDMap = null; // free memory
        cleanMem();

        rawWaysFile.close();  // close for writing and flush
        wcf.close();

        // create transitions

        if (!readOnly) buildTransitions();

        nlf.close();
        wlf.close();

        rawWaysFile.close();

        // finally make quadtree index

        if( !readOnly ) makeQuadtreeIndex();

        // close

        nif.close();

    }

    private long lastTime = System.currentTimeMillis();

    private long timespan() {
        long time = System.currentTimeMillis();
        long delta = time - lastTime;
        lastTime = time;
        return delta;
    }

    private long timeInsertNewNode,
            timePutOsm2OwnIDMap;

    private OSMNode[] nodesCache = new OSMNode[MAX_NODE_CACHE_SIZE];
    private int nodesCacheSize = 0;

    @Override
    public void foundNode(OSMNode node) {
        if (jumpOverNodes) return;

        nodesCache[nodesCacheSize++] = node;
        if (nodesCacheSize >= MAX_NODE_CACHE_SIZE)
            processNodesCache();
    }

    private static class OSMNodeByDirComparator implements Comparator<OSMNode> {

        @Override
        public int compare(OSMNode o1, OSMNode o2) {
            int k1 = o1.getLatLonDirKey();
            int k2 = o2.getLatLonDirKey();
            if (k1 == k2)
                return 0;
            else
                return k1 > k2 ? 1 : -1;
        }
    }

    private static class OSMNodeByOSMIDComparator implements Comparator<OSMNode> {

        @Override
        public int compare(OSMNode o1, OSMNode o2) {
            long k1 = o1.id;
            long k2 = o2.id;
            if (k1 == k2)
                return 0;
            else
                return k1 > k2 ? 1 : -1;
        }
    }

    void processNodesCache() {
        Arrays.sort(nodesCache, 0, nodesCacheSize, new OSMNodeByDirComparator());
        timespan();
        for (int i = 0; i < nodesCacheSize; i++) {
            OSMNode n = nodesCache[i];
            insertNewNode(n);
        }
        timeInsertNewNode += timespan();


        Arrays.sort(nodesCache, 0, nodesCacheSize, new OSMNodeByOSMIDComparator()); //mod!!!!
        for (int i = 0; i < nodesCacheSize; i++) {
            OSMNode node = nodesCache[i];
            osmNodeID2OwnIDMap.put(node.lat, node.lng, node.id, node.ownID);
        }
        timePutOsm2OwnIDMap += timespan();

        countNodes += nodesCacheSize;
        for (int i = 0; i < nodesCacheSize; i++)
            nodesCache[i] = null;
        nodesCacheSize = 0;

        System.gc();
        System.out.println("\n" + Utils.formatTimeStopWatch(new Date().getTime()
                - startTime.getTime())
                + " nd#"
                + countNodes
                + " ndh"
                + highestNodeID
                + " ndl"
                + lowestNodeID
                + " ona"
                + osmNodeID2OwnIDMap.getNumberOfUsedArrays()
                + "/"
                + osmNodeID2OwnIDMap.getMaxNumberOfArrays()
                + " ons"
                + (int) osmNodeID2OwnIDMap.getEstimatedMemorySizeMB() + "mb"
                + " mem"
                + Utils.memInfoStr()
                + " t_ins"
                + timeInsertNewNode
                + " t_put" + timePutOsm2OwnIDMap);
        timeInsertNewNode = 0L;
        timePutOsm2OwnIDMap = 0L;
        lowestNodeID = Long.MAX_VALUE;

    }

    private void insertNewNode(OSMNode node) {
        if (node.id > highestNodeID)
            highestNodeID = node.id;
        if (node.id < lowestNodeID)
            lowestNodeID = node.id;

        node.ownID = nlf.insertNewNodeStreamAppend(node.lat, node.lng);
    }

    private OSMWay[] waysCache = new OSMWay[MAX_NEW_WAY_QUEUE_SIZE];
    private int waysCacheSize = 0;

    @Override
    public void foundWay(OSMWay way) {
        if (countWays == 0 && nodesCache != null) {
            processNodesCache();  // process remaining
            nodesCache = null; // free mem
            nodesCacheSize = 0;

            osmNodeID2OwnIDMap.setReadOnly();
            nodesCache = null;
            nlf.close(); // switch from stream access to RandomAccessFile access, no need to reopen
            osmNodeID2OwnIDMap.persistCellMap(startTime);
        }

        waysCache[waysCacheSize++] = way;

        if (waysCacheSize >= MAX_NEW_WAY_QUEUE_SIZE)
            processWaysCache();

    }

    private void processWaysCache() {
        Arrays.sort(waysCache, 0, waysCacheSize, new OSMWayByDirComparator());
        timespan();
        for (int i = 0; i < waysCacheSize; i++) {
            OSMWay w = waysCache[i];
            calculateCost(w);
        }
        timeCalcAndInsertCost += timespan();

        for (int i = 0; i < waysCacheSize; i++) {
            OSMWay w = waysCache[i];
            prepareNodes(w);
        }
        timePrepareNodes += timespan();
        translateNodesOSMID2OWNID();

        for (int i = 0; i < waysCacheSize; i++) {
            OSMWay w = waysCache[i];
            rawWaysFile.writeWay(w, osmNodeID2OwnIDMap, findNodesNodesCache);
        }

        findNodesNodesCache.clear();
        for (int i = 0; i < waysCacheSize; i++) {
            waysCache[i] = null;
        }
        waysCacheSize = 0;

        cleanMem();
        System.out.println("\n" + Utils.formatTimeStopWatch(new Date().getTime()
                - startTime.getTime())
                + " wd# "
                + countWays
                + " nfc "
                + nlf.getFileChangesWithNewFileCreation()
                + "/"
                + nlf.getFileChanges()
                + " wfc "
                + wlf.getNumberOfFileChanges()
                + " arl "
                + osmNodeID2OwnIDMap.getAvgGetAccessNumberOfReads()
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
                + timeInsertTransitions + " mem " + Utils.memInfoStr());

        timePrepareNodes = 0L;
        timeOsm2Own = 0L;
        timeGetNodes = 0L;
        timeCalcAndInsertCost = 0L;
        timeInsertTransitions = 0L;
        timePrepareTransitions = 0L;
    }

    private class OSMWayByDirComparator implements Comparator<OSMWay> {

        @Override
        public int compare(OSMWay o1, OSMWay o2) {
            short k1 = o1.getLatLonDirID(osmNodeID2OwnIDMap);
            short k2 = o2.getLatLonDirID(osmNodeID2OwnIDMap);
            if (k1 == k2)
                return 0;
            else
                return k1 > k2 ? 1 : -1;
        }

    }

    private long timeOsm2Own, timeGetNodes, timePrepareNodes, timeCalcAndInsertCost, timePrepareTransitions, timeInsertTransitions;

    MemoryEfficientLongToIntMap findNodesNodesCache = new MemoryEfficientLongToIntMap();

    void prepareNodes(OSMWay way) {
        // neue Nodes erzeugen und das LatLonDir setzen f�r die Sortierung

        for (long osmID : way.getNodes()) {
            findNodesNodesCache.put(osmID, -1);
        }
    }

    void translateNodesOSMID2OWNID() {
        // die OSM ID in die OWN ID �bersetzen
        long[] foundNodes = findNodesNodesCache.keys();
        Arrays.sort(foundNodes);
        timespan();
        for (long osmID : foundNodes) {
            int nodeID = osmNodeID2OwnIDMap.get(osmID); // translate osm ID to
            // own ID
            if (nodeID == -1)
                System.out.println("Rebuilder Error: cannot get own ID for OSM ID " + osmID);
            findNodesNodesCache.put(osmID, nodeID);
        }
        timeOsm2Own += timespan();
    }

    private void calculateCost(OSMWay way) {
        // berechnet die Kostenfaktoren, nicht die absoluten Kosten
        countWays++;

        Map<String, String> wayTags = way.getTags();
        LatLonDir lld = new LatLonDir(way.getLatLonDirID(osmNodeID2OwnIDMap));

        Transition forward = new Transition();
        forward.costFoot = routingHeuristics.calculateCost(RoutingType.foot,
                wayTags, true);
        forward.costBike = routingHeuristics.calculateCost(RoutingType.bike,
                wayTags, true);
        forward.costRacingBike = routingHeuristics.calculateCost(
                RoutingType.racingBike, wayTags, true);
        forward.costMountainBike = routingHeuristics.calculateCost(
                RoutingType.mountainBike, wayTags, true);
        forward.costCar = routingHeuristics.calculateCost(RoutingType.car,
                wayTags, true);
        forward.costCarShortest = routingHeuristics.calculateCost(
                RoutingType.carShortest, wayTags, true);

        int wayCostIDForward = -1;
        if (!forward.isAllBlocked())
            wayCostIDForward = wcf.insertWay(lld, forward.costFoot,
                    forward.costBike, forward.costRacingBike,
                    forward.costMountainBike, forward.costCar,
                    forward.costCarShortest);

        Transition backward = new Transition();
        backward.costFoot = routingHeuristics.calculateCost(RoutingType.foot,
                wayTags, false);
        backward.costBike = routingHeuristics.calculateCost(RoutingType.bike,
                wayTags, false);
        backward.costRacingBike = routingHeuristics.calculateCost(
                RoutingType.racingBike, wayTags, false);
        backward.costMountainBike = routingHeuristics.calculateCost(
                RoutingType.mountainBike, wayTags, false);
        backward.costCar = routingHeuristics.calculateCost(RoutingType.car,
                wayTags, false);
        backward.costCarShortest = routingHeuristics.calculateCost(
                RoutingType.carShortest, wayTags, false);

        int wayCostIDBackward = -1;
        if (!backward.isAllBlocked())
            wayCostIDBackward = wcf.insertWay(lld, backward.costFoot,
                    backward.costBike, backward.costRacingBike,
                    backward.costMountainBike, backward.costCar,
                    backward.costCarShortest);

        way.setWayCostIDForward(wayCostIDForward);
        way.setWayCostIDBackward(wayCostIDBackward);

        way.getTags().clear();
        way.setTags(null); // not needed after that point
    }

    public void makeQuadtreeIndex() {
        if (nif instanceof PartitionedQuadtreeNodeIndexFile) {
            PartitionedQuadtreeNodeIndexFile pqnif = (PartitionedQuadtreeNodeIndexFile) nif;
            pqnif.makeQuadtreeIndex(startTime, nlf);
        }
    }

    public void buildTransitions() {
        rawWaysFile.buildTransitions(startTime, nlf, wlf, routingHeuristics, osmNodeID2OwnIDMap);
    }


}
