package de.esymetric.jerusalem.rebuilding.optimizer;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.esymetric.jerusalem.ownDataRepresentation.Node;
import de.esymetric.jerusalem.ownDataRepresentation.Transition;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedNodeListFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedTransitionListFile;
import de.esymetric.jerusalem.rebuilding.Rebuilder;
import de.esymetric.jerusalem.utils.BufferedRandomAccessFile;
import de.esymetric.jerusalem.utils.Utils;

public class TransitionsOptimizer {
	
	String dataDirectoryPath;
	PartitionedNodeListFile nlf;
	PartitionedTransitionListFile wlf;
	
	LatLonDir currentLatLonDir = new LatLonDir(-1000, -1000);

	int savedNodes = 0, totalSavedNodes;

	public TransitionsOptimizer(String dataDirectoryPath) {
		this.dataDirectoryPath = dataDirectoryPath;
		
		nlf = new PartitionedNodeListFile(dataDirectoryPath, true);
		nlf.setMaxFileCacheSize(30);
		wlf = new PartitionedTransitionListFile(dataDirectoryPath, false);
		wlf.setMaxFileCacheSize(30);
	}
	
	Transition getReplacementTransition(Node sourceNode, Transition t) {
		Set<Long> foundNodes = new HashSet<Long>();
		foundNodes.add(sourceNode.getUID());
		
		Transition tn = new Transition();
		
		int count = 0;
		tn.targetNode = t.targetNode;
		tn.origTargetNode = t.targetNode;
		tn.id = t.id;
		tn.distanceM = t.distanceM;
		
		
		Transition tf = tn;
		
		for(;;) {
			if( tf == null || tf.targetNode == null ) return null;
			
			List<Transition> ts = tf.targetNode.listTransitionsWithoutSameWayBack(sourceNode, false, nlf, wlf);
			if( ts.size() != 1 ) break;
			count++;
			
			sourceNode = tf.targetNode;
			if( foundNodes.contains(sourceNode.getUID())) return null;
			foundNodes.add(sourceNode.getUID());
			
			if( count > 10000 ) {
				// avoid loops
				ts.clear();
				return null;
			}
			tf = ts.get(0);
			tn.distanceM += tf.distanceM;
			tn.targetNode = tf.targetNode;
			
			ts.clear();
		}
		
		foundNodes.clear();
		
		savedNodes += count;
		return count == 0 ? null : tn;
	}
	
	
	void optimizeNodes(Date startTime, List<Node> nodes, String filePath) {
		
		int count = 0;
		
		//for( Node n : nodes) {
		while( !nodes.isEmpty() ) {
			Node n = nodes.remove(nodes.size() - 1);
			count++;
			if( count % 500000 == 0 ) {
				System.out.print("(" + count + ")");
				Rebuilder.cleanMem(startTime);
			}
			List<Transition> ts = n.listTransitions(false, nlf, wlf);
			
			if( ts.size() <= 2 )
				continue; // optimize ONLY master nodes
			
			for( Transition t : ts ) {
				Transition tn = getReplacementTransition(n, t);
				if( tn != null ) {
					wlf.updateTransition(n, tn, nlf);
					
					/* DEBUG
					Transition tt = wlf.getTransition(n, tn.id, true, nlf);
					if( tt.id != tn.id ) 
						System.out.print("ERROR");
							if( tt.targetNode.getUID() != tn.targetNode.getUID() )
								System.out.print("ERROR");
									if( tt.origTargetNode.getUID() != tn.origTargetNode.getUID() )
										System.out.print("ERROR");
							if( (int)Math.round(tt.distanceM * 10.0) != (int)Math.round(tn.distanceM  * 10.0) )
						System.out.print("ERROR");*/
				}
			}
		}
		
	}
	
	public void close() {
		wlf.close();
		nlf.close();
	}
	
	public void optimize(Date startTime) {
		File[] files = new File(dataDirectoryPath).listFiles();
		Arrays.sort(files);
		for (File f : files)
			if (f.isDirectory() && f.getName().startsWith("lat_")) {
				
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
									&& h.getName().equals(PartitionedNodeListFile.FILE_NAME)) {
								
								int dirLatInt = Integer.parseInt(f.getName()
										.replace("lat_", ""))
										- (int) LatLonDir.LAT_OFFS;
								int dirLngInt = Integer.parseInt(g.getName()
										.replace("lng_", ""))
										- (int) LatLonDir.LNG_OFFS;

								List<Node> nodes = nlf.getAllNodesInFile(h
										.getPath());
								
								System.out.println("\n"
										+ Utils.FormatTimeStopWatch(new Date()
												.getTime()
												- startTime.getTime())
										+ " optimizing lat=" + dirLatInt
										+ " lng=" + dirLngInt + " with "
										+ nodes.size() + " nodes");
								
								
								
								optimizeNodes(startTime, nodes, h.getPath());
								
								totalSavedNodes += savedNodes;
								System.out.println("saved nodes: " + savedNodes + " total saved nodes: " + totalSavedNodes + " braf: " + BufferedRandomAccessFile.getShortInfoAndResetCounters());
								savedNodes = 0;
								Rebuilder.cleanMem(startTime);
								
							}
					}
			}

		System.out.println("\n"
				+ Utils.FormatTimeStopWatch(new Date()
						.getTime()
						- startTime.getTime())
				+ " optimization done");
	}
	

}
