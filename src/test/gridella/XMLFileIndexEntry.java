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

package test.gridella;


import p2p.basic.Key;
import p2p.basic.GUID;
import p2p.basic.Peer;
import p2p.index.Type;
import pgrid.PGridHost;

/**
 * This class represents a shared Gridella file.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class XMLFileIndexEntry extends FileIndexEntry implements Cloneable {

	/**
	 * Part separator
	 */
	public static final String SEPARATOR = "\n";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_DATA_ITEM_INFO = "Info";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_DATA_ITEM_FILE = "File";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_DATA_ITEM_NAME = "Name";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_DATA_ITEM_PATH = "Path";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_DATA_ITEM_QOS = "QoS";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_DATA_ITEM_SIZE = "Size";

	/**
	 * A temp string used during parsing a data item.
	 */
	private String mTmpString = null;

	/**
	 * Creates a new empty PGridP2P data item.
	 */
	public XMLFileIndexEntry() {
		super();
	}

	/**
	 * Creates a new PGridP2P data item with all parameters.
	 *
	 * @param host  the storing host.
	 * @param qoS   the QoS of the storing host.
	 * @param key   the key for this file name.
	 * @param path  the file path.
	 * @param name  the file name.
	 * @param size  the file size.
	 * @param infos the extra informations for this file.
	 * @param desc  the file description.
	 */
	/*public XMLFileIndexEntry(PGridHost host, Type type,  int qoS, Key key, String path, String name, int size, String infos, String desc) {
		super(host, qoS, key, path, name, size, infos, desc);
	}*/

		/**
	 * Creates a new PGridP2P data item with all parameters.
	 *
	 * @param guid      the unique id.
	 * @param type      the data type.
	 * @param key       the key for this host.
	 * @param peer      the inserting peer.
	 * @param data      the data.
	 */
	public XMLFileIndexEntry(GUID guid, Type type, Key key, Peer peer, Object data) {
		super(guid, type, key, peer, data);
		decode();
	}

	/**
	 * Creates a new PGridP2P data item with all parameters.
	 *
	 * @param host  the storing host.
	 * @param qoS   the QoS of the storing host.
	 * @param key   the key for this file name.
	 * @param path  the file path.
	 * @param name  the file name.
	 * @param size  the file size.
	 * @param infos the extra informations for this file.
	 * @param desc  the file description.
	 */
	public XMLFileIndexEntry(GUID guid, Type type, Key key, PGridHost host, int qoS, String path, String name, int size, String infos, String desc) {
		setGUID(guid);
		setType(type);
		setPeer(host);
		setKey(key);
		setPath(path);
		setName(name);
		setSize(size);
		setInfos(infos);
		setDesc(desc);
		setQoS(qoS);
		encode();
	}

	public void encode() {
		String data = "";

		data =  getName()+SEPARATOR+
				getDesc()+SEPARATOR+
				getSize()+SEPARATOR+
				getPath()+SEPARATOR+
				getQoS()+
				(getInfos().length() > 0?SEPARATOR+getInfos():"");

		setData(data);
	}

	public void decode() {
		String data = (String)getData();
		String[] st = data.split(SEPARATOR);

		try {

			if (st.length > 0) {
				setName(st[0]);
			} else setName("");

			if (st.length > 1) {
				setDesc(st[1]);
			} else setDesc("");

			if (st.length > 2) {
				setSize(Integer.parseInt(st[2]));
			} else setSize(0);

			if (st.length > 3) {
				setPath(st[3]);
			} else setPath("");

			if (st.length > 4) {
				setQoS(Integer.parseInt(st[4]));
			} else setQoS(0);

			if (st.length > 5) {
				setInfos(st[5]);
			} else setInfos("");

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(mName);
			System.out.println(mDesc);
			System.out.println(mData);
		}

	}

 	public Object clone() {
		 return super.clone();
	 }

