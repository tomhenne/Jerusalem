package de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.longOsm2OwnMaps

import java.io.*
import java.nio.ByteBuffer

class LongOsmNodeID2OwnIDMapFile(var dataDirectoryPath: String) : LongOsmNodeID2OwnIDMap {
    var currentFileNumber = -1
    var currentFilePath: String? = null
    var currentFileIsModified = false
    var entries = IntArray(NUMBER_OF_ENTRIES_PER_FILE) { i -> -1 }
    var buf: ByteBuffer? = null

    override val numberOfUsedArrays: Int
        get() = 0

    fun isCurrentlyOpenFile(osmNodeID: Long): Boolean {
        return isCurrentlyOpenFile(getFileNumber(osmNodeID))
    }

    fun isCurrentlyOpenFile(fileNumber: Int): Boolean {
        return fileNumber == currentFileNumber
    }

    fun openFile(fileNumber: Int): Boolean {
        if (isCurrentlyOpenFile(fileNumber)) return true
        if (currentFileNumber != -1) closeFile()

        // load file
        currentFileNumber = fileNumber
        currentFilePath = getMapFilePath(fileNumber)
        val f = File(currentFilePath)
        if (f.exists()) {
            print("$$fileNumber")
            return readIntegers()
        }

        // need to clear buffer?
        print("*$fileNumber")
        return true
    }

    fun closeFile(): Boolean {
        if (currentFileNumber == -1) return true
        if (!currentFileIsModified) return true
        val success = writeIntegers()
        print("+$currentFileNumber")
        currentFileIsModified = false
        System.gc()
        return success
    }

    private fun writeIntegers(): Boolean {
        if (buf == null) buf = ByteBuffer.allocate(4 * NUMBER_OF_ENTRIES_PER_FILE) else buf!!.clear()
        for (i in entries) buf!!.putInt(i)
        buf!!.rewind()
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(currentFilePath)
            val file = out.channel
            file.write(buf)
            file.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } finally {
            safeClose(out)
            buf = null
        }
        return true
    }

    private fun readIntegers(): Boolean {
        var `in`: FileInputStream? = null
        try {
            `in` = FileInputStream(currentFilePath)
            val file = `in`.channel
            if (buf == null) buf = ByteBuffer.allocate(4 * NUMBER_OF_ENTRIES_PER_FILE) else buf!!.clear()
            file.read(buf)
            buf!!.rewind()
            for (i in entries.indices) {
                entries[i] = buf!!.int
            }
            file.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } finally {
            safeClose(`in`)
        }
        return true
    }

    override fun put(osmNodeID: Long, ownNodeID: Int): Boolean {
        val fileNumber = getFileNumber(osmNodeID)
        if (!openFile(fileNumber)) return false
        val indexInFile = getIndexInFile(osmNodeID, fileNumber)
        entries[indexInFile] = ownNodeID
        currentFileIsModified = true
        return true
    }

    private fun getMapFilePath(fileNumber: Int): String {
        return dataDirectoryPath + File.separatorChar + DIR + File.separatorChar + FILENAME + fileNumber + ".data"
    }

    override fun get(osmNodeID: Long): Int {
        val fileNumber = getFileNumber(osmNodeID)
        if (!openFile(fileNumber)) return -1
        val indexInFile = getIndexInFile(osmNodeID, fileNumber)
        return entries[indexInFile]
    }

    override fun close() {
        closeFile()
        if (buf != null) {
            buf!!.clear()
            buf = null
        }
    }

    override fun delete() {}

    companion object {
        const val DIR = "longOsm2OwnMap"
        const val FILENAME = "map_"
        const val NUMBER_OF_ENTRIES_PER_FILE = 50000000 // 50 Mio Speicherbedarf 200 MB
        const val FILE_SIZE = NUMBER_OF_ENTRIES_PER_FILE * 4
        fun getFileNumber(osmNodeID: Long): Int {
            return (osmNodeID / NUMBER_OF_ENTRIES_PER_FILE.toLong()).toInt()
        }

        fun getIndexInFile(osmNodeID: Long, fileNumber: Int): Int {
            return (osmNodeID - fileNumber.toLong() * NUMBER_OF_ENTRIES_PER_FILE.toLong()).toInt()
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