package de.esymetric.jerusalem.osmDataRepresentation

import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir

class OSMNode {
    @JvmField
	var id: Long = 0
    @JvmField
	var lat = 0.0
    @JvmField
	var lng = 0.0
    @JvmField
	var ownID = 0
    private var latLonDirKey: Short = 0
    fun getLatLonDirKey(): Int {
        if (latLonDirKey.toInt() == 0) {
            val lld = LatLonDir(lat, lng)
            latLonDirKey = lld.shortKey
        }
        return latLonDirKey.toInt()
    }
}