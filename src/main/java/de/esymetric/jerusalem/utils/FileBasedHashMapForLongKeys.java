package de.esymetric.jerusalem.utils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;


public class FileBasedHashMapForLongKeys {

	final static int HASH_ARRAY_NUMBER_OF_BITS = 20;
	final static int HASH_ARRAY_SIZE =  1 << HASH_ARRAY_NUMBER_OF_BITS; // 1048576 = 1M
	final static long HASH_ARRAY_MASK = (long)(HASH_ARRAY_SIZE - 1);
	final static int SENTENCE_LENGTH = 12; // 12 bytes: int stripped-key, int value, int
	// nextIndex

	BufferedRandomAccessFile raf;
	BufferedRandomAccessFileCache rafCache = new BufferedRandomAccessFileCache();

	boolean isEmpty = false;
	long fileLength;

	int countGets, countGetReads;

	public float getAvgGetAccessNumberOfReads() {
		float r = (float)countGetReads / (float)countGets;
		countGetReads = 0; countGets = 0;
		return r;
	}

	public int getAndClearNumberOfFileChanges() {
		return rafCache.getAndClearNumberOfFileChanges();
	}

	public void open(String filePath, boolean readOnly) {
		isEmpty = false;
		fileLength = 0L;

		if (readOnly && !new File(filePath).exists()) {
			isEmpty = true;
			return;
		}

		try {
			raf = rafCache.getRandomAccessFile(filePath, readOnly);
			/*
			if( raf != null ) raf.close();
			raf = new RandomAccessFile(filePath, readOnly ? "r" : "rw");
			*/
			if (raf.length() < (long) SENTENCE_LENGTH * (long) HASH_ARRAY_SIZE) {
				final int bufSize = HASH_ARRAY_SIZE; // other sizes could be chosen
				byte[] buf = new byte[bufSize];
				for (int i = 0; i < HASH_ARRAY_SIZE / bufSize * SENTENCE_LENGTH; i++)
					raf.write(buf);
				System.out.print("$");
			}

			fileLength = raf.length();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		rafCache.close();
	}

	public void put(long key, int value) {

		try {
			int arrayIndex = (int)(key & HASH_ARRAY_MASK);
			int strippedKey = (int)(key >> HASH_ARRAY_NUMBER_OF_BITS) + 1;
			long pos = (long) arrayIndex * (long) SENTENCE_LENGTH;
			raf.seek(pos);
			int readKey = raf.readInt();
			raf.readInt();
			int readNextIndex = raf.readInt();

			if (readKey == 0) {
				raf.seek(pos);
				raf.writeInt(strippedKey);
				raf.writeInt(value);
			} else {

				int newIndex = (int) (fileLength / (long) SENTENCE_LENGTH);
				raf.seek(fileLength);
				raf.writeInt(strippedKey);
				raf.writeInt(value);
				raf.writeInt(readNextIndex);
				fileLength += SENTENCE_LENGTH;

				raf.seek(pos + 8L);
				raf.writeInt(newIndex);


			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int get(long key) {
		if (isEmpty)
			return -1;

		try {
			int arrayIndex = (int)(key & HASH_ARRAY_MASK);
			int strippedKey = (int)(key >> HASH_ARRAY_NUMBER_OF_BITS) + 1;
			long pos = (long) arrayIndex * (long) SENTENCE_LENGTH;
			countGets++;
			countGetReads++;
			byte[] buf = new byte[SENTENCE_LENGTH];
			for (;;countGetReads++) {
				raf.seek(pos);
				raf.read(buf);

				ByteArrayInputStream bais = new ByteArrayInputStream(buf);
				DataInputStream dis = new DataInputStream(bais);
				int foundKey = dis.readInt();
				int foundValue = dis.readInt();
				int nextIndex = dis.readInt();
				dis.close();
				bais.close();
				if (foundKey == strippedKey)
					return foundValue;
				if (nextIndex <= 0L)
					return -1;
				else {
					pos = (long) nextIndex * (long) SENTENCE_LENGTH;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

}