//	/**
//	 * The Parser will call this method to report each chunk of character data. SAX parsers may return all contiguous
//	 * character data in a single chunk, or they may split it into several chunks; however, all of the characters in any
//	 * single event must come from the same external entity so that the Locator provides useful information.
//	 *
//	 * @param ch     the characters from the XML document.
//	 * @param start  the start position in the array.
//	 * @param length the number of characters to read from the array.
//	 * @throws org.xml.sax.SAXException any SAX exception, possibly wrapping another exception.
//	 */
//	public void characters(char[] ch, int start, int length) throws SAXException {
//		if (parsingCDATA()) {
//			if (mTmpString == null) {
//				mTmpString = String.valueOf(ch, start, length);
//			} else {
//				String append = String.valueOf(ch, start, length);
//				if (append.trim().length() > 0)
//					mTmpString = mTmpString.concat(append);
//			}
//		}
//	}
//
//	/**
//	 * The Parser will invoke this method at the beginning of every element in the XML document; there will be a
//	 * corresponding endElement event for every startElement event (even when the element is empty). All of the element's
//	 * content will be reported, in order, before the corresponding endElement event.
//	 *
//	 * @param uri   the Namespace URI.
//	 * @param lName the local name (without prefix), or the empty string if Namespace processing is not being performed.
//	 * @param qName the qualified name (with prefix), or the empty string if qualified names are not available.
//	 * @param attrs the attributes attached to the element. If there are no attributes, it shall be an empty Attributes
//	 *              object.
//	 * @throws org.xml.sax.SAXException any SAX exception, possibly wrapping another exception.
//	 */
//	public void startElement(String uri, String lName, String qName, Attributes attrs) throws SAXException {
//		if (qName.equals(XML_INDEX_ITEM)) {
//			// Data Item
//			mType = IndexManager.getTypeByString(attrs.getValue(XML_INDEX_ITEM_TYPE));
//			//mHost = XMLPGridHost.getPeer(attrs.getValue(XML_HOST_GUID), attrs.getValue(XML_HOST_ADDRESS), attrs.getValue(XML_HOST_PORT));
//			mQoS = Integer.parseInt(attrs.getValue(XML_DATA_ITEM_QOS));
//			mKey = new PGridKey(attrs.getValue(XML_INDEX_ITEM_KEY));
//			//mPath = attrs.getValue(XML_DATA_ITEM_PATH);
//			//mName = attrs.getValue(XML_DATA_ITEM_NAME);
//			//mSize = Integer.parseInt(attrs.getValue(XML_DATA_ITEM_SIZE));
//			//mInfos = attrs.getValue(XML_DATA_ITEM_INFO);
//		} else if (qName.equals(XMLPGridHost.XML_HOST)) {
//			mHost = XMLPGridHost.getHost(qName, attrs, false);
//		} else if (qName.equals(XML_DATA_ITEM_FILE)) {
//			mPath = attrs.getValue(XML_DATA_ITEM_PATH);
//			mName = attrs.getValue(XML_DATA_ITEM_NAME);
//			mSize = Integer.parseInt(attrs.getValue(XML_DATA_ITEM_SIZE));
//		}
//		/*
//										if (qName.equals(XML_INDEX_ITEM)) {
//														// Data Item
//														mType = IndexManager.getTypeByString(attrs.getValue(XML_INDEX_ITEM_TYPE));
//														mHost = XMLPGridHost.getPeer(attrs.getValue(XML_HOST_GUID), attrs.getValue(XML_HOST_ADDRESS), attrs.getValue(XML_HOST_PORT));
//														mQoS = Integer.parseInt(attrs.getValue(XML_DATA_ITEM_QOS));
//														mKey = attrs.getValue(XML_INDEX_ITEM_KEY);
//														mPath = attrs.getValue(XML_DATA_ITEM_PATH);
//														mName = attrs.getValue(XML_DATA_ITEM_NAME);
//														mSize = Integer.parseInt(attrs.getValue(XML_DATA_ITEM_SIZE));
//														mInfos = attrs.getValue(XML_DATA_ITEM_INFO);
//										}
//										*/
//	}
//
//	/**
//	 * The SAX parser will invoke this method at the end of every element in the XML document; there will be a
//	 * corresponding startElement event for every endElement event (even when the element is empty).
//	 *
//	 * @param uri   the Namespace URI.
//	 * @param lName the local name (without prefix), or the empty string if Namespace processing is not being performed.
//	 * @param qName the qualified name (with prefix), or the empty string if qualified names are not available.
//	 * @throws SAXException any SAX exception, possibly wrapping another exception.
//	 */
//	public void endElement(String uri, String lName, String qName) throws SAXException {
//		if (qName.equals(XML_DATA_ITEM_INFO)) {
//			mInfos = mTmpString.trim();
//			mTmpString = null;
//		} else if (qName.equals(XML_INDEX_ITEM_DATA)) {
//			mData = mTmpString.trim();
//			mTmpString = null;
//		}
//	}
//
//	/**
//	 * Returns the XML representation of this object.
//	 *
//	 * @return the XML string.
//	 */
//	public String toXMLString() {
//		return toXMLString("", XML_NEW_LINE);
//	}
//
//	/**
//	 * Returns a string represantation of this result set.
//	 *
//	 * @param prefix        a string prefix for each line.
//	 * @param newLine       the string for a new line, e.g. \n.
//	 * @param withSignature whether or not to include the signature.
//	 * @return a string represantation of this result set.
//	 */
//	public String toXMLString(String prefix, String newLine, boolean withSignature) {
//		return toXMLString(prefix, newLine);
//	}
//
//	/**
//	 * Returns a string represantation of this result set.
//	 *
//	 * @param prefix  a string prefix for each line.
//	 * @param newLine the string for a new line, e.g. \n.
//	 * @return a string represantation of this result set.
//	 */
//	public String toXMLString(String prefix, String newLine) {
//		StringBuffer strBuff = new StringBuffer(200);
//		strBuff.append(prefix + XML_ELEMENT_OPEN + XML_INDEX_ITEM + // {prefix}<IndexEntry
//				XML_SPACE + XML_INDEX_ITEM_TYPE + XML_ATTR_OPEN + getTypeString() + XML_ATTR_CLOSE + // _Type="TYPE"
//				XML_SPACE + XML_INDEX_ITEM_KEY + XML_ATTR_OPEN + mKey + XML_ATTR_CLOSE + // _Key="KEY"
//				XML_SPACE + XML_DATA_ITEM_QOS + XML_ATTR_OPEN + mQoS + XML_ATTR_CLOSE + XML_ELEMENT_CLOSE + newLine + // _QoS="QOS">{newLine}
//				XMLPGridHost.toXMLHost(mHost).toXMLString(prefix + XML_TAB, newLine, false)); // {prefix}\t<Host ...>{newLine}
//		if ((getName() != null) && (getName().length() > 0))
//			strBuff.append(prefix + XML_TAB + XML_ELEMENT_OPEN + XML_DATA_ITEM_FILE + // {prefix}\t<File
//					XML_SPACE + XML_DATA_ITEM_PATH + XML_ATTR_OPEN + XMLTools.xmlCoding(getPath()) + XML_ATTR_CLOSE + // _Path="PATH"
//					XML_SPACE + XML_DATA_ITEM_NAME + XML_ATTR_OPEN + XMLTools.xmlCoding(getName()) + XML_ATTR_CLOSE + // _Name="NAME"
//					XML_SPACE + XML_DATA_ITEM_SIZE + XML_ATTR_OPEN + mSize + XML_ATTR_CLOSE + XML_ELEMENT_END_CLOSE + newLine); // _Size="SIZE">{newLine}
//		if ((mInfos != null) && (mInfos.trim().length() > 0))
//			strBuff.append(prefix + XML_TAB + XML_ELEMENT_OPEN + XML_DATA_ITEM_INFO + XML_ELEMENT_CLOSE + // {prefix}\t<Info>
//					XML_CDATA_OPEN + mInfos + XML_CDATA_CLOSE + // <![CDATA[INFOS]]>
//					XML_ELEMENT_OPEN_END + XML_DATA_ITEM_INFO + XML_ELEMENT_CLOSE + newLine); // </Info>{newLine}
//		if ((mData != null) && (mData.trim().length() > 0))
//			strBuff.append(prefix + XML_TAB + XML_ELEMENT_OPEN + XML_INDEX_ITEM_DATA + XML_ELEMENT_CLOSE + // {prefix}\t<Data>
//					XML_CDATA_OPEN + mData + XML_CDATA_CLOSE + // <![CDATA[DESC]]>
//					XML_ELEMENT_OPEN_END + XML_INDEX_ITEM_DATA + XML_ELEMENT_CLOSE + newLine); // </Data>{newLine}
//		strBuff.append(prefix + XML_ELEMENT_OPEN_END + XML_INDEX_ITEM + XML_ELEMENT_CLOSE + newLine); // </IndexEntry>{newLine}
//		return strBuff.toString();
//	}

}