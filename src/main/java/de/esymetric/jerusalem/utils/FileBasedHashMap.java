package de.esymetric.jerusalem.utils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;


public class FileBasedHashMap {

	final static int HASH_ARRAY_SIZE = 0x100000;
	final static int HASH_ARRAY_MASK = HASH_ARRAY_SIZE - 1;
	final static int SENTENCE_LENGTH = 12; // 12 bytes: int key, int value, int
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
				byte[] buf = new byte[1024];
				for (int i = 0; i < HASH_ARRAY_SIZE / 1024 * SENTENCE_LENGTH; i++)
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
	
	public void put(int key, int value) {

		try {
			int arrayIndex = key & HASH_ARRAY_MASK;
			long pos = (long) arrayIndex * (long) SENTENCE_LENGTH;
			raf.seek(pos);
			int readKey = raf.readInt();
			raf.readInt();
			int readNextIndex = raf.readInt();
			
			if (readKey == 0) {
				raf.seek(pos);
				raf.writeInt(key);
				raf.writeInt(value);
			} else {
				
				int newIndex = (int) (fileLength / (long) SENTENCE_LENGTH);
				raf.seek(fileLength);
				raf.writeInt(key);
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

	public int get(int key) {
		if (isEmpty)
			return -1;

		try {
			int arrayIndex = key & HASH_ARRAY_MASK;
			long pos = (long) arrayIndex * (long) SENTENCE_LENGTH;
			countGets++;
			countGetReads++;
			byte[] buf = new byte[12];
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
				if (foundKey == key)
					return foundValue;
				if (nextIndex <= 0)
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
