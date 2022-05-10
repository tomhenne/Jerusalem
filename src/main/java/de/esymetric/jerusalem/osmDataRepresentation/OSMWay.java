package de.esymetric.jerusalem.osmDataRepresentation;

import java.util.ArrayList;
import java.util.Map;

import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.MemoryArrayOsmNodeID2OwnIDMap;

public class OSMWay {
	public int id;
	public ArrayList<Long> nodes;  // osm ids of nodes
	public Map<String, String> tags;
	public int wayCostIDForward, wayCostIDBackward;

	private short latLonDirKey;

	public short getLatLonDirID(MemoryArrayOsmNodeID2OwnIDMap osmID2ownIDMap) {  // int to short 25.03.13
		if (latLonDirKey == 0 && nodes.size() > 0 ) {
			latLonDirKey = osmID2ownIDMap.getShortCellID(nodes.get(0));
		}
		return latLonDirKey;
	}
}
