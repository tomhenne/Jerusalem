package de.esymetric.jerusalem.utils

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

class BufferedRandomAccessFileCache {
    private var maxSize = MAX_SIZE
    fun setMaxCacheSize(s: Int) {
        maxSize = s
    }

    var queue: Queue<String> = LinkedList()
    var cache: MutableMap<String, BufferedRandomAccessFile?> = HashMap()
    var numberOfAccesses = 0
    var numberOfFileChanges = 0
    fun getRandomAccessFile(filePath: String, readOnly: Boolean): BufferedRandomAccessFile? {
        numberOfAccesses++

        // get it from cache?
        if (cache.containsKey(filePath)) {
            queue.remove(filePath)
            queue.add(filePath) // insert at end of queue
            return cache[filePath]
        }

        // not in queue
        if (readOnly && !File(filePath).exists()) return null

        // remove first element?
        var raf: BufferedRandomAccessFile?
        raf = if (queue.size >= maxSize) {
            val fp = queue.poll()
            cache.remove(fp)
            // do not close here - file is closed in "open" method

            // now reuse the BufferedRandomAccessFile
        } else BufferedRandomAccessFile()
        try {
            if (!readOnly) File(filePath).parentFile.mkdirs()
            raf!!.open(filePath, if (readOnly) "r" else "rw")
            numberOfFileChanges++
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return null
        }
        cache[filePath] = raf
        queue.add(filePath)
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
            raf!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        queue.clear()
        cache.clear()
    }

    companion object {
        const val MAX_SIZE = 3
    }
}