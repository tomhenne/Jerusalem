package de.esymetric.jerusalem.utils

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.util.*

class RandomAccessFileCache {
    var queue: Queue<String> = LinkedList()
    var cache: MutableMap<String, RandomAccessFile> = HashMap()
    var numberOfAccesses = 0
    var numberOfFileChanges = 0
    fun getRandomAccessFile(
        filePath: String,
        readOnly: Boolean
    ): RandomAccessFile? {
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
        if (queue.size >= MAX_SIZE) {
            val fp = queue.poll()
            val raf = cache.remove(fp)
            try {
                raf!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        var raf: RandomAccessFile? = null
        try {
            raf = RandomAccessFile(filePath, if (readOnly) "r" else "rw")
            numberOfFileChanges++
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            println(
                "FAILURE to open RandomAccessFile, file cache size: "
                        + cache.size
            )
        }
        if (raf == null) {
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
            }
            File(filePath).delete()
            try {
                raf = RandomAccessFile(filePath, if (readOnly) "r" else "rw")
                numberOfFileChanges++
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                println(
                    "FAILURE(2) to open RandomAccessFile, file cache size: "
                            + cache.size
                )
                return null
            }
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
            raf.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        queue.clear()
        cache.clear()
    }

    companion object {
        const val MAX_SIZE = 200 // was 256
    }
}