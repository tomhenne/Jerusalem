package ownDataRepresentation;

import de.esymetric.jerusalem.ownDataRepresentation.Node;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedNodeListFile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PartitionedNodeListFileTest {

    @Test
    public void testPartionedNodeListFile() throws Exception {
        String dataDirectoryPath = "testData";
        PartitionedNodeListFile nlf = new PartitionedNodeListFile(dataDirectoryPath, false);

        int nodeID = nlf.insertNewNodeStreamAppend(48.11, 11.48);

        int nodeID2 = nlf.insertNewNodeStreamAppend(48.111, 11.481);

        int nodeID3 = nlf.insertNewNodeStreamAppend(-48.111, -11.481);


        nlf.close();

        // ---

        nlf = new PartitionedNodeListFile(dataDirectoryPath, false);

        nlf.setTransitionID(48.111, 11.481, nodeID2, 2222);
        nlf.setTransitionID(48.11, 11.48, nodeID, 1111);
        nlf.setTransitionID(-48.111, -11.481, nodeID3, 3333);

        nlf.close();

        // ---

        nlf = new PartitionedNodeListFile(dataDirectoryPath, true);
        Node n1 = nlf.getNode(new LatLonDir(48.11, 11.48), nodeID);
        assertEquals(1111, n1.transitionID);

        Node n2 = nlf.getNode(new LatLonDir(48.111, 11.481), nodeID2);
        assertEquals(2222, n2.transitionID);

        Node n3 = nlf.getNode(new LatLonDir(-48.111, -11.481), nodeID3);
        assertEquals(3333, n3.transitionID);

        nlf.close();
    }
}
