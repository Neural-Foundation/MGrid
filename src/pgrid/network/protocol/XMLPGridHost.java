/**
 *
 * Copyright (c) 2002-2008 The P-Grid Team, All Rights Reserved.
 *
 * This file is part of the P-Grid package.
 * P-Grid homepage: http://www.p-grid.org/
 *  
 * The P-Grid package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The P-Grid package is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the P-Grid package.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package pgrid.network.protocol;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import pgrid.PGridHost;
import pgrid.XMLizable;

import java.util.Hashtable;

/**
 * This class extends the {@link pgrid.PGridHost} with XML functionality.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class XMLPGridHost implements XMLizable, Cloneable {

	/**
	 * A part of the XML string.
	 */
	public static final String XML_HOST = "Host";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_HOST_ADDRESS = "Address";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_HOST_GUID = "GUID";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_HOST_PATH = "Path";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_HOST_PORT = "Port";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_HOST_REVISION = "Revision";

	/**
	 * The host.
	 */
	private PGridHost mHost = null;

	/**
	 * Creates an XML host object.
	 *
	 */
	public XMLPGridHost() {
	}

	/**
	 * Creates an XML host object for the host.
	 *
	 * @param host the host.
	 */
	public XMLPGridHost(PGridHost host) {
		mHost = host;
	}

	/**
	 * The Parser will call this method to report each chunk of character data. SAX parsers may return all contiguous
	 * character data in a single chunk, or they may split it into several chunks; however, all of the characters in any
	 * single event must come from the same external entity so that the Locator provides useful information.
	 *
	 * @param ch     the characters from the XML document.
	 * @param start  the start position in the array.
	 * @param length the number of characters to read from the array.
	 * @throws SAXException any SAX exception, possibly wrapping another exception.
	 */
	public void characters(char[] ch, int start, int length) throws SAXException {
	}

	/**
	 * The SAX parser will invoke this method at the end of every element in the XML document; there will be a
	 * corresponding startElement event for every endElement event (even when the element is empty).
	 *
	 * @param uri   the Namespace URI.
	 * @param lName the local name (without prefix), or the empty string if Namespace processing is not being performed.
	 * @param qName the qualified name (with prefix), or the empty string if qualified names are not available.
	 * @throws SAXException any SAX exception, possibly wrapping another exception.
	 */
	public void endElement(String uri, String lName, String qName) throws SAXException {
	}

	/**
	 * Returns the host.
	 * @return the host.
	 */
	public PGridHost getHost() {
		return mHost;
	}

	/**
	 * The Parser will invoke this method at the beginning of every element in the XML document; there will be a
	 * corresponding endElement event for every startElement event (even when the element is empty). All of the element's
	 * content will be reported, in order, before the corresponding endElement event.
	 *
	 * @param qName          the qualified name (with prefix), or the empty string if qualified names are not available.
	 * @param attrs          the attributes attached to the element. If there are no attributes, it shall be an empty Attributes
	 *                       object.
	 * @return a XMLPGridHost for the given XML string.
	 */
	public static PGridHost getHost(String qName, Attributes attrs) {
		if (qName.equals(XML_HOST) || qName.equals(MessageHeader.XML_DESTINATION_HOST) || qName.equals(MessageHeader.XML_SOURCE_HOST)) {
			int nb_attr = 3;
			PGridHost host = PGridHost.getHost(attrs.getValue(XML_HOST_GUID), attrs.getValue(XML_HOST_ADDRESS), attrs.getValue(XML_HOST_PORT));
			String path = attrs.getValue(XML_HOST_PATH);
			if (path != null) {
				nb_attr += 2;
				String timestamp = attrs.getValue(XML_HOST_REVISION);
				if (timestamp != null) nb_attr++;

				if (timestamp != null) {
					host.setPath(path, Long.parseLong(timestamp));
				} else {
					host.setPath(path);
				}
			}


			// retrieve all remaining properties
			int len = attrs.getLength();
			for(int i=nb_attr; i<len ;i++) {
				host.setProperty(attrs.getQName(i), attrs.getValue(i));
			}
			return host;
		}
		return null;
	}

	/**
	 * The Parser will invoke this method at the beginning of every element in the XML document; there will be a
	 * corresponding endElement event for every startElement event (even when the element is empty). All of the element's
	 * content will be reported, in order, before the corresponding endElement event.
	 *
	 * @param qName the qualified name (with prefix), or the empty string if qualified names are not available.
	 * @param attrs the attributes attached to the element. If there are no attributes, it shall be an empty Attributes
	 *              object.
	 * @return a XMLPGridHost for the given XML string.
	 */
	public static XMLPGridHost getXMLHost(String qName, Attributes attrs) {
		return toXMLHost(getHost(qName, attrs));
	}
	
	/**
	 * The Parser will invoke this method at the beginning of every element in the XML document; there will be a
	 * corresponding endElement event for every startElement event (even when the element is empty). All of the element's
	 * content will be reported, in order, before the corresponding endElement event.
	 *
	 * @param uri   the Namespace URI.
	 * @param lName the local name (without prefix), or the empty string if Namespace processing is not being performed.
	 * @param qName the qualified name (with prefix), or the empty string if qualified names are not available.
	 * @param attrs the attributes attached to the element. If there are no attributes, it shall be an empty Attributes
	 *              object.
	 * @throws SAXException any SAX exception, possibly wrapping another exception.
	 */
	public void startElement(String uri, String lName, String qName, Attributes attrs) throws SAXException {
		int nb_attr = 3;
		if (qName.equals(XML_HOST)) {
			PGridHost host = PGridHost.getHost(attrs.getValue(XML_HOST_GUID), attrs.getValue(XML_HOST_ADDRESS), attrs.getValue(XML_HOST_PORT));
			String path = attrs.getValue(XML_HOST_PATH);
			if (path != null) {
				nb_attr += 2;
				String rev = attrs.getValue(XML_HOST_REVISION);
				if (rev != null) {
					nb_attr++;
					host.setPath(path, Long.parseLong(rev));
				} else {
					host.setPath(path);
				}
			}

			// retrieve all remaining properties
			int len = attrs.getLength();
			for(int i=nb_attr; i<len ;i++) {
				host.setProperty(attrs.getQName(i), attrs.getValue(i));
			}
		}
	}

	/**
	 * Returns an XMLPGridHost for the host object.
	 *
	 * @return the XML host object.
	 */
	public static XMLPGridHost toXMLHost(PGridHost host) {
		return new XMLPGridHost(host);
	}

	/**
	 * Returns the XML representation of this object.
	 *
	 * @return the XML string.
	 */
	public String toXMLString() {
		// TODO make it static (also for other XML extensions)
		return toXMLString("", XML_NEW_LINE, false);
	}

	/**
	 * Returns the XML representation of this object.
	 *
	 * @param prefix  the XML prefix before each element in a new line.
	 * @param newLine the new line string.
	 * @return the XML string.
	 */
	public String toXMLString(String prefix, String newLine) {
		// TODO make it static (also for other XML extensions)
		return toXMLString(prefix, newLine, false);
	}

	/**
	 * Returns the XML representation of this object.
	 *
	 * @param prefix  the XML prefix before each element in a new line.
	 * @param newLine the new line string.
	 * @param path    if the path and its timestamp should be included.
	 * @return the XML string.
	 */
	public String toXMLString(String prefix, String newLine, boolean path) {
		// TODO make it static (also for other XML extensions)
		StringBuffer strBuff = new StringBuffer(100);
		strBuff.append(prefix + XML_ELEMENT_OPEN + XML_HOST + // {prefix}<Host
				XML_SPACE + XML_HOST_GUID + XML_ATTR_OPEN + mHost.getGUID().toString() + XML_ATTR_CLOSE + // _GUID="GUID"
				XML_SPACE + XML_HOST_ADDRESS + XML_ATTR_OPEN + mHost.getAddressString() + XML_ATTR_CLOSE + // _Address="ADDRESS"
				XML_SPACE + XML_HOST_PORT + XML_ATTR_OPEN + mHost.getPort() + XML_ATTR_CLOSE); // _Port="PORT"
		if (path) {
			strBuff.append(XML_SPACE + XML_HOST_PATH + XML_ATTR_OPEN + mHost.getPath() + XML_ATTR_CLOSE + // _Path="PATH"
					XML_SPACE + XML_HOST_REVISION + XML_ATTR_OPEN + mHost.getRevision() + XML_ATTR_CLOSE); // _Timestamp="TIMESTAMP"
		}
		if (!mHost.getProperties().isEmpty()) {
			Hashtable<String, String> ht = mHost.getProperties();
			for (String key: ht.keySet()) {
				strBuff.append(XML_SPACE + key + XML_ATTR_OPEN + ht.get(key) + XML_ATTR_CLOSE);
			}
		}

		strBuff.append(XML_ELEMENT_END_CLOSE + newLine); // />{newLine}
		return strBuff.toString();
	}
	
	
	/**
	 * Report the start of a CDATA section.
	 * <p/>
	 * <p>The contents of the CDATA section will be reported through
	 * the regular {@link org.xml.sax.ContentHandler#characters
	 * characters} event; this event is intended only to report
	 * the boundary.</p>
	 *
	 * @throws org.xml.sax.SAXException The application may raise an exception.
	 * @see #endCDATA
	 */
	public void startCDATA() throws SAXException {
	}

	/**
	 * Report the end of a CDATA section.
	 *
	 * @throws org.xml.sax.SAXException The application may raise an exception.
	 * @see #startCDATA
	 */
	public void endCDATA() throws SAXException {
	}

}
