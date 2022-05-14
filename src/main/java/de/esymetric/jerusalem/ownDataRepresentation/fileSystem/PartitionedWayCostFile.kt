package de.esymetric.jerusalem.ownDataRepresentation.fileSystem

import de.esymetric.jerusalem.ownDataRepresentation.Transition
import de.esymetric.jerusalem.utils.BufferedRandomAccessFile
import de.esymetric.jerusalem.utils.BufferedRandomAccessFileCache
import java.io.File
import java.io.IOException

class PartitionedWayCostFile(var dataDirectoryPath: String, var readOnly: Boolean) {
    var filePath: String? = null
    var currentLatLonDir = LatLonDir(-1000.0, -1000.0)
    var raf: BufferedRandomAccessFile? = null
    var rafCache = BufferedRandomAccessFileCache()
    var numberOfWayCosts = 0

    init {
        if (readOnly) rafCache.setMaxCacheSize(30) else rafCache.setMaxCacheSize(10)
    }

    fun checkAndCreateRandomAccessFile(lld: LatLonDir) {
        if (lld.equals(currentLatLonDir)) return
        currentLatLonDir = lld
        filePath = (currentLatLonDir.makeDir(dataDirectoryPath, !readOnly)
                + File.separatorChar + FILENAME)
        raf = rafCache.getRandomAccessFile(filePath!!, readOnly)
        try {
            val fileLength = raf!!.length()
            numberOfWayCosts = (fileLength / SENTENCE_LENGTH).toInt()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun close() {
        rafCache.close()
    }

    fun insertWay(
        lld: LatLonDir, costFoot: Double, costBike: Double,
        costRacingBike: Double, costMountainBike: Double, costCar: Double,
        costCarShortest: Double
    ): Int {
        checkAndCreateRandomAccessFile(lld)
        return try {
            val id = numberOfWayCosts
            raf!!.seek(numberOfWayCosts.toLong() * SENTENCE_LENGTH)
            raf!!.writeFloat(costFoot.toFloat())
            raf!!.writeFloat(costBike.toFloat())
            raf!!.writeFloat(costRacingBike.toFloat())
            raf!!.writeFloat(costMountainBike.toFloat())
            raf!!.writeFloat(costCar.toFloat())
            raf!!.writeFloat(costCarShortest.toFloat())
            numberOfWayCosts++
            id
        } catch (e: IOException) {
            e.printStackTrace()
            -1
        }
    }

    fun readTransitionCost(lld: LatLonDir, wayCostID: Int, t: Transition): Boolean {
        checkAndCreateRandomAccessFile(lld)
        return try {
            raf!!.seek(wayCostID.toLong() * SENTENCE_LENGTH)
            t.costFoot = raf!!.readFloat().toDouble()
            t.costBike = raf!!.readFloat().toDouble()
            t.costRacingBike = raf!!.readFloat().toDouble()
            t.costMountainBike = raf!!.readFloat().toDouble()
            t.costCar = raf!!.readFloat().toDouble()
            t.costCarShortest = raf!!.readFloat().toDouble()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    // TODO update waycost
    fun updateWay(
        lld: LatLonDir, wayCostID: Int, costFoot: Double, costBike: Double,
        costRacingBike: Double, costMountainBike: Double, costCar: Double,
        costCarShortest: Double
    ) {
        checkAndCreateRandomAccessFile(lld)
        return try {
            raf!!.seek(wayCostID.toLong() * SENTENCE_LENGTH)
            raf!!.writeFloat(costFoot.toFloat())
            raf!!.writeFloat(costBike.toFloat())
            raf!!.writeFloat(costRacingBike.toFloat())
            raf!!.writeFloat(costMountainBike.toFloat())
            raf!!.writeFloat(costCar.toFloat())
            raf!!.writeFloat(costCarShortest.toFloat())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    fun deleteAllWayCostFiles() {
        for (f in File(dataDirectoryPath).listFiles()) if (f.isDirectory && f.name.startsWith("lat_")) for (g in f.listFiles()) if (g.isDirectory && g.name.startsWith(
                "lng_"
            )
        ) for (h in g.listFiles()) if (h.isFile && h.name == FILENAME) h.delete()
    }

    companion object {
        const val SENTENCE_LENGTH = 24L
        const val FILENAME = "wayCost.data"
    }
}