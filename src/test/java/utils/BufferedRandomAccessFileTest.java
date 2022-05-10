package utils;

import de.esymetric.jerusalem.utils.BufferedRandomAccessFile;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BufferedRandomAccessFileTest {
    @Test

    public void testBufferedRandomAccessFile() throws Exception {
        new File("testData/braf.data").delete();

        BufferedRandomAccessFile braf = new BufferedRandomAccessFile();
        braf.open("testData/braf.data", "rw");
        for (int i = 0; i < 10000; i++)
            braf.writeInt(-1);
        braf.close();

        braf.open("testData/braf.data", "rw");
        braf.seek(20000);
        for (int i = 0; i < 10000; i++)
            braf.writeInt(-1);
        braf.close();

        assertEquals(60000, new File("testData/braf.data").length());

        braf.open("testData/braf.data", "rw");
        for (int i = 11; i < 2000; i += 10) {
            braf.seek(i);
            braf.writeInt(i);
        }
        for (int i = 11; i < 2000; i += 10) {
            braf.seek(i);
            assertTrue(braf.readInt() == i);
        }
        for (int i = 55; i < 2009; i += 9) {
            braf.seek(i);
            braf.writeInt(i);
        }
        braf.close();

        braf.open("testData/braf.data", "rw");
        for (int i = 55; i < 2009; i += 9) {
            braf.seek(i);
            assertTrue(braf.readInt() == i);
        }
        braf.close();
    }

}
