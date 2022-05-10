package utils;

import de.esymetric.jerusalem.utils.MemoryEfficientLongToIntMap;
import org.junit.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MemoryEfficientLongToIntMapTest {

    @Test
    public void testMemoryEfficientLongToIntMap() {
        MemoryEfficientLongToIntMap map = new MemoryEfficientLongToIntMap();

        map.put( 23477289977L, 1234567);

        for (int i = 0; i < 250; i++) {
            long key = (long) (Math.random() * 2100000000L);
            int value = (int) (Math.random() * 788823548);

            map.put(key, value);
            assertTrue(map.get(key) == value);
        }

        assertTrue(map.get(23477289977L) == 1234567);


        // test the keys() method

        map.clear();

        map.put( 23477289977L, 1234567);

        assertEquals(23477289977L, map.keys()[0]);

        // test the keysIterator() method

        map.put( 123456789123L, 12345);

        Set<Long> s = new HashSet<Long>();

        Iterator<Long> it = map.keysIterator();
        while( it.hasNext() )
            s.add(it.next());

        assertEquals(2, s.size());

        assertTrue(s.contains(23477289977L));
        assertTrue(s.contains(123456789123L));

        for (int i = 0; i < 250; i++) {
            long key = (long) (i * 21000000L);
            int value = (int) (Math.random() * 788823548);

            map.put(key, value);
        }

        s.clear();

        it = map.keysIterator();
        while( it.hasNext() )
            s.add(it.next());

        assertEquals(252, s.size());

    }

}
