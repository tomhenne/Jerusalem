package de.esymetric.jerusalem.osmDataRepresentation;

import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir;

public class OSMNode {
	public long id;
	public double lat;
	public double lng;
	public int ownID;

	private short latLonDirKey;
	
	public int getLatLonDirKey() {
		if (latLonDirKey == 0) {

			LatLonDir lld = new LatLonDir(lat, lng);
			latLonDirKey = lld.getShortKey();
		}
		return latLonDirKey;
	}

}
