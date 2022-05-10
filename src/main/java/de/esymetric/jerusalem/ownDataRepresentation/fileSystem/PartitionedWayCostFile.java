package de.esymetric.jerusalem.ownDataRepresentation.fileSystem;

import java.io.File;
import java.io.IOException;

import de.esymetric.jerusalem.ownDataRepresentation.Transition;
import de.esymetric.jerusalem.utils.BufferedRandomAccessFile;
import de.esymetric.jerusalem.utils.BufferedRandomAccessFileCache;

public class PartitionedWayCostFile {

	final static long SENTENCE_LENGTH = 24L;
	final static String FILENAME = "wayCost.data";

	String dataDirectoryPath, filePath;
	boolean readOnly;
	LatLonDir currentLatLonDir = new LatLonDir(-1000, -1000);

	BufferedRandomAccessFile raf;
	BufferedRandomAccessFileCache rafCache = new BufferedRandomAccessFileCache();

	int numberOfWayCosts;

	public PartitionedWayCostFile(String dataDirectoryPath, boolean readOnly) {
		this.dataDirectoryPath = dataDirectoryPath;
		this.readOnly = readOnly;
		if( readOnly )
			rafCache.setMaxCacheSize(30);
		else
			rafCache.setMaxCacheSize(10);
	}

	void checkAndCreateRandomAccessFile(LatLonDir lld) {
		//LatLonDir newLatLonDir = new LatLonDir(lat, lng);
		if (lld.equals(currentLatLonDir))
			return;

		currentLatLonDir = lld;

		filePath = currentLatLonDir.makeDir(dataDirectoryPath, !readOnly)
				+ File.separatorChar + FILENAME;
		raf = rafCache.getRandomAccessFile(filePath, readOnly);
		try {
			long fileLength = raf.length();
			numberOfWayCosts = (int) (fileLength / SENTENCE_LENGTH);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void close() {
		rafCache.close();
	}





	public int insertWay(LatLonDir lld, double costFoot, double costBike,
			double costRacingBike, double costMountainBike, double costCar,
			double costCarShortest) {
		checkAndCreateRandomAccessFile(lld);

		try {
			int id = numberOfWayCosts;

			raf.seek((long)numberOfWayCosts * SENTENCE_LENGTH);

			raf.writeFloat((float) costFoot);
			raf.writeFloat((float) costBike);
			raf.writeFloat((float) costRacingBike);
			raf.writeFloat((float) costMountainBike);
			raf.writeFloat((float) costCar);
			raf.writeFloat((float) costCarShortest);
			numberOfWayCosts++;
			return id;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public boolean readTransitionCost(LatLonDir lld, int wayCostID, Transition t) {
		checkAndCreateRandomAccessFile(lld);

		try {
			raf.seek((long) wayCostID * SENTENCE_LENGTH);
			t.costFoot = raf.readFloat();
			t.costBike = raf.readFloat();
			t.costRacingBike = raf.readFloat();
			t.costMountainBike = raf.readFloat();
			t.costCar = raf.readFloat();
			t.costCarShortest = raf.readFloat();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	public void deleteAllWayCostFiles() {
		for(File f : new File(dataDirectoryPath).listFiles() )
			if( f.isDirectory() && f.getName().startsWith("lat_"))
				for(File g : f.listFiles() )
					if( g.isDirectory() && g.getName().startsWith("lng_"))
						for(File h : g.listFiles() )
							if( h.isFile() && h.getName().equals(FILENAME) )
								h.delete();

	}
}
