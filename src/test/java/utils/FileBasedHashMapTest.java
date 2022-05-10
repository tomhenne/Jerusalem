package utils;

import de.esymetric.jerusalem.utils.FileBasedHashMap;
import de.esymetric.jerusalem.utils.FileBasedHashMapForLongKeys;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class FileBasedHashMapTest {

    @Test
    public void testFileBasedHashMap() {
        new File("testData/fileBasedHashMapTest.data").delete();
        FileBasedHashMap fbhm = new FileBasedHashMap();
        fbhm.open("testData/fileBasedHashMapTest.data", false);
        for (int id = 1000000; id > 0; id -= 111) {
            int value = (int) (Math.random() * 788823548);
            fbhm.put(id, value);
            assertTrue(fbhm.get(id) == value);
        }
        for (int id = 1000001; id < 1286746; id += 263) {
            int value = (int) (Math.random() * 788823548);
            fbhm.put(id, value);
            if (fbhm.get(id) != value) {
                fbhm.put(id, value);
                assertTrue(fbhm.get(id) == value);
            }
            assertTrue(fbhm.get(id) == value);
        }
        for (int id = 2000001; id < 3000000; id += 555) {
            int key = id;
            int value = (int) (Math.random() * 788823548);
            fbhm.put(key, value);
            assertTrue(fbhm.get(key) == value);
        }
        fbhm.close();
        new File("testData/fileBasedHashMapTest.data").delete();
    }

    @Test
    public void testFileBasedHashMapForLongKeys() throws Exception {
        new File("testData/fileBasedHashMapTestForLongKeys.data").delete();
        FileBasedHashMapForLongKeys fbhm = new FileBasedHashMapForLongKeys();
        fbhm.open("testData/fileBasedHashMapTestForLongKeys.data", false);
        for (int id = 1000000; id > 0; id -= 111) {
            int value = (int) (Math.random() * 788823548);
            fbhm.put(id, value);
            assertTrue(fbhm.get(id) == value);
        }
        for (int id = 1000001; id < 1286746; id += 263) {
            int value = (int) (Math.random() * 788823548);
            fbhm.put(id, value);
            if (fbhm.get(id) != value) {
                fbhm.put(id, value);
                assertTrue(fbhm.get(id) == value);
            }
            assertTrue(fbhm.get(id) == value);
        }
        for (long id = 3000000000L; id < 3300000000L; id += 23555L) {
            long key = id;
            int value = (int) (Math.random() * 788823548);
            fbhm.put(key, value);
            assertTrue(fbhm.get(key) == value);
        }
        fbhm.close();
        new File("testData/fileBasedHashMapTestForLongKeys.data").delete();
    }

}
