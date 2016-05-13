package de.esymetric.jerusalem.ownDataRepresentation.fileSystem.unused;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import de.esymetric.jerusalem.ownDataRepresentation.Transition;

public class WayCostFile {

	final static long SENTENCE_LENGTH = 24L;

	String filePath;
	RandomAccessFile raf; // = new BufferedRandomAccessFile(); not possible - file too big for buffers ...
	DataOutputStream dos;
	int nextID;

	private  WayCostFile(String dataDirectoryPath, boolean readOnly,
			boolean startNewFile) {
		filePath = dataDirectoryPath + File.separatorChar + "wayCost.data";
		if (startNewFile)
			deleteFile();
		if (readOnly) {
			try {
				raf = new RandomAccessFile(filePath, "r");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				nextID = (int)(new File(filePath).length() / SENTENCE_LENGTH);
				dos = new DataOutputStream(new BufferedOutputStream(
						new FileOutputStream(filePath), 32000));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public void close() {
		try {
			if (raf != null)
				raf.close();
			if (dos != null)
				dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	boolean deleteFile() {
		new File(filePath).delete();
		return !new File(filePath).exists();
	}

	public boolean exists() {
		return !new File(filePath).exists();
	}

	public int insertWay(double costFoot, double costBike,
			double costRacingBike, double costMountainBike, double costCar,
			double costCarShortest) {
		try {
			int id = nextID;
			
			dos.writeFloat((float) costFoot);
			dos.writeFloat((float) costBike);
			dos.writeFloat((float) costRacingBike);
			dos.writeFloat((float) costMountainBike);
			dos.writeFloat((float) costCar);
			dos.writeFloat((float) costCarShortest);
			nextID++;
			return id;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public boolean readTransitionCost(int wayCostID, Transition t) {
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
	
}
