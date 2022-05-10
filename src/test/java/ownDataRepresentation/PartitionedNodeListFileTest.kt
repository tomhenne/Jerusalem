package ownDataRepresentation

import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedNodeListFile
import org.junit.Assert
import org.junit.Test

class PartitionedNodeListFileTest {
    @Test
    @Throws(Exception::class)
    fun testPartionedNodeListFile() {
        val dataDirectoryPath = "testData"
        var nlf = PartitionedNodeListFile(dataDirectoryPath, false)
        val nodeID = nlf.insertNewNodeStreamAppend(48.11, 11.48)
        val nodeID2 = nlf.insertNewNodeStreamAppend(48.111, 11.481)
        val nodeID3 = nlf.insertNewNodeStreamAppend(-48.111, -11.481)
        nlf.close()

        // ---
        nlf = PartitionedNodeListFile(dataDirectoryPath, false)
        nlf.setTransitionID(48.111, 11.481, nodeID2, 2222)
        nlf.setTransitionID(48.11, 11.48, nodeID, 1111)
        nlf.setTransitionID(-48.111, -11.481, nodeID3, 3333)
        nlf.close()

        // ---
        nlf = PartitionedNodeListFile(dataDirectoryPath, true)
        val n1 = nlf.getNode(LatLonDir(48.11, 11.48), nodeID)
        Assert.assertEquals(1111, n1!!.transitionID.toLong())
        val n2 = nlf.getNode(LatLonDir(48.111, 11.481), nodeID2)
        Assert.assertEquals(2222, n2!!.transitionID.toLong())
        val n3 = nlf.getNode(LatLonDir(-48.111, -11.481), nodeID3)
        Assert.assertEquals(3333, n3!!.transitionID.toLong())
        nlf.close()
    }
}