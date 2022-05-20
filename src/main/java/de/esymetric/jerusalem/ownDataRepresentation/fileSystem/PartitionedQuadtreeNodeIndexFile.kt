package de.esymetric.jerusalem.ownDataRepresentation.fileSystem

import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.NodeIndexFile.NodeIndexNodeDescriptor
import de.esymetric.jerusalem.rebuilding.Rebuilder
import de.esymetric.jerusalem.utils.BufferedRandomAccessFile
import de.esymetric.jerusalem.utils.BufferedRandomAccessFileCache
import de.esymetric.jerusalem.utils.Utils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

class PartitionedQuadtreeNodeIndexFile(
    var dataDirectoryPath: String,
    readOnly: Boolean, startNewFile: Boolean
) : NodeIndexFile {
    var rafIndexCache = BufferedRandomAccessFileCache()
    var rafListCache = BufferedRandomAccessFileCache()
    var readOnly = false
    var numberOfSentences = 0
    var emptySentence = ByteArray(40)
    var writeCount = 0
    var writeCacheHits = 0
    var readCount = 0
    var readCacheHits = 0
    override val writeCacheHitRatio: Float
        get() {
            if (writeCount == 0) return 0f
            val f = 100.0f * writeCacheHits / writeCount
            writeCacheHits = 0
            writeCount = 0
            return f
        }
    override val readCacheHitRatio: Float
        get() {
            if (readCount == 0) return 0f
            val f = 100.0f * readCacheHits / readCount
            readCacheHits = 0
            readCount = 0
            return f
        }

    fun getIndexFilePath(lat: Double, lng: Double): String {
        val lld = LatLonDir(lat, lng)
        return (lld.getDir(dataDirectoryPath) + File.separatorChar
                + INDEX_FILENAME)
    }

    fun getListFilePath(lat: Double, lng: Double): String {
        val lld = LatLonDir(lat, lng)
        return (lld.getDir(dataDirectoryPath) + File.separatorChar
                + LIST_FILENAME)
    }

    /* cache.size(); */
    override val cacheSize: Int
        get() = 0 /* cache.size(); */
    var lastIndexFilePath: String? = null
    var lastIndexRaf: BufferedRandomAccessFile? = null
    fun getIndexRaf(lat: Double, lng: Double): BufferedRandomAccessFile? {
        val filePath = getIndexFilePath(lat, lng)
        return if (filePath == lastIndexFilePath) lastIndexRaf else try {
            val raf = rafIndexCache.getRandomAccessFile(
                filePath, readOnly
            )
            if (raf != null) {
                lastIndexFilePath = filePath
                lastIndexRaf = raf
                numberOfSentences = raf.size / SENTENCE_LENGTH.toInt()
                if (numberOfSentences == 0) insertNewSentence(raf)
            }
            raf
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    var lastListFilePath: String? = null
    var lastListRaf: BufferedRandomAccessFile? = null

    init {
        this.readOnly = readOnly
        if (readOnly) {
            rafIndexCache.setMaxCacheSize(8)
            rafListCache.setMaxCacheSize(8)
        }
        for (i in 0..39) emptySentence[i] = -1
    }

    fun getListRaf(lat: Double, lng: Double): BufferedRandomAccessFile? {
        val filePath = getListFilePath(lat, lng)
        return if (filePath == lastListFilePath) lastListRaf else try {
            val raf = rafListCache.getRandomAccessFile(
                filePath, readOnly
            )
            if (raf != null) {
                lastListFilePath = filePath
                lastListRaf = raf
            }
            raf
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun close() {
        rafIndexCache.close()
        rafListCache.close()
    }

    override fun getID(lat: Double, lng: Double): Int {
        val raf = getIndexRaf(lat, lng) ?: return -1 // rafCache.getRandomAccessFile(getFilePath(lat,
        // lng),
        // readOnly);
        val latInt = ((lat + LAT_OFFS) * USED_DIGITS_MULT).toInt()
        val lngInt = ((lng + LNG_OFFS) * USED_DIGITS_MULT).toInt()
        return try {
            getID(latInt, lngInt, raf)
        } catch (e: IOException) {
            e.printStackTrace()
            -1
        }
    }

    fun getID(latInt: Int, lngInt: Int): Int {
        val lat = latInt / USED_DIGITS_MULT - LAT_OFFS.toInt()
        val lng = lngInt / USED_DIGITS_MULT - LNG_OFFS.toInt()
        val raf =
            getIndexRaf(lat.toDouble(), lng.toDouble()) ?: return -1 // rafCache.getRandomAccessFile(getFilePath(lat,
        // lng),
        // readOnly);
        return try {
            getID(latInt, lngInt, raf)
        } catch (e: IOException) {
            e.printStackTrace()
            -1
        }
    }

    @Throws(IOException::class)
    fun getID(latInt: Int, lngInt: Int, raf: BufferedRandomAccessFile): Int {
        if (numberOfSentences == 0) return -1
        if (latInt < 0 || latInt > MAX_LAT_INT) return -1
        if (lngInt < 0 || lngInt > MAX_LNG_INT) return -1
        readCount++
        var id = 0
        val keyChain = getKeyChain(latInt, lngInt)
        var i = 0
        while (i < NUMBER_OF_USED_DIGITS_MULT_2) {
            raf.seek(id.toLong() * SENTENCE_LENGTH + keyChain[i].toLong() * 4L)
            id = raf.readInt()
            if (id == -1) break
            i++
        }
        return id // the last id is the id referring to a node
    }

    fun getKeyChain(latInt: Int, lngInt: Int): IntArray {
        var latInt = latInt
        var lngInt = lngInt
        val keyChain = IntArray(NUMBER_OF_USED_DIGITS_MULT_2)
        var i = NUMBER_OF_USED_DIGITS_MULT_2 - 2
        while (i >= 0) {
            keyChain[i] = latInt % 10
            keyChain[i + 1] = lngInt % 10
            latInt /= 10
            lngInt /= 10
            i -= 2
        }
        return keyChain
    }

    fun makeShortCacheKey(keyChain: IntArray): Int {
        var key = 0
        for (i in 0 until CACHE_KEY_LENGTH) {
            key += keyChain[i]
            key *= 10
        }
        return key
    }

    override fun getIDPlusSourroundingIDs(
        lat: Double,
        lng: Double, radius: Int
    ): List<NodeIndexNodeDescriptor> {
        val list: MutableList<NodeIndexNodeDescriptor> = ArrayList()
        val latInt = ((lat + LAT_OFFS) * USED_DIGITS_MULT).toInt()
        val lngInt = ((lng + LNG_OFFS) * USED_DIGITS_MULT).toInt()
        try {
            val rSquare = radius * radius
            for (x in latInt - radius..latInt + radius) for (y in lngInt - radius..lngInt + radius) {
                val deltaX = x - latInt
                val deltaY = y - lngInt
                if (deltaX * deltaX + deltaY * deltaY <= rSquare) {
                    val idInListFile = getID(x, y)
                    if (idInListFile == -1) continue
                    val nodes = getAllNodeIDsFromQuadtreeList(
                        lat, lng, idInListFile
                    )
                    for (nodeID in nodes) {
                        val nind = NodeIndexNodeDescriptor()
                        nind.id = nodeID
                        nind.latInt = x / USED_DIGITS_MULT - LAT_OFFS.toInt() // not
                        // elegant
                        nind.lngInt = y / USED_DIGITS_MULT - LNG_OFFS.toInt()
                        list.add(nind)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getAllNodeIDsFromQuadtreeList(lat: Double, lng: Double, id: Int): List<Int> {
        var id = id
        val l: MutableList<Int> = LinkedList()
        val rafList = getListRaf(lat, lng) ?: return l
        try {
            while (true) {
                rafList.seek(id * NODELIST_SENTENCE_LENGTH)
                var nodeID: Int
                nodeID = rafList.readInt()
                val nextID = rafList.readInt()
                l.add(nodeID)
                if (nextID == -1) break
                id = nextID
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return l
    }

    override fun setID(lat: Double, lng: Double, id: Int): Int {
        val raf = getIndexRaf(lat, lng) // rafCache.getRandomAccessFile(getFilePath(lat,
        // lng),
        // readOnly);
        val latInt = ((lat + LAT_OFFS) * USED_DIGITS_MULT).toInt()
        val lngInt = ((lng + LNG_OFFS) * USED_DIGITS_MULT).toInt()
        return try {
            setID(latInt, lngInt, id, raf)
        } catch (e: IOException) {
            e.printStackTrace()
            -1
        }
    }

    @Throws(IOException::class)
    fun setID(latInt: Int, lngInt: Int, nodeID: Int, raf: BufferedRandomAccessFile?): Int {
        if (latInt < 0 || latInt > MAX_LAT_INT) return -1
        if (lngInt < 0 || lngInt > MAX_LNG_INT) return -1
        writeCount++
        var id = 0
        val keyChain = getKeyChain(latInt, lngInt)
        var i = 0

        // read as long as leafs exist
        while (i < NUMBER_OF_USED_DIGITS_MULT_2 - 1) {
            val pos = id.toLong() * SENTENCE_LENGTH + keyChain[i].toLong() * 4L
            if (!raf!!.seek(pos)) break
            val foundID = raf.readInt()
            if (foundID == -1) break
            id = foundID
            i++
        }

        // add new leafs
        while (i < NUMBER_OF_USED_DIGITS_MULT_2 - 1) {
            val newID = insertNewSentence(raf)
            if (newID == -1) {
                println("ERROR: cannot create new sentence in QuadtreeNodeIndexFile")
                return -1
            }
            val pos = id.toLong() * SENTENCE_LENGTH + keyChain[i].toLong() * 4L
            raf!!.seek(pos)
            raf.writeInt(newID)
            id = newID
            i++
        }

        // insert nodeID on the last leaf
        val pos = id.toLong() * SENTENCE_LENGTH + keyChain[i].toLong() * 4L
        raf!!.seek(pos)
        val oldID = raf.readInt()
        raf.seek(pos)
        raf.writeInt(nodeID)
        return oldID
    }

    fun insertNewSentence(raf: BufferedRandomAccessFile?): Int {
        val fileLength = numberOfSentences.toLong() * SENTENCE_LENGTH
        return try {
            raf!!.seek(fileLength)
            raf.write(emptySentence)
            val id = numberOfSentences
            numberOfSentences++
            id
        } catch (e: IOException) {
            e.printStackTrace()
            -1
        }
    }

    override val capacity: Int
        get() = 10 * numberOfSentences

    fun makeQuadtreeIndex(startTime: Date, nlf: PartitionedNodeListFile) {
        val files = File(dataDirectoryPath).listFiles()
        Arrays.sort(files)
        for (f in files) if (f.isDirectory && f.name.startsWith("lat_")) {
            for (g in f.listFiles()) if (g != null && g.isDirectory
                && g.name.startsWith("lng_")
            ) {
                val list = g.listFiles()
                if (list == null) {
                    println(
                        "Cannot list files in "
                                + g.path
                    )
                    continue
                }
                for (h in list) if (h != null && h.isFile
                    && h.name == PartitionedNodeListFile.Companion.FILE_NAME
                ) {
                    val dirLatInt: Int = f.name
                        .replace("lat_", "").toInt() - LatLonDir.Companion.LAT_OFFS.toInt()
                    val dirLngInt: Int = g.name
                        .replace("lng_", "").toInt() - LatLonDir.Companion.LNG_OFFS.toInt()
                    val quadtreeIndexFilePath = (g.path
                            + File.separatorChar
                            + INDEX_FILENAME)
                    val quadtreeListFilePath = (g.path
                            + File.separatorChar
                            + LIST_FILENAME)
                    File(quadtreeIndexFilePath).delete()
                    File(quadtreeListFilePath).delete()
                    val rafIndex = BufferedRandomAccessFile()
                    val rafList = BufferedRandomAccessFile()
                    try {
                        rafIndex.open(quadtreeIndexFilePath, "rw")
                        rafList.open(quadtreeListFilePath, "rw")
                    } catch (e1: FileNotFoundException) {
                        e1.printStackTrace()
                        continue
                    }
                    numberOfSentences = 0
                    insertNewSentence(rafIndex)
                    val nodes = nlf.getAllNodesInFile(
                        h.path
                    )
                    println(
                        "\n"
                                + Utils.formatTimeStopWatch(
                            Date()
                                .time
                                    - startTime.time
                        )
                                + " building quadtree lat=" + dirLatInt
                                + " lng=" + dirLngInt + " with "
                                + nodes.size + " nodes"
                    )
                    var idInListFile = 0
                    for (n in nodes) {
                        try {
                            val latInt = ((n.lat + LAT_OFFS) * USED_DIGITS_MULT).toInt()
                            val lngInt = ((n.lng + LNG_OFFS) * USED_DIGITS_MULT).toInt()
                            val foundID = setID(
                                latInt, lngInt,
                                idInListFile, rafIndex
                            )

                            // TEST REMOVE
                            /*
										 * int testID = getID(latInt, lngInt,
										 * rafIndex); if( testID != idInListFile
										 * ) System.out.println("error");
										 */
                            idInListFile++
                            rafList.writeInt(n.id.toInt())
                            rafList.writeInt(foundID)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    try {
                        rafIndex.close()
                        rafList.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            Rebuilder.cleanMem(startTime)
        }
        println(
            Utils.formatTimeStopWatch(
                Date().time
                        - startTime.time
            )
                    + " finished building quadtree"
        )
    }

    override val maxCacheSize = 32000 // number of entries

    companion object {
        const val NUMBER_OF_USED_DIGITS = 6 // if you change this, also

        // change USED_DIGITS_MULT and
        // MAX_SEARCH_RADIUS in
        // NearestNodeFinder
        const val NUMBER_OF_USED_DIGITS_MULT_2 = NUMBER_OF_USED_DIGITS * 2
        const val NUMBER_OF_USED_DIGITS_BEHIND_COMMA = NUMBER_OF_USED_DIGITS - 3
        const val USED_DIGITS_MULT = 1000 // 4 digits
        const val LAT_OFFS = 90.0
        const val LNG_OFFS = 180.0
        const val SENTENCE_LENGTH = 40L
        const val NODELIST_SENTENCE_LENGTH = 8L // int nodeID, int next
        const val MAX_LAT_INT = 180 * USED_DIGITS_MULT
        const val MAX_LNG_INT = 360 * USED_DIGITS_MULT
        const val INDEX_FILENAME = "quadtreeIndex.data"
        const val LIST_FILENAME = "quadtreeNodeList.data"
        const val CACHE_KEY_LENGTH = 9

    }
}