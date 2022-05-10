package de.esymetric.jerusalem.osmDataRepresentation

import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.MemoryArrayOsmNodeID2OwnIDMap

class OSMWay {
    var id = 0
    var nodes // osm ids of nodes
            : ArrayList<Long?>? = null
    var tags: MutableMap<String?, String?>? = null
    var wayCostIDForward = 0
    var wayCostIDBackward = 0
    private var latLonDirKey: Short = 0
    fun getLatLonDirID(osmID2ownIDMap: MemoryArrayOsmNodeID2OwnIDMap): Short {  // int to short 25.03.13
        if (latLonDirKey.toInt() == 0 && nodes!!.size > 0) {
            latLonDirKey = osmID2ownIDMap.getShortCellID(nodes!![0]!!)
        }
        return latLonDirKey
    }
}