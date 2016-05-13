package de.esymetric.jerusalem.ownDataRepresentation.fileSystem;

import java.io.File;

public class LatLonDir {

	public final static double LAT_OFFS = 90.0;
	public final static double LNG_OFFS = 180.0;

	int latInt;
	int lngInt;

	public LatLonDir(double lat, double lng) {
		latInt = (int) (lat + LAT_OFFS);
		lngInt = (int) (lng + LNG_OFFS);
	}

	public LatLonDir(double lat, double lng, int offsetBits) {
		latInt = (int) (lat + LAT_OFFS);
		lngInt = (int) (lng + LNG_OFFS);

		if ((offsetBits & 1) != 0)
			latInt++;
		if ((offsetBits & 2) != 0)
			latInt--;
		if ((offsetBits & 4) != 0)
			lngInt++;
		if ((offsetBits & 8) != 0)
			lngInt--;
	}

	public LatLonDir(short shortKey) {
		int key = shortKey;
		if (key < 0)
			key += 65536;
		latInt = key / 360;
		lngInt = key - 360 * latInt;
	}

	public int getOffsetBits(double lat, double lng) {
		LatLonDir lld2 = new LatLonDir(lat, lng);
		int deltaLat = lld2.latInt - latInt;
		int deltaLng = lld2.lngInt - lngInt;
		int bits = 0;
		if (deltaLat == 1)
			bits |= 1;
		if (deltaLat == -1)
			bits |= 2;
		if (deltaLng == 1)
			bits |= 4;
		if (deltaLng == -1)
			bits |= 8;
		return bits;
	}

	public short getShortKey() {
		return (short) (latInt * 360 + lngInt);
	}

	public boolean equals(LatLonDir lld) {
		return lld.latInt == latInt && lld.lngInt == lngInt;
	}

	public String getDir(String dataDirectoryPath) {
		String path = dataDirectoryPath + File.separatorChar + "lat_" + latInt
				+ File.separatorChar + "lng_" + lngInt;
		return path;
	}

	public String makeDir(String dataDirectoryPath, boolean createDir) {
		String path = dataDirectoryPath + File.separatorChar + "lat_" + latInt
				+ File.separatorChar + "lng_" + lngInt;
		if (createDir) {
			File d = new File(path);
			if( !d.exists() )
				d.mkdirs();
		}
		return path;
	}

	public static void deleteAllLatLonDataFiles(String dataDirectoryPath) {
		for (File f : new File(dataDirectoryPath).listFiles())
			if (f.isDirectory() && f.getName().startsWith("lat_"))
				for (File g : f.listFiles())
					if (g.isDirectory() && g.getName().startsWith("lng_"))
						for (File h : g.listFiles())
							if (h.isFile())
								h.delete();

	}

}
