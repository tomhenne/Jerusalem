package de.esymetric.jerusalem.ownDataRepresentation.fileSystem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import de.esymetric.jerusalem.ownDataRepresentation.Node;
import de.esymetric.jerusalem.utils.BufferedRandomAccessFile;
import de.esymetric.jerusalem.utils.BufferedRandomAccessFileCache;
import de.esymetric.jerusalem.utils.Utils;

public class PartitionedNodeListFile {
	final static long SENTENCE_LENGTH = 12L;
	final static int MAX_CACHE_SIZE = 5000;
	public static final String FILE_NAME = "nodes.data";

	String filePath;
	BufferedRandomAccessFile raf;
	BufferedRandomAccessFileCache rafCache = new BufferedRandomAccessFileCache();
	public void setMaxFileCacheSize(int n) { rafCache.setMaxCacheSize(n); }

	int maxNodeID;
	boolean readOnly;
	String dataDirectoryPath;

	LatLonDir currentLatLonDir = new LatLonDir(-1000, -1000);

	int fileChanges = 0;

	public int getFileChanges() {
		int fileChanges_ = fileChanges;
		fileChanges = 0;
		return fileChanges_;
	}

	public int getFileChangesWithNewFileCreation() {
		return rafCache.getAndClearNumberOfFileChanges();
	}

	public PartitionedNodeListFile(String dataDirectoryPath, boolean readOnly) {
		this.readOnly = readOnly;
		this.dataDirectoryPath = dataDirectoryPath;
		
		if( readOnly )
			rafCache.setMaxCacheSize(30);
	}

	boolean checkAndCreateRandomAccessFile(double lat, double lng) {
		LatLonDir newLatLonDir = new LatLonDir(lat, lng);
		return checkAndCreateRandomAccessFile(newLatLonDir);
	}

