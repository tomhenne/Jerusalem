package de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps

import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir
import java.io.*

class OsmNodeID2CellIDMapMemory {
    var mapArrays = arrayOfNulls<ShortArray>(INITIAL_NUMBER_OF_ARRAYS)
    var arraySize: Int
    var numberOfUsedArrays = 0

    fun getMaxNumberOfArrays(): Int {
        return mapArrays.size
    }

    init {
        arraySize = (HIGHEST_OSM_NODE_ID / INITIAL_NUMBER_OF_ARRAYS.toLong()).toInt() + 1
    }

    fun put(osmNodeID: Long, lld: LatLonDir): Boolean {
        return put(osmNodeID, lld.shortKey)
    }

    fun increaseArray(requiredSize: Int) {
        while (mapArrays.size < requiredSize) {
            val newMapArrays = arrayOfNulls<ShortArray>(
                mapArrays.size
                        + NUMBER_OF_ARRAYS_INCREMENT
            )
            System.arraycopy(mapArrays, 0, newMapArrays, 0, mapArrays.size)
            mapArrays = newMapArrays
            print("o2o+")
        }
    }

    fun put(osmNodeID: Long, ownNodeID: Short): Boolean {
        val arrayNumber = (osmNodeID / arraySize.toLong()).toInt()
        increaseArray(arrayNumber + 1)
        if (mapArrays[arrayNumber] == null) {
            mapArrays[arrayNumber] = ShortArray(arraySize)
            numberOfUsedArrays++
        }
        val arrayIndex = (osmNodeID - arrayNumber.toLong() * arraySize.toLong()).toInt()
        mapArrays[arrayNumber]!![arrayIndex] = (ownNodeID + 1).toShort()
        return true
    }

    operator fun get(osmNodeID: Long): LatLonDir {
        return LatLonDir(getShort(osmNodeID))
    }

    fun getShort(osmNodeID: Long): Short {
        val arrayNumber = (osmNodeID / arraySize.toLong()).toInt()
        if (mapArrays.size <= arrayNumber) return -1
        if (mapArrays[arrayNumber] == null) return -1
        val arrayIndex = (osmNodeID - arrayNumber.toLong() * arraySize.toLong()).toInt()
        return (mapArrays[arrayNumber]!![arrayIndex] - 1).toShort()
    }

    fun getEstimatedMemorySizeMB(): Float {
        return (arraySize.toFloat() * 2.0f * numberOfUsedArrays.toFloat() / 1024.0f
                / 1024.0f)
    }

    fun save(filePath: String?): Boolean {
        try {
            val dos = DataOutputStream(
                BufferedOutputStream(
                    FileOutputStream(filePath, false),
                    32000
                )
            )
            dos.writeInt(mapArrays.size)
            for (array in mapArrays) {
                if (array == null) {
                    dos.writeInt(0)
                    continue
                }
                dos.writeInt(array.size)
                for (s in array) dos.writeShort(s.toInt())
            }
            dos.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun load(filePath: String?): Boolean {
        try {
            val dis = DataInputStream(
                BufferedInputStream(
                    FileInputStream(filePath),
                    32000
                )
            )
            val nrArrays = dis.readInt()
            mapArrays = arrayOfNulls(nrArrays)
            for (i in 0 until nrArrays) {
                val l = dis.readInt()
                if (l == 0) continue
                mapArrays[i] = ShortArray(l)
                val array = mapArrays[i]
                for (j in 0 until l) {
                    array!![j] = dis.readShort()
                }
            }
            dis.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    companion object {
        const val INITIAL_NUMBER_OF_ARRAYS = 128000
        const val NUMBER_OF_ARRAYS_INCREMENT = 16000
        const val HIGHEST_OSM_NODE_ID = 2200000000L // just an estimate
    }
}