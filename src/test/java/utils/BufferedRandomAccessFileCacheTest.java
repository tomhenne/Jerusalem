package utils;

import de.esymetric.jerusalem.utils.BufferedRandomAccessFile;
import de.esymetric.jerusalem.utils.BufferedRandomAccessFileCache;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BufferedRandomAccessFileCacheTest {


    @Test
    public void testBufferedRandomAccessFileCache() throws Exception {
        BufferedRandomAccessFileCache rafCache = new BufferedRandomAccessFileCache();

        for (int i = 0; i <= 30; i++) {
            BufferedRandomAccessFile raf = rafCache.getRandomAccessFile(
                    "testData/rafCache" + i + ".data", false);
            for (int j = 0; j < 200000; j++) {
                int p = (int)(Math.random() * 20000);
                raf.seek(p);
                raf.writeInt(j);
            }
            raf.seek(999);
            raf.writeInt(333);
        }

        for (int i = 10; i > 0; i--) {
            BufferedRandomAccessFile raf = rafCache.getRandomAccessFile(
                    "testData/rafCache" + i + ".data", false);
            raf.seek(999);
            assertEquals(333, raf.readInt());
        }

        rafCache.close();
    }

}
