package de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps

import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir
import de.esymetric.jerusalem.utils.FileBasedHashMapForLongKeys
import de.esymetric.jerusalem.utils.Utils
import java.io.*
import java.util.*

class PartitionedOsmNodeID2OwnIDMap(
    var dataDirectoryPath: String,
    var readOnly: Boolean
) {
    private var cellMap = OsmNodeID2CellIDMapMemory()
    private var currentHashMap = FileBasedHashMapForLongKeys()
    private var currentHashMapLatLonDir: LatLonDir? = null

    fun getAvgGetAccessNumberOfReads(): Float {
        return currentHashMap.avgGetAccessNumberOfReads
    }

    fun getAndClearNumberOfFileChanges(): Int {
        return currentHashMap.andClearNumberOfFileChanges
    }

    fun deleteLatLonTempFiles() {
        for (f in File(dataDirectoryPath).listFiles()) if (f.isDirectory && f.name.startsWith("lat_")) for (g in f.listFiles()) if (g.isDirectory && g.name.startsWith(
                "lng_"
            )
        ) for (h in g.listFiles()) if (h.isFile && h.name == "osmMap.data") h.delete()
    }

    fun put(lat: Double, lng: Double, osmID: Long, ownID: Int) {
        val lld = LatLonDir(lat, lng)
        if (currentHashMapLatLonDir == null
            || !lld.equals(currentHashMapLatLonDir)
        ) {
            currentHashMapLatLonDir = lld
            currentHashMap.open(
                lld.makeDir(dataDirectoryPath, true)
                        + File.separatorChar + "osmMap.data", readOnly
            )
        }
        cellMap.put(osmID, lld)
        currentHashMap.put(osmID, ownID)
    }

    fun loadExistingOsm2OwnIDIntoMemory(startTime: Date) {
        var count = 1
        val files = File(dataDirectoryPath).listFiles()
        Arrays.sort(files)
        for (f in files) if (f != null && f.isDirectory && f.name.startsWith("lat_")) {
            println(
                Utils.formatTimeStopWatch(
                    Date()
                        .time
                            - startTime.time
                )
                        + "  >>> "
                        + f.name
                        + " # "
                        + count++
                        + ", cellmap uses "
                        + cellMap.numberOfUsedArrays
                        + "/" + cellMap.getMaxNumberOfArrays() + " arrays"
            )
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
                    && h.name == "osmMap.data"
                ) {
                    val lat = f.name
                        .substring(4).toInt() - LatLonDir.LAT_OFFS
                    val lng = g.name
                        .substring(4).toInt() - LatLonDir.LNG_OFFS
                    val lld = LatLonDir(lat, lng)
                    val cellID = lld.shortKey
                    try {
                        val l = (h.length() / 12L).toInt()
                        val fis = FileInputStream(h)
                        val dis = DataInputStream(
                            BufferedInputStream(fis, 100000)
                        )
                        for (i in 0 until l) {
                            val osmNodeID = dis.readInt()
                            dis.readLong()
                            if (osmNodeID > 0) cellMap.put(osmNodeID.toLong(), cellID)
                        }
                        dis.close()
                        fis.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun getLatLonDir(osmNodeID: Int): LatLonDir {
        return LatLonDir(cellMap.getShort(osmNodeID.toLong()))
    }

    fun getShortCellID(osmNodeID: Long): Short {
        return cellMap.getShort(osmNodeID)
    }

    operator fun get(osmNodeID: Long): Int {
        val lld = LatLonDir(cellMap.getShort(osmNodeID))
        if (currentHashMapLatLonDir == null
            || !lld.equals(currentHashMapLatLonDir)
        ) {
            val nextFilePath = (lld.makeDir(dataDirectoryPath, false)
                    + File.separatorChar + "osmMap.data")
            if (File(nextFilePath).exists()) {
                currentHashMapLatLonDir = lld
                currentHashMap.open(nextFilePath, readOnly)
            } else return -1
        }
        return currentHashMap[osmNodeID]
    }

    fun close() {
        currentHashMap.close()
    }

    fun setReadOnly() {
        currentHashMap.close()
        currentHashMapLatLonDir = null
        readOnly = true
    }

    fun getNumberOfUsedArrays(): Int {
        return cellMap.numberOfUsedArrays
    }

    fun getMaxNumberOfArrays(): Int {
        return cellMap.getMaxNumberOfArrays()
    }

    fun getEstimatedMemorySizeMB(): Float {
        return cellMap.getEstimatedMemorySizeMB()
    }
}