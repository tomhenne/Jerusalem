package de.esymetric.jerusalem.utils;

import java.util.Iterator;


public class MemoryEfficientLongToIntMap {

	final static int HASH_ARRAY_NUMBER_OF_BITS = 22; // 4 mio entries = 4 mio *
														// 4 bytes = 16 mb
														// speicherbedarf
	final static int HASH_ARRAY_SIZE = 1 << HASH_ARRAY_NUMBER_OF_BITS;
	final static long HASH_ARRAY_MASK = (long) (HASH_ARRAY_SIZE - 1);

	class Entry { // 12 bytes
		Entry nextEntry;
		int keyRemainder; // = key >> HASH_ARRAY_NUMBER_OF_BITS
		int value;
	
		long getKey(int hash) {
			return ((long)keyRemainder << HASH_ARRAY_NUMBER_OF_BITS) | (long)hash;		
		}
	}

	Entry[] entries = new Entry[HASH_ARRAY_SIZE];
	int size;

	public void put(long key, int value) {
		int arrayIndex = (int) (key & HASH_ARRAY_MASK);
		int strippedKey = (int) (key >> HASH_ARRAY_NUMBER_OF_BITS);

		Entry oldEntry = entries[arrayIndex];

		Entry foundEntry = findMatchingEntry(oldEntry, strippedKey);

		if (foundEntry == null) {
			// insert new
			Entry e = new Entry();
			e.keyRemainder = strippedKey;
			e.value = value;
			e.nextEntry = oldEntry; // can be null if no entry at that position

			entries[arrayIndex] = e;
			size++;
		} else {
			foundEntry.value = value;
		}
	}

	private Entry findMatchingEntry(Entry e, int strippedKey) {
		while (e != null) {
			if (e.keyRemainder == strippedKey)
				return e;
			e = e.nextEntry;
		}
		return null;
	}

	public Integer get(long key) {

		int arrayIndex = (int) (key & HASH_ARRAY_MASK);
		int strippedKey = (int) (key >> HASH_ARRAY_NUMBER_OF_BITS);

		Entry e = entries[arrayIndex];
		Entry foundEntry = findMatchingEntry(e, strippedKey);
		return foundEntry == null ? null : foundEntry.value;
	}

	public void clear() {
		for (int i = 0; i < entries.length; i++)
			entries[i] = null;
		size = 0;
	}

	
	public Iterator<Long> keysIterator() {
		return new Iterator<Long>() {

			int hash = -1;  // == index in entries array
			Entry entry = null;
			Entry nextEntry = null;
			
			private Entry getNextEntry() {
				Entry foundEntry = null;
				
				if( entry != null ) {
					foundEntry = entry.nextEntry;
				}
				
				if( foundEntry != null ) return foundEntry;
				
				while(true) {
					hash++;
					if( hash >= entries.length ) return null;
					
					foundEntry = entries[hash];
					if( foundEntry != null ) return foundEntry;
				}
			}
			
			
			
			@Override
			public boolean hasNext() {
				if( nextEntry != null ) return true;
				
				nextEntry = getNextEntry();
				return nextEntry != null;
			}

			@Override
			public Long next() {
				entry = nextEntry;
				nextEntry = null;
				return entry.getKey(hash);
			}

			@Override
			public void remove() {
				// not supported
			}
			
		};
	}
	
	public long[] keys() {
		long[] array = new long[size];
		int i = 0, h = 0;
		for( Entry e : entries ) {
			while ( e != null ) {
				array[i++] = e.getKey(h);
				e = e.nextEntry;
			}
			h++; // hash = Index in array
		}
		return array;
	}
}
