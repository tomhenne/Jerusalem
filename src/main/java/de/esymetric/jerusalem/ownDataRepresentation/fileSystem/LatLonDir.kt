package de.esymetric.jerusalem.ownDataRepresentation.fileSystem

import java.io.File

class LatLonDir {
    var latInt: Int
    var lngInt: Int

    constructor(lat: Double, lng: Double) {
        latInt = (lat + LAT_OFFS).toInt()
        lngInt = (lng + LNG_OFFS).toInt()
    }

    constructor(lat: Double, lng: Double, offsetBits: Int) {
        latInt = (lat + LAT_OFFS).toInt()
        lngInt = (lng + LNG_OFFS).toInt()
        if (offsetBits and 1 != 0) latInt++
        if (offsetBits and 2 != 0) latInt--
        if (offsetBits and 4 != 0) lngInt++
        if (offsetBits and 8 != 0) lngInt--
    }

    constructor(shortKey: Short) {
        var key = shortKey.toInt()
        if (key < 0) key += 65536
        latInt = key / 360
        lngInt = key - 360 * latInt
    }

    fun getOffsetBits(lat: Double, lng: Double): Int {
        val lld2 = LatLonDir(lat, lng)
        val deltaLat = lld2.latInt - latInt
        val deltaLng = lld2.lngInt - lngInt
        var bits = 0
        if (deltaLat == 1) bits = bits or 1
        if (deltaLat == -1) bits = bits or 2
        if (deltaLng == 1) bits = bits or 4
        if (deltaLng == -1) bits = bits or 8
        return bits
    }

    val shortKey: Short
        get() = (latInt * 360 + lngInt).toShort()

    fun equals(lld: LatLonDir?): Boolean {
        return lld!!.latInt == latInt && lld.lngInt == lngInt
    }

    fun getDir(dataDirectoryPath: String): String {
        return (dataDirectoryPath + File.separatorChar + "lat_" + latInt
                + File.separatorChar + "lng_" + lngInt)
    }

    fun makeDir(dataDirectoryPath: String, createDir: Boolean): String {
        val path = (dataDirectoryPath + File.separatorChar + "lat_" + latInt
                + File.separatorChar + "lng_" + lngInt)
        if (createDir) {
            val d = File(path)
            if (!d.exists()) d.mkdirs()
        }
        return path
    }

    companion object {
        const val LAT_OFFS = 90.0
        const val LNG_OFFS = 180.0

		fun deleteAllLatLonDataFiles(dataDirectoryPath: String?) {
            for (f in File(dataDirectoryPath).listFiles()) if (f.isDirectory && f.name.startsWith("lat_")) for (g in f.listFiles()) if (g.isDirectory && g.name.startsWith(
                    "lng_"
                )
            ) for (h in g.listFiles()) if (h.isFile) h.delete()
        }
    }
}