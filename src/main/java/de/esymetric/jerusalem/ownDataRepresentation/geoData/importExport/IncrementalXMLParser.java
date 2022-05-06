package de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport;

import net.n3.nanoxml.XMLElement;

import java.io.IOException;
import java.io.LineNumberReader;

public class IncrementalXMLParser {

	LineNumberReader lnr;
	StringBuilder buffer = new StringBuilder();
	
	public IncrementalXMLParser(LineNumberReader reader) {
		lnr = reader;
	}
	
	public XMLElement readElement(String tagName, int maxChars) throws IOException  {
		int p1 = buffer.indexOf("<" + tagName);
		if( p1 < 0 ) {
			if( buffer.length() > maxChars ) return null;
			if( !readToBuffer() ) return null;
			return readElement(tagName, maxChars);
		}
		
		int p2 = buffer.indexOf("</" + tagName, p1 + tagName.length());
		if( p2 < 0 ) {
			if( buffer.length() > maxChars ) return null;
			if( !readToBuffer() ) return null;
			return readElement(tagName, maxChars);
		}
		
		int p3 = buffer.indexOf(">", p2);
		if( p3 < 0 ) {
			if( buffer.length() > maxChars ) return null;
			if( !readToBuffer() ) return null;
			return readElement(tagName, maxChars);
		}
		
		String content = buffer.substring(p1, p3 + 1);
		XMLElement r = new XMLElement();
		r.createElement(content); // TODO don't know if this parses
		// TODO check r.parseString(content);
		buffer.delete(0, p3 + 1);
		return r;
	}
	
	char[] charBuffer = new char[1024];
	boolean readToBuffer() throws IOException {
		int l = lnr.read(charBuffer, 0, charBuffer.length);
		if( l <= 0 ) return false;
		buffer.append(charBuffer);
		return true;
	}
	
}
