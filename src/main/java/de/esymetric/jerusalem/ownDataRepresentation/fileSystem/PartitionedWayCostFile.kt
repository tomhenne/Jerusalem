package de.esymetric.jerusalem.ownDataRepresentation.fileSystem

import de.esymetric.jerusalem.ownDataRepresentation.Transition
import de.esymetric.jerusalem.routing.RoutingHeuristics.Companion.BLOCKED_WAY_COST
import de.esymetric.jerusalem.utils.BufferedRandomAccessFile
import de.esymetric.jerusalem.utils.BufferedRandomAccessFileCache
import java.io.File
import java.io.IOException

class PartitionedWayCostFile(var dataDirectoryPath: String, var readOnly: Boolean) {
    var filePath: String? = null
    var currentLatLonDir = LatLonDir(-1000.0, -1000.0)
    var raf: BufferedRandomAccessFile? = null
    private var rafCache = BufferedRandomAccessFileCache()
    private var numberOfWayCosts = 0

    init {
        if (readOnly) rafCache.setMaxCacheSize(30) else rafCache.setMaxCacheSize(10)
    }

    private fun checkAndCreateRandomAccessFile(lld: LatLonDir) {
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
            raf!!.writeUShort(costDouble2Short(costFoot))
            raf!!.writeUShort(costDouble2Short(costBike))
            raf!!.writeUShort(costDouble2Short(costRacingBike))
            raf!!.writeUShort(costDouble2Short(costMountainBike))
            raf!!.writeUShort(costDouble2Short(costCar))
            raf!!.writeUShort(costDouble2Short(costCarShortest))
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
            t.costFoot = short2DoubleCost(raf!!.readUShort())
            t.costBike = short2DoubleCost(raf!!.readUShort())
            t.costRacingBike = short2DoubleCost(raf!!.readUShort())
            t.costMountainBike = short2DoubleCost(raf!!.readUShort())
            t.costCar = short2DoubleCost(raf!!.readUShort())
            t.costCarShortest = short2DoubleCost(raf!!.readUShort())
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun deleteAllWayCostFiles() {
        for (f in File(dataDirectoryPath).listFiles()) if (f.isDirectory && f.name.startsWith("lat_")) for (g in f.listFiles()) if (g.isDirectory && g.name.startsWith(
                "lng_"
            )
        ) for (h in g.listFiles()) if (h.isFile && h.name == FILENAME) h.delete()
    }

    private fun costDouble2Short(cost: Double) : UShort {
        if (cost == BLOCKED_WAY_COST) return BLOCKED_WAY_COST_USHORT

        var costM = cost * MULT_FACTOR_SHORT_TO_DOUBLE
        if (costM > 64000.0 || costM < 0.0) {
            println("Error: invalid cost " + cost)
            costM = 64000.0
        }
        return costM.toUInt().toUShort()
    }

    private fun short2DoubleCost(cost: UShort): Double {
        if ( cost == BLOCKED_WAY_COST_USHORT ) return BLOCKED_WAY_COST

        return cost.toDouble() / MULT_FACTOR_SHORT_TO_DOUBLE
    }

    companion object {
        const val SENTENCE_LENGTH = 12L
        const val FILENAME = "wayCost.data"
        const val MULT_FACTOR_SHORT_TO_DOUBLE = 64.0
        const val BLOCKED_WAY_COST_USHORT = UShort.MAX_VALUE
    }
}