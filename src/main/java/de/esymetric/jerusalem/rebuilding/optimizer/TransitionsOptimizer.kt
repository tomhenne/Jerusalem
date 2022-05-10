package de.esymetric.jerusalem.rebuilding.optimizer

import de.esymetric.jerusalem.ownDataRepresentation.Node
import de.esymetric.jerusalem.ownDataRepresentation.Transition
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedNodeListFile
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedTransitionListFile
import de.esymetric.jerusalem.rebuilding.Rebuilder
import de.esymetric.jerusalem.utils.BufferedRandomAccessFile
import de.esymetric.jerusalem.utils.Utils
import java.io.File
import java.util.*

class TransitionsOptimizer(var dataDirectoryPath: String) {
    var nlf: PartitionedNodeListFile
    var wlf: PartitionedTransitionListFile
    var savedNodes = 0
    var totalSavedNodes = 0

    init {
        nlf = PartitionedNodeListFile(dataDirectoryPath, true)
        nlf.setMaxFileCacheSize(30)
        wlf = PartitionedTransitionListFile(dataDirectoryPath, false)
        wlf.setMaxFileCacheSize(30)
    }

    fun getReplacementTransition(sourceNode: Node, t: Transition): Transition? {
        var sourceNode = sourceNode
        val foundNodes: MutableSet<Long> = HashSet()
        foundNodes.add(sourceNode.uid)
        val tn = Transition()
        var count = 0
        tn.targetNode = t.targetNode
        tn.origTargetNode = t.targetNode
        tn.id = t.id
        tn.distanceM = t.distanceM
        var tf = tn
        while (true) {
            if (tf == null || tf.targetNode == null) return null
            val ts = tf.targetNode.listTransitionsWithoutSameWayBack(sourceNode, false, nlf, wlf)
            if (ts.size != 1) break
            count++
            sourceNode = tf.targetNode
            if (foundNodes.contains(sourceNode.uid)) return null
            foundNodes.add(sourceNode.uid)
            if (count > 10000) {
                // avoid loops
                ts.clear()
                return null
            }
            tf = ts[0]
            tn.distanceM += tf.distanceM
            tn.targetNode = tf.targetNode
            ts.clear()
        }
        foundNodes.clear()
        savedNodes += count
        return if (count == 0) null else tn
    }

    fun optimizeNodes(startTime: Date, nodes: MutableList<Node>) {
        var count = 0

        while (!nodes.isEmpty()) {
            val n: Node = nodes.removeAt(nodes.size - 1)
            count++
            if (count % 500000 == 0) {
                print("($count)")
                Rebuilder.cleanMem(startTime)
            }
            val ts = n.listTransitions(false, nlf, wlf)
            if (ts.size <= 2) continue  // optimize ONLY master nodes
            for (t in ts) {
                val tn = getReplacementTransition(n, t)
                if (tn != null) {
                    wlf.updateTransition(n, tn, nlf)

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

    fun close() {
        wlf.close()
        nlf.close()
    }

    fun optimize(startTime: Date) {
        val files = File(dataDirectoryPath).listFiles()
        Arrays.sort(files)
        for (f in files) if (f.isDirectory && f.name.startsWith("lat_")) {
            for (g in f.listFiles()) if (g != null && g.isDirectory
                && g.name.startsWith("lng_")
            ) {
                val list = g.listFiles()
                if (list == null) {
                    println("Cannot list files in " + g.path)
                    continue
                }
                for (h in list) if (h != null && h.isFile
                    && h.name == PartitionedNodeListFile.FILE_NAME
                ) {
                    val dirLatInt = f.name
                        .replace("lat_", "").toInt() - LatLonDir.LAT_OFFS.toInt()
                    val dirLngInt = g.name
                        .replace("lng_", "").toInt() - LatLonDir.LNG_OFFS.toInt()
                    val nodes = nlf.getAllNodesInFile(
                        h.path
                    ).toMutableList()
                    println(
                        "\n"
                                + Utils.formatTimeStopWatch(
                            Date()
                                .time
                                    - startTime.time
                        )
                                + " optimizing lat=" + dirLatInt
                                + " lng=" + dirLngInt + " with "
                                + nodes.size + " nodes"
                    )
                    optimizeNodes(startTime, nodes)
                    totalSavedNodes += savedNodes
                    println("saved nodes: " + savedNodes + " total saved nodes: " + totalSavedNodes + " braf: " + BufferedRandomAccessFile.getShortInfoAndResetCounters())
                    savedNodes = 0
                    Rebuilder.cleanMem(startTime)
                }
            }
        }
        println(
            "\n"
                    + Utils.formatTimeStopWatch(
                Date()
                    .time
                        - startTime.time
            )
                    + " optimization done"
        )
    }
}