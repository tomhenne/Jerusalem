package de.esymetric.jerusalem.osmDataRepresentation.unused;

import java.io.File;
import java.util.Date;

import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.longOsm2OwnMaps.LongOsmNodeID2OwnIDMap;
import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.longOsm2OwnMaps.LongOsmNodeID2OwnIDMapFile;


public class LongOsmNodeID2OwnIDMapFileBased implements LongOsmNodeID2OwnIDMap {
	
	String dataDirectoryPath;
	
	LongOsmNodeID2OwnIDMapFile f1;
	
	
	public LongOsmNodeID2OwnIDMapFileBased(String dataDirectoryPath) {
		this.dataDirectoryPath = dataDirectoryPath;
		
		String dirPath = dataDirectoryPath + File.separatorChar + LongOsmNodeID2OwnIDMapFile.DIR;
		deleteRecursively(dirPath);
		new File(dirPath).mkdir();
		
		f1 = new LongOsmNodeID2OwnIDMapFile(dataDirectoryPath);
	}
	
	public static void deleteRecursively(String dirPath) {
		File f = new File(dirPath);
		if (!f.exists() || !f.isDirectory())
			return;

		for (File sf : f.listFiles()) {
			if (sf.isDirectory())
				deleteRecursively(sf.getPath());
			else
				sf.delete();
		}
		f.delete();
	}

	
	 
	@Override
	public boolean put(long osmNodeID, int ownNodeID) {
		
		return f1.put(osmNodeID, ownNodeID);
		
	}
	

	@Override
	public int get(long osmNodeID) {
		return f1.get(osmNodeID);
	}

	@Override
	public void close() {
		f1.close();
		
	}

	@Override
	public void delete() {
		f1.delete();
		
	}


	@Override
	public int getNumberOfUsedArrays() {
		return 0;
	}

	public void loadExistingOsm2OwnIDIntoMemory(Date startTime) {
		// TODO Auto-generated method stub
		
	}

	public void setReadOnly() {
		// TODO Auto-generated method stub
		
	}


}
