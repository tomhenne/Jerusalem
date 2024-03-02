package de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.longOsm2OwnMaps

import java.io.File
import java.util.*

class LongOsmNodeID2OwnIDMapFileCache(var dataDirectoryPath: String, maxCacheSize: Int) {
    private var maxSize = 1 // server: 16  pc: 2
    private var queue: Queue<Int> = LinkedList()
    private var cache: MutableMap<Int, LongOsmNodeID2OwnIDMapFile> = HashMap()
    private var numberOfAccesses = 0
    private var numberOfFileChanges = 0

    init {
        maxSize = maxCacheSize
        val dirPath = dataDirectoryPath + File.separatorChar + LongOsmNodeID2OwnIDMapFile.Companion.DIR
        File(dirPath).mkdir()
    }

    fun put(osmNodeID: Long, ownNodeID: Int): Boolean {
        val f1 = getMap(osmNodeID, false)
        return f1!!.put(osmNodeID, ownNodeID)
    }

    operator fun get(osmNodeID: Long): Int {
        val f1 = getMap(osmNodeID, true)
        return f1!![osmNodeID]
    }

    val estimatedMemoryUsedMB: Int
        get() = (cache.size.toLong() * LongOsmNodeID2OwnIDMapFile.Companion.FILE_SIZE.toLong() / 1024L / 1024L).toInt()

    private fun getMap(osmNodeID: Long, readOnly: Boolean): LongOsmNodeID2OwnIDMapFile? {
        numberOfAccesses++
        val fileNumber: Int = LongOsmNodeID2OwnIDMapFile.Companion.getFileNumber(osmNodeID)

        // get it from cache?
        if (cache.containsKey(fileNumber)) {
            queue.remove(fileNumber)
            queue.add(fileNumber) // insert at end of queue
            return cache[fileNumber]
        }

        // not in queue

        // remove first element?
        var raf: LongOsmNodeID2OwnIDMapFile? = null
        if (queue.size >= maxSize) {
            val fn = queue.poll()
            raf = cache.remove(fn)
        }
        if (raf == null) raf = LongOsmNodeID2OwnIDMapFile(dataDirectoryPath)
        raf.openFile(fileNumber)
        numberOfFileChanges++
        cache[fileNumber] = raf
        queue.add(fileNumber)
        return raf
    }

    val andClearNumberOfFileChanges: Int
        get() {
            val n = numberOfFileChanges
            numberOfFileChanges = 0
            return n
        }

    fun close() {
        for (raf in cache.values) try {
            raf.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        queue.clear()
        cache.clear()
    }
}