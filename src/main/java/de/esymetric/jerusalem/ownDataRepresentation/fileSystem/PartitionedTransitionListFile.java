package de.esymetric.jerusalem.ownDataRepresentation.fileSystem;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import de.esymetric.jerusalem.ownDataRepresentation.Node;
import de.esymetric.jerusalem.ownDataRepresentation.Transition;
import de.esymetric.jerusalem.utils.BufferedRandomAccessFile;
import de.esymetric.jerusalem.utils.BufferedRandomAccessFileCache;

public class PartitionedTransitionListFile {

	final static long SENTENCE_LENGTH = 22L;
	final static String FILENAME = "transitions.data";

	LatLonDir currentLatLonDir = new LatLonDir(-1000, -1000);
	String filePath;
	BufferedRandomAccessFile raf;
	BufferedRandomAccessFileCache rafCache = new BufferedRandomAccessFileCache();
	boolean readOnly;
	String dataDirectoryPath;
	int numberOfTransitions;

	public PartitionedTransitionListFile(String dataDirectoryPath,
			boolean readOnly) {
		this.readOnly = readOnly;
		this.dataDirectoryPath = dataDirectoryPath;

		if (readOnly)
			rafCache.setMaxCacheSize(30);
	}
	
	public void setMaxFileCacheSize(int s) { rafCache.setMaxCacheSize(s); }

	void checkAndCreateRandomAccessFile(double lat, double lng) {
		LatLonDir newLatLonDir = new LatLonDir(lat, lng);
		if (newLatLonDir.equals(currentLatLonDir))
			return;

		currentLatLonDir = newLatLonDir;

		filePath = currentLatLonDir.makeDir(dataDirectoryPath, !readOnly)
				+ File.separatorChar + FILENAME;
		raf = rafCache.getRandomAccessFile(filePath, readOnly);
		try {
			long fileLength = raf.length();
			numberOfTransitions = (int) (fileLength / SENTENCE_LENGTH);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void close() {
		rafCache.close();
	}

	public int insertTransition(Node sourceNode, Node targetNode,
			double distanceM, int nextTransitionID, int wayCostID,
			short wayCostLatLonDirKey) {
		checkAndCreateRandomAccessFile(sourceNode.lat, sourceNode.lng);

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream daos = new DataOutputStream(baos);
			int targetNodeID = (int) targetNode.id << 4;
			int offsetBits = currentLatLonDir.getOffsetBits(targetNode.lat,
					targetNode.lng);
			daos.writeInt(targetNodeID | offsetBits);
			daos.writeInt(-1); // origTargetNodeID, for TransitionOptimizer
			daos.writeInt(nextTransitionID);
			daos.writeInt(wayCostID);
			daos.writeShort(wayCostLatLonDirKey);
			daos.writeFloat((float) distanceM);
			raf.seek((long) numberOfTransitions * SENTENCE_LENGTH);
			raf.write(baos.toByteArray());
			daos.close();
			baos.close();
			int id = numberOfTransitions;
			numberOfTransitions++;
			return id;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public Transition getTransition(Node sourceNode, int transitionID, boolean loadOriginalTargetNode,
			PartitionedNodeListFile nlf) {
		return getTransition(sourceNode, transitionID, loadOriginalTargetNode,nlf, null);
	}

	public Transition getTransition(Node sourceNode, int transitionID, boolean loadOriginalTargetNode,
			PartitionedNodeListFile nlf, PartitionedWayCostFile wcf) {
		checkAndCreateRandomAccessFile(sourceNode.lat, sourceNode.lng);
		try {
			if( !raf.seek((long) transitionID * SENTENCE_LENGTH) )
				return null;
			Transition t = new Transition();
			t.id = transitionID;
			
			int targetNodeID = raf.readInt();
			int offsetBits = targetNodeID & 0xF;
			targetNodeID = targetNodeID >> 4;
			t.targetNode = nlf.getNode(new LatLonDir(sourceNode.lat,
					sourceNode.lng, offsetBits), targetNodeID);
			
			if (t.targetNode == null) {
				// DEBUG

				System.out
						.println("Transition: cannot get target node with id "
								+ targetNodeID);
				t.targetNode = nlf.getNode(new LatLonDir(sourceNode.lat,
						sourceNode.lng, offsetBits), targetNodeID);
				return null;
			}

			int origTargetNodeID = raf.readInt();  // TransitionOptimizer backed up node
			if( loadOriginalTargetNode &&  origTargetNodeID != -1) {
				offsetBits = origTargetNodeID & 0xF;
				origTargetNodeID = origTargetNodeID >> 4;
				t.origTargetNode = nlf.getNode(new LatLonDir(sourceNode.lat,
						sourceNode.lng, offsetBits), origTargetNodeID);
			}
			
			t.nextTransitionID = raf.readInt();
			int wayCostID = raf.readInt();
			short wayCostLatLonDirKey = raf.readShort();
			t.distanceM = raf.readFloat();
			if (wcf != null)
				wcf.readTransitionCost(new LatLonDir(wayCostLatLonDirKey),
						wayCostID, t);
			return t;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public int getNumberOfFileChanges() {
		return rafCache.getAndClearNumberOfFileChanges();
	}

	public void deleteAllTransitionFiles() {
		for (File f : new File(dataDirectoryPath).listFiles())
			if (f.isDirectory() && f.getName().startsWith("lat_"))
				for (File g : f.listFiles())
					if (g.isDirectory() && g.getName().startsWith("lng_"))
						for (File h : g.listFiles())
							if (h.isFile() && h.getName().equals(FILENAME))
								h.delete();

	}

	public boolean updateTransition(Node sourceNode, Transition t,
			PartitionedNodeListFile nlf) {

		checkAndCreateRandomAccessFile(sourceNode.lat, sourceNode.lng);
		try {
			raf.seek((long) t.id * SENTENCE_LENGTH);

			Node targetNode = t.targetNode;
			int targetNodeID = (int) targetNode.id << 4;
			int offsetBits = currentLatLonDir.getOffsetBits(targetNode.lat,
					targetNode.lng);
			raf.writeInt(targetNodeID | offsetBits);

			Node origTargetNode = t.origTargetNode;
			int origTargetNodeID = (int) origTargetNode.id << 4;
			offsetBits = currentLatLonDir.getOffsetBits(origTargetNode.lat,
					origTargetNode.lng);
			raf.writeInt(origTargetNodeID | offsetBits);

			raf.seek((long) t.id * SENTENCE_LENGTH + 18L); // targetNodeID,
															// origTargetNodeID,
															// nextTransitionID,
															// wayCostID,
															// wayCostLatLonDirKey

			raf.writeFloat((float) t.distanceM);

			return true;
		} catch (IOException e) {  
			e.printStackTrace();
			return false;
		}

	}

}
