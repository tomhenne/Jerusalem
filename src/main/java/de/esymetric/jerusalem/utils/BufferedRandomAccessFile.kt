package de.esymetric.jerusalem.utils

import java.io.*
import java.nio.ByteBuffer
import java.util.*

class BufferedRandomAccessFile {
    var buf: ByteArray? = null
    var index = 0
    var size = 0
    var filePath: String? = null
    var isChanged = false
    @Throws(FileNotFoundException::class)
    fun open(filePath: String, mode: String) {
        if (this.filePath != null) {
            if (buf != null && this.filePath == filePath) return
            try {
                close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        this.filePath = filePath
        index = 0
        size = 0
        if (File(filePath).exists()) {
            size = File(filePath).length().toInt()
            val newBufSize = size + if ("r" == mode) 0 else SIZE_INCREMENT
            if (buf == null || buf!!.size < newBufSize) {
                if (buf != null) openFileSize -= buf!!.size.toLong()
                buf = ByteArray(newBufSize)
                openFileSize += newBufSize.toLong()
            }
            if (!readFromFile(buf, filePath)) {
                println("ERROR cannot read from file $filePath")
            }
        } else {
            if (buf == null || buf!!.size < INITIAL_SIZE) {
                if (buf != null) openFileSize -= buf!!.size.toLong()
                openFileSize += INITIAL_SIZE.toLong()
                buf = ByteArray(INITIAL_SIZE)
            }
        }
        openFileCount++
    }

    @Throws(IOException::class)
    fun close() {
        if (isChanged) if (!writeToFile(buf, size, filePath)) println("ERROR cannot write to file $filePath")
        isChanged = false
        filePath = null
        if (buf != null) {
            openFileSize -= buf!!.size.toLong()
            buf = null
            openFileCount--
        }
    }

    @Throws(IOException::class)
    fun readShort(): Short {
        if (index > buf!!.size - 2) {
            println("Out of bounds!!")
        }
        if (index > size - 2) {
            println("Out of bounds also!!")
        }
        val dis = DataInputStream(
            ByteArrayInputStream(
                buf,
                index, 2
            )
        )
        val r = dis.readShort()
        dis.close()
        index += 2
        return r
    }

    @Throws(IOException::class)
    fun readInt(): Int {
        if (index > buf!!.size - 4) {
            println("Out of bounds!!")
        }
        if (index > size - 4) {
            println("Out of bounds also!!")
        }
        val dis = DataInputStream(
            ByteArrayInputStream(
                buf,
                index, 4
            )
        )
        val r = dis.readInt()
        dis.close()
        index += 4
        return r
    }

    @Throws(IOException::class)
    fun readLong(): Long {
        val dis = DataInputStream(
            ByteArrayInputStream(
                buf,
                index, 8
            )
        )
        val r = dis.readLong()
        dis.close()
        index += 8
        return r
    }

    @Throws(IOException::class)
    fun readFloat(): Float {
        val dis = DataInputStream(
            ByteArrayInputStream(
                buf,
                index, 4
            )
        )
        val r = dis.readFloat()
        dis.close()
        index += 4
        return r
    }

    @Throws(IOException::class)
    fun readUShort(): UShort {
        val bytes = buf!!.copyOfRange(index, index + 2)
        index += 2
        return toUShort(bytes)
    }

    @Throws(IOException::class)
    fun read(buffer: ByteArray) {
        // not tested
        System.arraycopy(buf, index, buffer, 0, buffer.size)
        index += buffer.size
    }

    fun seek(i: Int): Boolean {
        if (i < 0) println("negative seek")
        index = i
        return i < size // not eof
    }

    fun seek(i: Long): Boolean {
        return seek(i.toInt())
    }

    @Throws(IOException::class)
    fun writeInt(v: Int) {
        increaseBuffer(4)
        val bos = ByteArrayOutputStream(4)
        val dis = DataOutputStream(bos)
        dis.writeInt(v)
        System.arraycopy(bos.toByteArray(), 0, buf, index, 4)
        dis.close()
        bos.close()
        index += 4
        isChanged = true
    }

    @Throws(IOException::class)
    fun writeLong(v: Int) {
        increaseBuffer(8)
        val bos = ByteArrayOutputStream(4)
        val dis = DataOutputStream(bos)
        dis.writeLong(v.toLong())
        System.arraycopy(bos.toByteArray(), 0, buf, index, 8)
        dis.close()
        bos.close()
        index += 8
        isChanged = true
    }

    @Throws(IOException::class)
    fun writeFloat(v: Float) {
        increaseBuffer(4)
        val bos = ByteArrayOutputStream(4)
        val dis = DataOutputStream(bos)
        dis.writeFloat(v)
        System.arraycopy(bos.toByteArray(), 0, buf, index, 4)
        dis.close()
        bos.close()
        index += 4
        isChanged = true
    }

    private fun toBytes(s: UShort): ByteArray {
        return byteArrayOf((s.toInt() and 0x00FF).toByte(), ((s.toInt() and 0xFF00) shr (8)).toByte())
    }

    private fun toUShort(a: ByteArray): UShort {
        return ((a[0].toUInt() and 0xff.toUInt()) or (a[1].toUInt() shl (8))).toUShort()
    }

    @Throws(IOException::class)
    fun writeUShort(v: UShort) {
        increaseBuffer(2)
        val bos = ByteArrayOutputStream(2)
        val bytes = toBytes(v)
        System.arraycopy(bytes, 0, buf, index, 2)
        bos.close()
        index += 2
        isChanged = true
    }

    @Throws(IOException::class)
    fun write(byteBuf: ByteArray) {
        increaseBuffer(byteBuf.size)
        System.arraycopy(byteBuf, 0, buf, index, byteBuf.size)
        index += byteBuf.size
        isChanged = true
    }

    fun increaseBuffer(nBytes: Int) {
        var d = buf!!.size - index // example: size = 10 index = 10 >> d = 0
        var increment = nBytes - d // example: nBytes = 4 >> increment = 4
        if (increment > 0) {
            // byte[] oldBuf = buf;
            // buf = new byte[size + SIZE_INCREMENT];
            val sizeInc = size / 2
            val doIncrement = Math.max(sizeInc, increment)
            buf = Arrays.copyOf(buf, buf!!.size + doIncrement)
            print("!" + buf!!.size / 1024 / 1024 + "mb")
            System.gc()
            openFileSize += doIncrement.toLong()
        }
        d = size - index // example: size = 10 index = 10 >> d = 0
        increment = nBytes - d // example: nBytes = 4 >> increment = 4
        if (increment > 0) size += increment
    }

    fun skipBytes(nb: Int) {
        index += nb
    }

    @Throws(IOException::class)
    fun length(): Long {
        return size.toLong()
    }

    companion object {
        const val SIZE_INCREMENT = 800000 // 0,8 MB
        const val INITIAL_SIZE = 1500000 // 1,5 MB
        private var readCount = 0
        private var writeCount = 0
        private var readFileSize: Long = 0
        private var writtenFileSize: Long = 0
        var openFileCount = 0
            private set
        var openFileSize: Long = 0
            private set
        @JvmStatic
		val shortInfoAndResetCounters: String
            get() = "r#" + andResetReadCount +
                    "/rfs" + andResetReadFileSize / 1024L / 1024L + "mb" +
                    "/w#" + andResetWriteCount +
                    "/wfs" + andResetWrittenFileSize / 1024L / 1024L + "mb" +
                    "/op#" + openFileCount +
                    "/fs" + openFileSize / 1024L / 1024L + "mb"
        val andResetReadCount: Int
            get() {
                val c = readCount
                readCount = 0
                return c
            }
        val andResetReadFileSize: Long
            get() {
                val c = readFileSize
                readFileSize = 0
                return c
            }
        val andResetWriteCount: Int
            get() {
                val c = writeCount
                writeCount = 0
                return c
            }
        val andResetWrittenFileSize: Long
            get() {
                val c = writtenFileSize
                writtenFileSize = 0
                return c
            }

        private fun readFromFile(data: ByteArray?, filePath: String): Boolean {
            if (data == null) return false
            var `in`: FileInputStream? = null
            try {
                `in` = FileInputStream(filePath)
                val file = `in`.channel
                val buf = ByteBuffer.wrap(data) // allocate(4 * NUMBER_OF_ENTRIES_PER_FILE);
                readFileSize += file.read(buf).toLong()
                file.close()
                //System.out.print('#');
                readCount++
            } catch (e: IOException) {
                e.printStackTrace()
                return false
            } finally {
                safeClose(`in`)
            }
            return true
        }

        private fun writeToFile(data: ByteArray?, size: Int, filePath: String?): Boolean {
            if (data == null) return false
            val buf = ByteBuffer.wrap(data, 0, size)
            buf.rewind()
            var out: FileOutputStream? = null
            try {
                out = FileOutputStream(filePath)
                val file = out.channel
                file.write(buf)
                file.close()
                //System.out.print('%');
                writeCount++
                writtenFileSize += size.toLong()
            } catch (e: IOException) {
                e.printStackTrace()
                return false
            } finally {
                safeClose(out)
            }
            return true
        }

        private fun safeClose(out: OutputStream?) {
            try {
                out?.close()
            } catch (e: IOException) {
                // do nothing
            }
        }

        private fun safeClose(out: InputStream?) {
            try {
                out?.close()
            } catch (e: IOException) {
                // do nothing
            }
        }
    }
}