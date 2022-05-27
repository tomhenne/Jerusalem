package utils

import de.esymetric.jerusalem.utils.MemoryEfficientLongToIntMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MemoryEfficientLongToIntMapTest {
    @Test
    fun testMemoryEfficientLongToIntMap() {
        val map = MemoryEfficientLongToIntMap()
        map.put(23477289977L, 1234567)
        for (i in 0..249) {
            val key = (Math.random() * 2100000000L).toLong()
            val value = (Math.random() * 788823548).toInt()
            map.put(key, value)
            assertTrue(map[key] == value)
        }
        assertTrue(map[23477289977L] == 1234567)


        // test the keys() method
        map.clear()
        map.put(23477289977L, 1234567)
        assertEquals(23477289977L, map.keys()[0])

        // test the keysIterator() method
        map.put(123456789123L, 12345)
        val s: MutableSet<Long> = HashSet()
        var it = map.keysIterator()
        while (it.hasNext()) s.add(it.next())
        assertEquals(2, s.size.toLong())
        assertTrue(s.contains(23477289977L))
        assertTrue(s.contains(123456789123L))
        for (i in 0..249) {
            val value = (Math.random() * 788823548).toInt()
            map.put((i * 21000000L), value)
        }
        s.clear()
        it = map.keysIterator()
        while (it.hasNext()) s.add(it.next())
        assertEquals(252, s.size.toLong())
    }
}