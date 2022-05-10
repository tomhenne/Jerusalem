package de.esymetric.jerusalem.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class BufferedRandomAccessFile  {
	final static int SIZE_INCREMENT = 800000;  // 0,8 MB
	final static int INITIAL_SIZE =   1500000;  // 1,5 MB

	byte[] buf;
	int index;
	int size;
	String filePath;
	boolean isChanged = false;

	private static int readCount, writeCount;

	private static long readFileSize, writtenFileSize;

	private static int openFileCount;

	private static long openFileSize;

	public static String getShortInfoAndResetCounters() {
		return
				"r#" + getAndResetReadCount() +
				"/rfs" + (getAndResetReadFileSize() / 1024L / 1024L) + "mb" +
				"/w#" + getAndResetWriteCount() +
				"/wfs" + (getAndResetWrittenFileSize() / 1024L / 1024L) + "mb" +
				"/op#" + getOpenFileCount() +
				"/fs" + (getOpenFileSize() / 1024L / 1024L) + "mb";
	}

	public static int getOpenFileCount() {
		return openFileCount;
	}

	public static long getOpenFileSize() {
		return openFileSize;
	}

	public static int getAndResetReadCount() {
		int c = readCount;
		readCount = 0;
		return c;
	}

	public static long getAndResetReadFileSize() {
		long c = readFileSize;
		readFileSize = 0;
		return c;
	}

	public static int getAndResetWriteCount() {
		int c = writeCount;
		writeCount = 0;
		return c;
	}

	public static long getAndResetWrittenFileSize() {
		long c = writtenFileSize;
		writtenFileSize = 0;
		return c;
	}

	public void open(String filePath, String mode) throws FileNotFoundException {

		if( this.filePath != null ) {

			if( buf != null && this.filePath.equals(filePath) ) return;

			try {
				close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		this.filePath = filePath;

		index = 0;
		size = 0;

		if (new File(filePath).exists()) {
			size = (int) new File(filePath).length();
			int newBufSize = size + ("r".equals(mode) ? 0 : SIZE_INCREMENT);
			if( buf == null || buf.length < newBufSize ) {
				if( buf != null ) openFileSize -= buf.length;
				buf = new byte[newBufSize];
				openFileSize += newBufSize;
			}

			if( !readFromFile(buf, filePath)) {
				System.out.println("ERROR cannot read from file " + filePath);
			}

		}
		else {
			if( buf == null || buf.length < INITIAL_SIZE) {
				if( buf != null ) openFileSize -= buf.length;
				openFileSize += INITIAL_SIZE;
				buf = new byte[INITIAL_SIZE];
			}
		}

		openFileCount++;
	}

	public int getSize() {
		return size;
	}

	public void close() throws IOException {

		if( isChanged )
			if( !writeToFile(buf, size, filePath) )
				System.out.println("ERROR cannot write to file " + filePath);

		isChanged = false;
		filePath = null;
		if( buf != null ) {
			openFileSize -= buf.length;
			buf = null;
			openFileCount--;
		}
	}

	private static boolean readFromFile(byte [] data, String filePath) {
		if( data == null ) return false;

		FileInputStream in = null;
		try {
			in = new FileInputStream(filePath);
			FileChannel file = in.getChannel();
			ByteBuffer buf = ByteBuffer.wrap(data); // allocate(4 * NUMBER_OF_ENTRIES_PER_FILE);
			readFileSize += file.read(buf);
			file.close();
			//System.out.print('#');
			readCount++;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			safeClose(in);
		}
		return true;
	}


	private static boolean writeToFile(byte [] data, int size, String filePath) {
		if( data == null ) return false;

			ByteBuffer buf = ByteBuffer.wrap(data, 0, size);

			buf.rewind();


			FileOutputStream out = null;
			try {
				out = new FileOutputStream(filePath);
				FileChannel file = out.getChannel();
				file.write(buf);
				file.close();
				//System.out.print('%');
				writeCount++;
				writtenFileSize += size;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} finally {
				safeClose(out);
			}
			return true;
	}

	private static void safeClose(OutputStream out) {
		try {
			if (out != null) {
				out.close();
			}
		} catch (IOException e) {
			// do nothing
		}
	}

	private static void safeClose(InputStream out) {
		try {
			if (out != null) {
				out.close();
			}
		} catch (IOException e) {
			// do nothing
		}
	}

	public short readShort() throws IOException {
		if( index > buf.length - 2 ) {
			System.out.println("Out of bounds!!");
		}

		if( index > size - 2 ) {
			System.out.println("Out of bounds also!!");
		}

		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf,
				index, 2));
		short r = dis.readShort();
		dis.close();
		index += 2;
		return r;
	}

	public int readInt() throws IOException {
		if( index > buf.length - 4 ) {
			System.out.println("Out of bounds!!");
		}

		if( index > size - 4 ) {
			System.out.println("Out of bounds also!!");
		}

		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf,
				index, 4));
		int r = dis.readInt();
		dis.close();
		index += 4;
		return r;
	}

	public long readLong() throws IOException {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf,
				index, 8));
		long r = dis.readLong();
		dis.close();
		index += 8;
		return r;
	}

	public float readFloat() throws IOException {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf,
				index, 4));
		float r = dis.readFloat();
		dis.close();
		index += 4;
		return r;
	}

	public void read(byte[] buffer) throws IOException {
		// not tested

		System.arraycopy(buf, index, buffer, 0, buffer.length);
		index += buffer.length;
	}

	public boolean seek(int i) {
		if( i < 0 )
			System.out.println("negative seek");

		index = i;

		return i < size;  // not eof
	}

	public boolean seek(long i) {
		return seek((int)i);
	}

	public void writeInt(int v) throws IOException {
		increaseBuffer(4);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(4);
		DataOutputStream dis = new DataOutputStream(bos);
		dis.writeInt(v);
		System.arraycopy(bos.toByteArray(), 0, buf, index, 4);
		dis.close();
		bos.close();
		index += 4;
		isChanged = true;
	}

	public void writeLong(int v) throws IOException {
		increaseBuffer(8);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(4);
		DataOutputStream dis = new DataOutputStream(bos);
		dis.writeLong(v);
		System.arraycopy(bos.toByteArray(), 0, buf, index, 8);
		dis.close();
		bos.close();
		index += 8;
		isChanged = true;
	}

	public void writeFloat(float v) throws IOException {
		increaseBuffer(4);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(4);
		DataOutputStream dis = new DataOutputStream(bos);
		dis.writeFloat(v);
		System.arraycopy(bos.toByteArray(), 0, buf, index, 4);
		dis.close();
		bos.close();
		index += 4;
		isChanged = true;
	}

	public void write(byte[] byteBuf) throws IOException {
		increaseBuffer(byteBuf.length);
		System.arraycopy(byteBuf, 0, buf, index, byteBuf.length);
		index += byteBuf.length;
		isChanged = true;
	}

	void increaseBuffer(int nBytes) {
		int d = buf.length - index; // example: size = 10 index = 10 >> d = 0
		int increment = nBytes - d; // example: nBytes = 4 >> increment = 4
		if (increment > 0) {
			// byte[] oldBuf = buf;
			// buf = new byte[size + SIZE_INCREMENT];
			int sizeInc = size / 2;
			int doIncrement = Math.max(sizeInc, increment);
			buf = Arrays.copyOf(buf, buf.length + doIncrement);
			System.out.print("!" + (buf.length / 1024 / 1024) + "mb");
			System.gc();
			openFileSize += doIncrement;
		}

		d = size - index; // example: size = 10 index = 10 >> d = 0
		increment = nBytes - d; // example: nBytes = 4 >> increment = 4
		if (increment > 0)
			size += increment;
	}

	public void skipBytes(int nb) {
		index += nb;
	}

	public long length() throws IOException { return size; }

}
