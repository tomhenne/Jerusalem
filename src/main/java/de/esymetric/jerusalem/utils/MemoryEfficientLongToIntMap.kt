package de.esymetric.jerusalem.utils

import de.esymetric.jerusalem.utils.MemoryEfficientLongToIntMap

class MemoryEfficientLongToIntMap {
    inner class Entry {
        // 12 bytes
        var nextEntry: Entry? = null
        var keyRemainder // = key >> HASH_ARRAY_NUMBER_OF_BITS
                = 0
        var value = 0
        fun getKey(hash: Int): Long {
            return keyRemainder.toLong() shl HASH_ARRAY_NUMBER_OF_BITS or hash.toLong()
        }
    }

    var entries = arrayOfNulls<Entry>(HASH_ARRAY_SIZE)
    var size = 0
    fun put(key: Long, value: Int) {
        val arrayIndex = (key and HASH_ARRAY_MASK).toInt()
        val strippedKey = (key shr HASH_ARRAY_NUMBER_OF_BITS).toInt()
        val oldEntry = entries[arrayIndex]
        val foundEntry = findMatchingEntry(oldEntry, strippedKey)
        if (foundEntry == null) {
            // insert new
            val e: Entry = Entry()
            e.keyRemainder = strippedKey
            e.value = value
            e.nextEntry = oldEntry // can be null if no entry at that position
            entries[arrayIndex] = e
            size++
        } else {
            foundEntry.value = value
        }
    }

    private fun findMatchingEntry(e: Entry?, strippedKey: Int): Entry? {
        var e = e
        while (e != null) {
            if (e.keyRemainder == strippedKey) return e
            e = e.nextEntry
        }
        return null
    }

    operator fun get(key: Long): Int? {
        val arrayIndex = (key and HASH_ARRAY_MASK).toInt()
        val strippedKey = (key shr HASH_ARRAY_NUMBER_OF_BITS).toInt()
        val e = entries[arrayIndex]
        val foundEntry = findMatchingEntry(e, strippedKey)
        return foundEntry?.value
    }

    fun clear() {
        for (i in entries.indices) entries[i] = null
        size = 0
    }

    fun keysIterator(): Iterator<Long> {
        return object : Iterator<Long> {

            var hash = -1 // == index in entries array
            var entry: Entry? = null
            var nextEntry: Entry? = null

            override fun hasNext(): Boolean {
                if (nextEntry != null) return true
                nextEntry = getNextEntryInternal()
                return nextEntry != null
            }

            override fun next(): Long {
                entry = nextEntry
                nextEntry = null
                return entry!!.getKey(hash)
            }

            private fun getNextEntryInternal(): Entry? {
                var foundEntry: Entry? = null
                if (entry != null) {
                    foundEntry = entry!!.nextEntry
                }
                if (foundEntry != null) return foundEntry
                while (true) {
                    hash++
                    if (hash >= entries.size) return null
                    foundEntry = entries[hash]
                    if (foundEntry != null) return foundEntry
                }
            }

        }
    }

    fun keys(): LongArray {
        val array = LongArray(size)
        var i = 0
        var h = 0
        for (e in entries) {
            var en = e
            while (en != null) {
                array[i++] = en.getKey(h)
                en = en.nextEntry
            }
            h++ // hash = Index in array
        }
        return array
    }

    companion object {
        const val HASH_ARRAY_NUMBER_OF_BITS = 22 // 4 mio entries = 4 mio *

        // 4 bytes = 16 mb
        // speicherbedarf
        const val HASH_ARRAY_SIZE = 1 shl HASH_ARRAY_NUMBER_OF_BITS
        const val HASH_ARRAY_MASK = (HASH_ARRAY_SIZE - 1).toLong()
    }
}