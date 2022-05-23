package de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps

import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.longOsm2OwnMaps.LongOsmNodeID2OwnIDMapFileCache
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir
import java.io.File
import java.util.*

class MemoryArrayOsmNodeID2OwnIDMap(
    dataDirectoryPath: String,
    maxOsm2OwnMapCacheSize: Int, readOnly: Boolean
) {
    var cellMap: OsmNodeID2CellIDMapMemory
    var osm2ownMap: LongOsmNodeID2OwnIDMapFileCache
    var dataDirectoryPath: String
    var readOnly: Boolean

    init {
        cellMap = OsmNodeID2CellIDMapMemory()
        this.dataDirectoryPath = dataDirectoryPath
        this.readOnly = readOnly
        osm2ownMap = LongOsmNodeID2OwnIDMapFileCache(dataDirectoryPath, maxOsm2OwnMapCacheSize)
    }

    fun put(lat: Double, lng: Double, osmID: Long, ownID: Int) {
        val lld = LatLonDir(lat, lng)
        cellMap.put(osmID, lld)
        osm2ownMap.put(osmID, ownID)
    }

    fun loadExistingOsm2OwnIDIntoMemory(startTime: Date?) {
        val filePath = dataDirectoryPath + File.separatorChar + CELL_MAP_FILENAME
        cellMap.load(filePath)
    }

    fun persistCellMap(startTime: Date?) {
        val filePath = dataDirectoryPath + File.separatorChar + CELL_MAP_FILENAME
        cellMap.save(filePath)
    }

    fun getLatLonDir(osmNodeID: Int): LatLonDir {
        return LatLonDir(cellMap.getShort(osmNodeID.toLong()))
    }

    fun getShortCellID(osmNodeID: Long): Short {
        return cellMap.getShort(osmNodeID)
    }

    operator fun get(osmNodeID: Long): Int {
        return osm2ownMap[osmNodeID]
    }

    fun close() {
        osm2ownMap.close()
    }

    fun setReadOnly() {
        osm2ownMap.close()
        readOnly = true
    }

    fun getNumberOfUsedArrays(): Int {
        return cellMap.numberOfUsedArrays
    }

    fun getMaxNumberOfArrays(): Int {
        return cellMap.getMaxNumberOfArrays()
    }

    fun getEstimatedMemorySizeMB(): Float {
        return cellMap.getEstimatedMemorySizeMB() + osm2ownMap.estimatedMemoryUsedMB
    }

    companion object {
        const val CELL_MAP_FILENAME = "cellMap.data"
    }
}