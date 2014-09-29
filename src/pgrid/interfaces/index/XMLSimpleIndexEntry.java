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

package pgrid.interfaces.index;

import mgrid.core.Point;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import p2p.basic.GUID;
import p2p.basic.Key;
import p2p.basic.Peer;
import p2p.index.Type;
import pgrid.PGridKey;
import pgrid.Properties;
import pgrid.XMLIndexEntry;
import pgrid.PGridHost;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.protocol.XMLPGridHost;

/**
 * This class represents a shared Gridella file.
 * 
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman
 *         Schmidt</a>
 * @version 1.0.0
 */
public class XMLSimpleIndexEntry extends SimpleIndexEntry implements
		XMLIndexEntry {

	/**
	 * read the default values from ini file
	 */
	private static PGridP2P mPGridP2P = PGridP2P.sharedInstance();
	private static final String TYPE_NAME = mPGridP2P
			.propertyString(Properties.TYPE_NAME);
	private static final int PORT_NUMBER = Integer.parseInt(mPGridP2P
			.propertyString(Properties.PORT_NUMBER));

	/**
	 * A temp string used during parsing a data item.
	 */
	private String mTmpString = null;
	
	/**
	 * A temp string used during parsing a point.ID
	 */
	private String mIDTmpString = null;

	/**
	 * A XML string represeting this IE.
	 */
	private String mXMLString = null;

	/**
	 * Creates a new empty PGridP2P data item.
	 */
	public XMLSimpleIndexEntry() {
		super();
	}

	/**
	 * Creates a new PGridP2P data item with all parameters.
	 * 
	 * @param guid
	 *            the unique id.
	 * @param type
	 *            the data type.
	 * @param key
	 *            the key for this host.
	 * @param peer
	 *            the inserting peer.
	 * @param data
	 *            the data.
	 */
	public XMLSimpleIndexEntry(GUID guid, Type type, Key key, Peer peer,
			Object data) {
		super(guid, type, key, peer, data);
	}

	/**
	 * The Parser will call this method to report each chunk of character data.
	 * SAX parsers may return all contiguous character data in a single chunk,
	 * or they may split it into several chunks; however, all of the characters
	 * in any single event must come from the same external entity so that the
	 * Locator provides useful information.
	 * 
	 * @param ch
	 *            the characters from the XML document.
	 * @param start
	 *            the start position in the array.
	 * @param length
	 *            the number of characters to read from the array.
	 * @throws org.xml.sax.SAXException
	 *             any SAX exception, possibly wrapping another exception.
	 */
	/*
	 * public void characters(char[] ch, int start, int length) throws
	 * SAXException {
	 * 
	 * 
	 * if(parsingCDATA()) { if (mTmpString == null) { mTmpString =
	 * String.valueOf(ch, start, length);
	 * 
	 * } else { String append = String.valueOf(ch, start, length); if
	 * (append.length() > 0) mTmpString = mTmpString.concat(append);
	 * 
	 * } }
	 * 
	 * }
	 */

	public void characters(char[] ch, int start, int length)
			throws SAXException {

		if (parsingCDATA()) {
			if (mTmpString == null) {
				mTmpString = String.valueOf(ch, start, length);

			} else {
				String append = String.valueOf(ch, start, length);
				if (append.length() > 0)
					mTmpString = mTmpString.concat(append);
			}
		}
		/*
		 * if(mXDataSection) { if(mXTmpString==null) { mXTmpString =
		 * String.valueOf(ch, start, length); mXDataSection = false;
		 * 
		 * } }
		 * 
		 * if(mYDataSection) { if(mYTmpString==null) { mYTmpString =
		 * String.valueOf(ch, start, length); mYDataSection = false; } }
		 */

		if (mIDDataSection) {
			if (mIDTmpString == null) {
				mIDTmpString = String.valueOf(ch, start, length);
				mIDDataSection = false;
			}
		}

	}

	/**
	 * The Parser will invoke this method at the beginning of every element in
	 * the XML document; there will be a corresponding endElement event for
	 * every startElement event (even when the element is empty). All of the
	 * element's content will be reported, in order, before the corresponding
	 * endElement event.
	 * 
	 * @param uri
	 *            the Namespace URI.
	 * @param lName
	 *            the local name (without prefix), or the empty string if
	 *            Namespace processing is not being performed.
	 * @param qName
	 *            the qualified name (with prefix), or the empty string if
	 *            qualified names are not available.
	 * @param attrs
	 *            the attributes attached to the element. If there are no
	 *            attributes, it shall be an empty Attributes object.
	 * @throws org.xml.sax.SAXException
	 *             any SAX exception, possibly wrapping another exception.
	 */
	public void startElement(String uri, String lName, String qName,
			Attributes attrs) throws SAXException {

		if (qName.equals(XML_INDEX_ITEM)) {
			// Data Item
			mGUID = pgrid.GUID.getGUID(attrs.getValue(XML_INDEX_ITEM_GUID));
			mType = PGridIndexFactory.sharedInstance().getTypeByString(
					TYPE_NAME);
			mKey = new PGridKey(attrs.getValue(XML_INDEX_ITEM_KEY));
		} else if (qName.equals(XMLPGridHost.XML_HOST)) {
			mHost = XMLPGridHost.getHost(qName, attrs);
		}
		if (qName.equals(XML_INDEX_ITEM_ID)) {
			mIDDataSection = true;
		}

	}

	/**
	 * The SAX parser will invoke this method at the end of every element in the
	 * XML document; there will be a corresponding startElement event for every
	 * endElement event (even when the element is empty).
	 * 
	 * @param uri
	 *            the Namespace URI.
	 * @param lName
	 *            the local name (without prefix), or the empty string if
	 *            Namespace processing is not being performed.
	 * @param qName
	 *            the qualified name (with prefix), or the empty string if
	 *            qualified names are not available.
	 * @throws SAXException
	 *             any SAX exception, possibly wrapping another exception.
	 */
	public void endElement(String uri, String lName, String qName)
			throws SAXException {

		if (qName.equals(XML_INDEX_ITEM_KEY)) {
			mData = mTmpString;
			mTmpString = null;
		}

		if (qName.equals(XML_INDEX_ITEM_ID)) {
			mPointID = Long.parseLong(mIDTmpString);
			mIDTmpString = null;
		}
		decode();
	}

	/**
	 * Returns the XML representation of this object.
	 * 
	 * @return the XML string.
	 */
	public String toString() {
		return toXMLString("", XML_NEW_LINE);
	}

	/**
	 * Returns the XML representation of this object.
	 * 
	 * @return the XML string.
	 */
	public String toXMLString() {
		return toXMLString("", XML_NEW_LINE);
	}

	/**
	 * Returns a string represantation of this result set.
	 * 
	 * @param prefix
	 *            a string prefix for each line.
	 * @param newLine
	 *            the string for a new line, e.g. \n.
	 * @return a string represantation of this result set.
	 */
	public String toXMLString(String prefix, String newLine) {
		return toXMLString(prefix, newLine, false);
	}

	/**
	 * Clear content of this object. This method can be used in situation where
	 * only a single object is used for decoding multiple objects.
	 */
	public void clear() {
		mData = "";
		mGUID = null;
		mType = null;
		mKey = null;
		mHost = null;
		mTmpString = null;
		mIDTmpString = null;
		mPoint = null;
	}

	private static String a = XML_ELEMENT_OPEN + XML_INDEX_ITEM + XML_SPACE
			+ XML_INDEX_ITEM_GUID + XML_ATTR_OPEN; // <IndexEntry GUID =\"
//	private static String b = XML_ATTR_CLOSE + XML_SPACE + XML_INDEX_ITEM_TYPE
	//		+ XML_ATTR_OPEN; // \" Type =\"
	private static String c = XML_ATTR_CLOSE + XML_SPACE + XML_INDEX_ITEM_KEY
			+ XML_ATTR_OPEN; // \" Key =\"
	private static String d = XML_ATTR_CLOSE + XML_ELEMENT_CLOSE + "\n"; // \">
																			// \n
	// private static String e = XML_TAB + XML_ELEMENT_OPEN +
	// XML_INDEX_ITEM_DATA + XML_ELEMENT_CLOSE + XML_CDATA_OPEN; //
	// \t<Data><![CDATA[
	// private static String f = XML_CDATA_CLOSE + XML_ELEMENT_OPEN_END +
	// XML_INDEX_ITEM_DATA + XML_ELEMENT_CLOSE + "\n"; // ]] </Data> \n
	// private static String fx = XML_TAB+XML_ELEMENT_OPEN + XML_INDEX_ITEM_X+
	// XML_ELEMENT_CLOSE ; // \t<X><<![XDATA[
	// private static String fx_close = XML_ELEMENT_OPEN_END + XML_INDEX_ITEM_X
	// + XML_ELEMENT_CLOSE + "\n"; // </X> \n
	// private static String fy = XML_TAB+XML_ELEMENT_OPEN + XML_INDEX_ITEM_Y +
	// XML_ELEMENT_CLOSE ; //\t<Y><<![YDATA[
	// private static String fy_close = XML_ELEMENT_OPEN_END + XML_INDEX_ITEM_Y
	// + XML_ELEMENT_CLOSE + "\n"; // </Y> \n
	private static String fid = XML_TAB + XML_ELEMENT_OPEN + XML_INDEX_ITEM_ID
			+ XML_ELEMENT_CLOSE; // \t<ID><<![IDDATA[
	private static String fid_close = XML_ELEMENT_OPEN_END + XML_INDEX_ITEM_ID
			+ XML_ELEMENT_CLOSE + "\n"; // </ID> \n
	private static String g = XML_ELEMENT_OPEN_END + XML_INDEX_ITEM
			+ XML_ELEMENT_CLOSE + "\n"; // </ IndexEntry> \n

	

	public String toXMLString(String prefix, String newLine,
			boolean withSignature) {

		StringBuffer strBuff = new StringBuffer(500);
		strBuff.append(prefix)
				.append(a)
				.append(mGUID.toString())
				.append(c)
				.append(mKey)
				.append(d)
				.append(XMLPGridHost.toXMLHost(mHost).toXMLString(
						prefix + XML_TAB, newLine, false)); // {prefix}\t<Host
		strBuff.append(prefix).append(fid).append(mPointID).append(fid_close); // </ID>{newLine}
		strBuff.append(prefix).append(g);// {prefix}</IndexEntry>
		return strBuff.toString();
	}

	public static String toXMLString(String prefix, String newLine,
			String dGuid, String type, String key, PGridHost host, String data,
			Point point) {
		StringBuffer strBuff = new StringBuffer(500);
		strBuff.append(prefix)
				.append(a)
				.append(dGuid)
		//		.append(b)
		//		.append(type)
				.append(c)
				.append(key)
				.append(d)
				.append(XMLPGridHost.toXMLHost(host).toXMLString(
						prefix + XML_TAB, newLine, false)); // {prefix}\t<Host
															// ...>{newLine}
	//	if ((data != null) && (data.trim().length() > 0))
			// strBuff.append(prefix).append(e).append(data).append(f); //
			// </Data>{newLine}
			// strBuff.append(prefix).append(fx).append(point.x).append(fx_close);
			// // </X>{newLine}
			// strBuff.append(prefix).append(fy).append(point.y).append(fy_close);
			// // </Y>{newLine}
			// strBuff.append(prefix).append(fid).append(point.id).append(fid_close);
			// // </ID>{newLine}
			strBuff.append(prefix).append(g);// {prefix}</IndexEntry>
		return strBuff.toString();
	}

	/**
	 * Returns a string represantation of an index entry.
	 * 
	 * @param prefix
	 *            a string prefix for each line.
	 * @param newLine
	 *            the string for a new line, e.g. \n.
	 * 
	 * @return a string represantation of this result set.
	 */

	public static String toXMLString(String prefix, String newLine,
			String dGuid, String type, String key, PGridHost host, String data) {
		StringBuffer strBuff = new StringBuffer(200);
		strBuff.append(prefix)
				.append(a)
				.append(dGuid)
	//			.append(b)
	//			.append(type)
				.append(c)
				.append(key)
				.append(d)
				.append(XMLPGridHost.toXMLHost(host).toXMLString(
						prefix + XML_TAB, newLine, false)); // {prefix}\t<Host
															// ...>{newLine}
		// if ((data != null) && (data.trim().length() > 0))
		// strBuff.append(prefix).append(e).append(data).append(f); //
		// </Data>{newLine}
		strBuff.append(prefix).append(g);// {prefix}</IndexEntry>
		return strBuff.toString();
	}

	public void decode() {

	}

}