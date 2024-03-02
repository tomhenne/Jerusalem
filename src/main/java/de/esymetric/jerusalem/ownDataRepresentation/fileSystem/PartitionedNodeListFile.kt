package de.esymetric.jerusalem.ownDataRepresentation.fileSystem

import de.esymetric.jerusalem.ownDataRepresentation.Node
import de.esymetric.jerusalem.utils.BufferedRandomAccessFile
import de.esymetric.jerusalem.utils.BufferedRandomAccessFileCache
import de.esymetric.jerusalem.utils.Utils
import java.io.*
import java.util.*

class PartitionedNodeListFile(var dataDirectoryPath: String, var readOnly: Boolean) {
    var filePath: String? = null
    var raf: BufferedRandomAccessFile? = null
    private var rafCache = BufferedRandomAccessFileCache()
    fun setMaxFileCacheSize(n: Int) {
        rafCache.setMaxCacheSize(n)
    }

    private var maxNodeID = 0
    private var currentLatLonDir = LatLonDir(-1000.0, -1000.0)
    private var fileChangesRecorded = 0
    fun getFileChanges(): Int {
        val fileChanges = fileChangesRecorded
        fileChangesRecorded = 0
        return fileChanges
    }

    val fileChangesWithNewFileCreation: Int
        get() = rafCache.andClearNumberOfFileChanges

    fun checkAndCreateRandomAccessFile(lat: Double, lng: Double): Boolean {
        val newLatLonDir = LatLonDir(lat, lng)
        return checkAndCreateRandomAccessFile(newLatLonDir)
    }

    fun checkAndCreateRandomAccessFile(newLatLonDir: LatLonDir): Boolean {
        if (newLatLonDir.equals(currentLatLonDir)) return true
        fileChangesRecorded++
        currentLatLonDir = newLatLonDir
        filePath = (currentLatLonDir.makeDir(dataDirectoryPath, !readOnly)
                + File.separatorChar + FILE_NAME)
        raf = rafCache.getRandomAccessFile(filePath!!, readOnly)
        if (raf == null) return false
        maxNodeID = try {
            (raf!!.length() / SENTENCE_LENGTH).toInt() - 1
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun close() {
        rafCache.close()
        if (appendNewNodeStream != null) {
            try {
                appendNewNodeStream!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        currentLatLonDir = LatLonDir(-1000.0, -1000.0)
    }

    fun getAllNodesInFile(filePath: String): List<Node> {
        val fs = File(filePath).length()
        val nr = (fs / SENTENCE_LENGTH).toInt()
        val arrayList: MutableList<Node> = ArrayList()
        var dis: DataInputStream? = null
        try {
            dis = DataInputStream(BufferedInputStream(FileInputStream(filePath), 32000))
            for (i in 0 until nr) {
                val n = Node()
                n.id = i.toLong()
                n.lat = dis.readFloat().toDouble()
                n.lng = dis.readFloat().toDouble()
                n.transitionID = dis.readInt()
                arrayList.add(n)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (dis != null) try {
                dis.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return arrayList
    }

    var appendNewNodeStream: DataOutputStream? = null
    var appendNewNodeStreamLatLonDir: LatLonDir? = null
    fun insertNewNodeStreamAppend(lat: Double, lng: Double): Int {
        val lld = LatLonDir(lat, lng)
        if (appendNewNodeStream == null ||
            !lld.equals(appendNewNodeStreamLatLonDir)
        ) {
            val filePath = (lld.makeDir(dataDirectoryPath, true)
                    + File.separatorChar + FILE_NAME)
            if (appendNewNodeStream != null) {
                try {
                    appendNewNodeStream!!.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            maxNodeID = (File(filePath).length() / SENTENCE_LENGTH).toInt() - 1
            appendNewNodeStream = try {
                DataOutputStream(BufferedOutputStream(FileOutputStream(filePath, true), 32000))
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                return -1
            }
            appendNewNodeStreamLatLonDir = lld
        }
        return try {
            appendNewNodeStream!!.writeFloat(lat.toFloat())
            appendNewNodeStream!!.writeFloat(lng.toFloat())
            appendNewNodeStream!!.writeInt(-1)
            maxNodeID++
            maxNodeID
        } catch (e: IOException) {
            e.printStackTrace()
            -1
        }
    }

    fun setTransitionID(lat: Double, lng: Double, id: Int, transitionID: Int) {
        if (!checkAndCreateRandomAccessFile(lat, lng)) return
        try {
            raf!!.seek(id.toLong() * SENTENCE_LENGTH + 8L)
            raf!!.writeInt(transitionID)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    var buf = ByteArray(SENTENCE_LENGTH.toInt())

    init {
        if (readOnly) rafCache.setMaxCacheSize(30)
    }

    fun getNode(latLonDir: LatLonDir, id: Int): Node? {
        return getNode(Node(), latLonDir, id)
    }

    fun getNode(n: Node, latLonDir: LatLonDir, id: Int): Node? {
        checkAndCreateRandomAccessFile(latLonDir)
        if (raf == null) return null
            return if (id < 0 || id > maxNodeID) {
                println("Cannot load node $id in ${latLonDir.getDir("")}")
                null
            } else try {
            n.id = id.toLong()
            raf!!.seek(id.toLong() * SENTENCE_LENGTH)
            raf!!.read(buf)
            val bais = ByteArrayInputStream(buf)
            val dis = DataInputStream(bais)
            n.lat = dis.readFloat().toDouble()
            n.lng = dis.readFloat().toDouble()
            n.transitionID = dis.readInt()
            n.setLatLonDirKey(latLonDir.shortKey) // for sorting
            dis.close()
            bais.close()
            n
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun resetTransitionIDsInAllFilesBuffered(startTime: Date) {
        var count = 1
        val files = File(dataDirectoryPath).listFiles()
        Arrays.sort(files)
        for (f in files) if (f.isDirectory && f.name.startsWith("lat_")) {
            println(
                Utils.formatTimeStopWatch(
                    Date()
                        .time
                            - startTime.time
                )
                        + "  >>> " + f.name + " # " + count++
            )
            for (g in f.listFiles()) if (g != null && g.isDirectory
                && g.name.startsWith("lng_")
            ) {
                val list = g.listFiles()
                if (list == null) {
                    println("Cannot list files in " + g.path)
                    continue
                }
                for (h in list) if (h != null && h.isFile
                    && h.name == FILE_NAME
                ) {
                    try {
                        val raf = BufferedRandomAccessFile()
                        raf.open(
                            h.path, "rw"
                        )
                        val l = (raf.length() / SENTENCE_LENGTH).toInt()
                        for (i in 0 until l) {
                            raf.seek(i.toLong() * SENTENCE_LENGTH + 8L)
                            raf.writeInt(-1)
                        }
                        raf.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    companion object {
        const val SENTENCE_LENGTH = 12L
        const val FILE_NAME = "nodes.data"
    }
}