	boolean checkAndCreateRandomAccessFile(LatLonDir newLatLonDir) {
		if (newLatLonDir.equals(currentLatLonDir))
			return true;

		fileChanges++;
		currentLatLonDir = newLatLonDir;
		filePath = currentLatLonDir.makeDir(dataDirectoryPath, !readOnly)
				+ File.separatorChar + FILE_NAME;
		raf = rafCache.getRandomAccessFile(filePath, readOnly);
		if( raf == null ) return false;
		try {
			maxNodeID = (int) (raf.length() / SENTENCE_LENGTH) - 1;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void close() {
		rafCache.close();
		
		if( appendNewNodeStream != null ) {
			try {
				appendNewNodeStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		currentLatLonDir = new LatLonDir(-1000, -1000);
		
	}


	public List<Node> getAllNodesInFile(String filePath) {
		long fs = new File(filePath).length();
		int nr = (int)(fs / SENTENCE_LENGTH);
		
		List<Node> arrayList = new ArrayList<Node>();
		DataInputStream dis = null;
		try {
		dis = new DataInputStream(new BufferedInputStream(new FileInputStream(filePath), 32000));
		for(int i = 0; i < nr; i++) {
			Node n = new Node();
			n.id = i;
			n.lat = dis.readFloat();
			n.lng = dis.readFloat();
			//n.nextNodeID = dis.readInt();
			n.transitionID = dis.readInt();
			arrayList.add(n);
		}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			if( dis != null )
				try {
					dis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return arrayList;
	}
	
	
	
	
	DataOutputStream appendNewNodeStream = null;
	LatLonDir appendNewNodeStreamLatLonDir = null;
	
	public int insertNewNodeStreamAppend(double lat, double lng) {
		LatLonDir lld = new LatLonDir(lat, lng);
		if( appendNewNodeStream == null ||
			!lld.equals(appendNewNodeStreamLatLonDir ) ) {
			
			String filePath = lld.makeDir(dataDirectoryPath, true)
					+ File.separatorChar + FILE_NAME;
			
			if( appendNewNodeStream != null ) {
				try {
					appendNewNodeStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			maxNodeID = (int) (new File(filePath).length() / SENTENCE_LENGTH) - 1;
			
			try {
				appendNewNodeStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath, true), 32000));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return -1;
			}
			appendNewNodeStreamLatLonDir = lld;
			
		}
		
		try {
			appendNewNodeStream.writeFloat((float) lat);
			appendNewNodeStream.writeFloat((float) lng);
			//appendNewNodeStream.writeInt(-1);
			appendNewNodeStream.writeInt(-1);

			maxNodeID++;
			return maxNodeID;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	
	
	
	public void setTransitionID(double lat, double lng, int id, int transitionID) {
		if( !checkAndCreateRandomAccessFile(lat, lng) ) return;
		try {
			raf.seek((long) id * SENTENCE_LENGTH + 8L);
			raf.writeInt(transitionID);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	byte[] buf = new byte[(int)SENTENCE_LENGTH];

	public Node getNode(LatLonDir latLonDir, int id) {
		return getNode(new Node(), latLonDir, id);
	}
	
	public Node getNode(Node n, LatLonDir latLonDir, int id) {
		checkAndCreateRandomAccessFile(latLonDir);
		if (raf == null)
			return null;

		if (id < 0 || id > maxNodeID)
			return null;

		try {
			n.id = id;
			raf.seek((long) id * SENTENCE_LENGTH);
			raf.read(buf);

			ByteArrayInputStream bais = new ByteArrayInputStream(buf);
			DataInputStream dis = new DataInputStream(bais);
			n.lat = dis.readFloat();
			n.lng = dis.readFloat();
			//n.nextNodeID = dis.readInt();
			n.transitionID = dis.readInt();
			n.setLatLonDirKey(latLonDir.getShortKey()); // for sorting

			dis.close();
			bais.close();

			return n;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void resetTransitionIDsInAllFilesBuffered(Date startTime) {
		int count = 1;
		File[] files = new File(dataDirectoryPath).listFiles();
		Arrays.sort(files);
		for (File f : files)
			if (f.isDirectory() && f.getName().startsWith("lat_")) {
				
				
				System.out.println(Utils.FormatTimeStopWatch(new Date()
						.getTime()
						- startTime.getTime())
						+ "  >>> " + f.getName() + " # " + count++);
				
				for (File g : f.listFiles())
					if (g != null && g.isDirectory()
							&& g.getName().startsWith("lng_")) {
						File[] list = g.listFiles();
						if( list == null ) {
							System.out.println("Cannot list files in " + g.getPath());
							continue;
						}
						for (File h : list)
							if (h != null && h.isFile()
									&& h.getName().equals(FILE_NAME)) {
								try {
									BufferedRandomAccessFile raf = new BufferedRandomAccessFile();
									raf.open(
											h.getPath(), "rw");
									int l = (int) (raf.length() / SENTENCE_LENGTH);
									for (int i = 0; i < l; i++) {
										raf.seek((long)i * SENTENCE_LENGTH + 8L);
										raf.writeInt(-1);
									}
									raf.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
					}
			}

	}
	
	
	public void resetTransitionIDsInAllFilesStreamed(Date startTime) {
		int count = 1;
		File[] files = new File(dataDirectoryPath).listFiles();
		Arrays.sort(files);
		for (File f : files)
			if (f.isDirectory() && f.getName().startsWith("lat_")) {
				System.out.println(Utils.FormatTimeStopWatch(new Date()
						.getTime()
						- startTime.getTime())
						+ "  >>> " + f.getName() + " # " + count++);
				for (File g : f.listFiles())
					if (g != null && g.isDirectory()
							&& g.getName().startsWith("lng_")) {
						File[] list = g.listFiles();
						if( list == null ) {
							System.out.println("Cannot list files in " + g.getPath());
							continue;
						}
						for (File h : list)
							if (h != null && h.isFile()
									&& h.getName().equals(FILE_NAME)) {
								resetTransitionIDs(h);
							}
					}
			}

	}
	
	void resetTransitionIDs(File f) {
		File f2 = new File(f.getPath() + "_");
		try {
			DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(f), 100000)); 
			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f2), 100000));
			while(dis.available() > 0)
			{
				dos.writeInt(dis.readInt());
				dos.writeInt(dis.readInt());
				//dos.writeInt(dis.readInt());
				dis.readInt();
				dos.writeInt(-1);
			}
			dis.close();
			dos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		f.delete();
		f2.renameTo(f);
	}
	

}
