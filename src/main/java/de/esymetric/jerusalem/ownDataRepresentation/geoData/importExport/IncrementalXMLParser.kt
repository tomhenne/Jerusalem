package de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport

import nanoxml.XMLElement
import java.io.IOException
import java.io.LineNumberReader
import java.lang.StringBuilder
import kotlin.Throws

class IncrementalXMLParser(var lnr: LineNumberReader) {
    var buffer = StringBuilder()
    @Throws(IOException::class)
    fun readElement(tagName: String, maxChars: Int): XMLElement? {
        val p1 = buffer.indexOf("<$tagName")
        if (p1 < 0) {
            if (buffer.length > maxChars) return null
            return if (!readToBuffer()) null else readElement(tagName, maxChars)
        }
        val p2 = buffer.indexOf("</$tagName", p1 + tagName.length)
        if (p2 < 0) {
            if (buffer.length > maxChars) return null
            return if (!readToBuffer()) null else readElement(tagName, maxChars)
        }
        val p3 = buffer.indexOf(">", p2)
        if (p3 < 0) {
            if (buffer.length > maxChars) return null
            return if (!readToBuffer()) null else readElement(tagName, maxChars)
        }
        val content = buffer.substring(p1, p3 + 1)
        val r = XMLElement()
        r.parseString(content)
        buffer.delete(0, p3 + 1)
        return r
    }

    var charBuffer = CharArray(1024)
    @Throws(IOException::class)
    fun readToBuffer(): Boolean {
        val l = lnr.read(charBuffer, 0, charBuffer.size)
        if (l <= 0) return false
        buffer.append(charBuffer)
        return true
    }
}