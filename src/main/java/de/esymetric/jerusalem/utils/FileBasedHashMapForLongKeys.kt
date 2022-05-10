package de.esymetric.jerusalem.utils

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.IOException

class FileBasedHashMapForLongKeys {
    // nextIndex
    var raf: BufferedRandomAccessFile? = null
    var rafCache = BufferedRandomAccessFileCache()
    var isEmpty = false
    var fileLength: Long = 0
    var countGets = 0
    var countGetReads = 0
    val avgGetAccessNumberOfReads: Float
        get() {
            val r = countGetReads.toFloat() / countGets.toFloat()
            countGetReads = 0
            countGets = 0
            return r
        }
    val andClearNumberOfFileChanges: Int
        get() = rafCache.andClearNumberOfFileChanges

    fun open(filePath: String?, readOnly: Boolean) {
        isEmpty = false
        fileLength = 0L
        if (readOnly && !File(filePath).exists()) {
            isEmpty = true
            return
        }
        try {
            raf = rafCache.getRandomAccessFile(filePath!!, readOnly)
            if (raf!!.length() < SENTENCE_LENGTH.toLong() * HASH_ARRAY_SIZE.toLong()) {
                val bufSize = HASH_ARRAY_SIZE // other sizes could be chosen
                val buf = ByteArray(bufSize)
                for (i in 0 until HASH_ARRAY_SIZE / bufSize * SENTENCE_LENGTH) raf?.write(buf)
                print("$")
            }
            fileLength = raf!!.length()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun close() {
        rafCache.close()
    }

    fun put(key: Long, value: Int) {
        try {
            val arrayIndex = (key and HASH_ARRAY_MASK).toInt()
            val strippedKey = (key shr HASH_ARRAY_NUMBER_OF_BITS).toInt() + 1
            val pos = arrayIndex.toLong() * SENTENCE_LENGTH.toLong()
            raf!!.seek(pos)
            val readKey = raf!!.readInt()
            raf!!.readInt()
            val readNextIndex = raf!!.readInt()
            if (readKey == 0) {
                raf!!.seek(pos)
                raf!!.writeInt(strippedKey)
                raf!!.writeInt(value)
            } else {
                val newIndex = (fileLength / SENTENCE_LENGTH.toLong()).toInt()
                raf!!.seek(fileLength)
                raf!!.writeInt(strippedKey)
                raf!!.writeInt(value)
                raf!!.writeInt(readNextIndex)
                fileLength += SENTENCE_LENGTH.toLong()
                raf!!.seek(pos + 8L)
                raf!!.writeInt(newIndex)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    operator fun get(key: Long): Int {
        if (isEmpty) return -1
        try {
            val arrayIndex = (key and HASH_ARRAY_MASK).toInt()
            val strippedKey = (key shr HASH_ARRAY_NUMBER_OF_BITS).toInt() + 1
            var pos = arrayIndex.toLong() * SENTENCE_LENGTH.toLong()
            countGets++
            countGetReads++
            val buf = ByteArray(SENTENCE_LENGTH)
            while (true) {
                raf!!.seek(pos)
                raf!!.read(buf)
                val bais = ByteArrayInputStream(buf)
                val dis = DataInputStream(bais)
                val foundKey = dis.readInt()
                val foundValue = dis.readInt()
                val nextIndex = dis.readInt()
                dis.close()
                bais.close()
                if (foundKey == strippedKey) return foundValue
                pos = if (nextIndex <= 0L) return -1 else {
                    nextIndex.toLong() * SENTENCE_LENGTH.toLong()
                }
                countGetReads++
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return -1
        }
    }

    companion object {
        const val HASH_ARRAY_NUMBER_OF_BITS = 20
        const val HASH_ARRAY_SIZE = 1 shl HASH_ARRAY_NUMBER_OF_BITS // 1048576 = 1M
        const val HASH_ARRAY_MASK = (HASH_ARRAY_SIZE - 1).toLong()
        const val SENTENCE_LENGTH = 12 // 12 bytes: int stripped-key, int value, int
    }
}