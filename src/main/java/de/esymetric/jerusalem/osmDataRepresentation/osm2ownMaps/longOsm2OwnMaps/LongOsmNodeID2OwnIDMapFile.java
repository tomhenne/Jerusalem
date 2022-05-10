package de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.longOsm2OwnMaps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class LongOsmNodeID2OwnIDMapFile implements LongOsmNodeID2OwnIDMap {

	public final static String DIR = "longOsm2OwnMap";
	final static String FILENAME = "map_";
	public final static int NUMBER_OF_ENTRIES_PER_FILE = 50000000;  // 50 Mio Speicherbedarf 200 MB
	public final static int FILE_SIZE = NUMBER_OF_ENTRIES_PER_FILE * 4;

	int currentFileNumber = -1;
	String currentFilePath = null;
	boolean currentFileIsModified = false;

	int[] entries = new int[NUMBER_OF_ENTRIES_PER_FILE];
	ByteBuffer buf;
	
	String dataDirectoryPath;
	
	public LongOsmNodeID2OwnIDMapFile(String dataDirectoryPath) {
		this.dataDirectoryPath = dataDirectoryPath;
	}
	
	
	@Override
	public int getNumberOfUsedArrays() {
		return 0;
	}

	public static int getFileNumber(long osmNodeID) {
		return (int) (osmNodeID / (long) NUMBER_OF_ENTRIES_PER_FILE);
	}

	public static int getIndexInFile(long osmNodeID, int fileNumber) {
		return (int) (osmNodeID - (long) fileNumber
				* (long) NUMBER_OF_ENTRIES_PER_FILE);
	}

	public boolean isCurrentlyOpenFile(long osmNodeID) {
		return isCurrentlyOpenFile(getFileNumber(osmNodeID));
	}

	boolean isCurrentlyOpenFile(int fileNumber) {
		return fileNumber == currentFileNumber;
	}

	boolean openFile(int fileNumber) {
		if (isCurrentlyOpenFile(fileNumber))
			return true;

		if (currentFileNumber != -1)
			closeFile();

		// load file

		currentFileNumber = fileNumber;
		currentFilePath = getMapFilePath(fileNumber);

		File f = new File(currentFilePath);
		if( f.exists() ) {
			System.out.print("$" + fileNumber);
			return readIntegers();
		}

		// need to clear buffer?
		System.out.print("*" + fileNumber);
		return true;
	}

	public boolean closeFile() {
		if (currentFileNumber == -1)
			return true;

		if( !currentFileIsModified )  return true;
		
		boolean success = writeIntegers();
		System.out.print("+" + currentFileNumber);
		currentFileIsModified = false;
		
		System.gc();
		return success;
	}

	private boolean writeIntegers() {
		if( buf == null )
			buf = ByteBuffer.allocate(4 * NUMBER_OF_ENTRIES_PER_FILE);
		else buf.clear();
		
		for( int i : entries)
			buf.putInt(i);
		
		buf.rewind();
		
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(currentFilePath);
			FileChannel file = out.getChannel();
			file.write(buf);
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			safeClose(out);
			buf = null;
		}
		return true;
	}

	private boolean readIntegers() {
		FileInputStream in = null;
		try {
			in = new FileInputStream(currentFilePath);
			FileChannel file = in.getChannel();
			if( buf == null )
				buf = ByteBuffer.allocate(4 * NUMBER_OF_ENTRIES_PER_FILE);
			else buf.clear();
			file.read(buf);
			
			buf.rewind();
				
			for (int i = 0; i < entries.length; i++) {
				entries[i] = buf.getInt();
			}
			
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			safeClose(in);
		}
		return true;
	}

	private static void safeClose(OutputStream out) {
		try {
			if (out != null) {
				out.close();
			}
		} catch (IOException e) {
			// do nothing
		}
	}

	private static void safeClose(InputStream out) {
		try {
			if (out != null) {
				out.close();
			}
		} catch (IOException e) {
			// do nothing
		}
	}

	@Override
	public boolean put(long osmNodeID, int ownNodeID) {
		int fileNumber = getFileNumber(osmNodeID);
		if( !openFile(fileNumber)) return false;
		
		int indexInFile = getIndexInFile(osmNodeID, fileNumber);
		entries[indexInFile] = ownNodeID;
		currentFileIsModified = true;
		return true;
	}

	private String getMapFilePath(int fileNumber) {
		return dataDirectoryPath + File.separatorChar + DIR + File.separatorChar + FILENAME + fileNumber + ".data";
	}

	@Override
	public int get(long osmNodeID) {
		int fileNumber = getFileNumber(osmNodeID);
		if( !openFile(fileNumber)) return -1;
		
		int indexInFile = getIndexInFile(osmNodeID, fileNumber);
		return entries[indexInFile];
	}

	@Override
	public void close() {
		closeFile();
		if( buf != null ) {
			buf.clear();
			buf = null;
		}
	}

	@Override
	public void delete() {
	}

}
