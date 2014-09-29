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
import p2p.basic.GUID;
import pgrid.XMLizable;
import pgrid.core.index.DBIndexTable;
import pgrid.core.index.IndexTable;
import pgrid.core.XMLRoutingTable;

/**
 * This class represents a search path reply message.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class SearchPathReplyMessage extends PGridMessageImp {

	/**
	 * The search path reply code Path Changed.
	 */
	public static final int CODE_PATH_CHANGED = 400;

	/**
	 * The search path reply code OK.
	 */
	public static final int CODE_OK = 200;

	/**
	 * A part of the XML string.
	 */
	public static final String XML_SEARCH_PATH_REPLY = "SearchPathReply";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_SEARCH_PATH_REPLY_CODE = "Code";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_SEARCH_PATH_REPLY_MINSTORAGE = "MinStorage";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_SEARCH_PATH_REPLY_PATH = "Path";

	/**
	 * The Query Hit reply code.
	 */
	private int mCode = -1;

	/**
	 * The data table.
	 */
	private DBIndexTable mIndexTable = null;

	/**
	 * The minstorage estimation
	 */
	private int mMinStorage = -1;

	/**
	 * The path of the host.
	 */
	private String mPath = null;

	/**
	 * The routing table.
	 */
	private XMLRoutingTable mRoutingTable = null;

	/**
	 * The temporary variable during parsing.
	 */
	private XMLizable mParsedObject = null;

	/**
	 * The data table as XML.
	 */
	private XMLIndexTable mXMLIndexTable = null;

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_SEARCH_PATH_REPLY;
	}

	/**
	 * Default constructor
	 */
	public SearchPathReplyMessage() {
	}

	/**
	 * Creates a search path reply message with the given header.
	 *
	 * @param header the message header.
	 */
	public SearchPathReplyMessage(MessageHeader header) {
		super(header);
	}

	/**
	 * Creates a new search path reply message with given values.
	 *
	 * @param refGuid GUID of the SearchPathMessage this reply refers to
	 * @param path expected path
	 */
	public SearchPathReplyMessage(GUID refGuid, String path) {
		super(refGuid);
		mPath = path;
		mCode = CODE_PATH_CHANGED;
		mMinStorage = 0;
	}

	/**
	 * Returns the message GUID to which this message refere to.
	 * @return  the message GUID to which this message refere to.
	 */
	public GUID getReferencedMsgGUID() {
		return getHeader().getReferences().iterator().next();
	}

	/**
	 * Creates a new search path reply message with given values.
	 *
	 * @param refGuid 	   The GUID of the SearchPathMessage this reply refers to
	 * @param path         the path of the host.
	 * @param routingTable the routing table of the host.
	 * @param dataTable    the dataTable items of the host.
	 * @param minStorage	Estimation of the minstorage variable.
	 */
	public SearchPathReplyMessage(GUID refGuid, String path, XMLRoutingTable routingTable, DBIndexTable dataTable, int minStorage) {
		super(refGuid);
		mPath = path;
		mRoutingTable = routingTable;
		mIndexTable = dataTable;
		mXMLIndexTable = new XMLIndexTable(dataTable);
		mCode = CODE_OK;
		mMinStorage = minStorage;
	}

	/**
	 * Returns the message code.
	 *
	 * @return the code.
	 */
	public int getCode() {
		return mCode;
	}

	/**
	 * Returns the data items.
	 *
	 * @return the data items.
	 */
	public IndexTable getIndexTable() {
		return mIndexTable;
	}

	/**
	 * Sets the data items.
	 *
	 * @param dataTable the data items.
	 */
	public void setIndexItems(DBIndexTable dataTable) {
		mIndexTable = dataTable;
	}

	/**
	 * Returns the host path.
	 *
	 * @return the host path.
	 */
	public String getPath() {
		return mPath;
	}

	/**
	 * Sets the host path.
	 *
	 * @param path the host path.
	 */
	public void setPath(String path) {
		mPath = path;
	}

	/**
	 * Returns the routing table.
	 *
	 * @return the routing table.
	 */
	public pgrid.core.RoutingTable getRoutingTable() {
		return mRoutingTable;
	}

	/**
	 * Sets the routing table.
	 *
	 * @param routingTable the routing table.
	 */
	public void setRoutingTable(XMLRoutingTable routingTable) {
		mRoutingTable = routingTable;
	}

	/**
	 * Returns the message size.
	 *
	 * @return the message size.
	 */
	public int getSize() {
		return toXMLString().length();
	}

	/**
	 * Tests if this query hit message is valid.
	 *
	 * @return <code>true</code> if valid.
	 */
	public boolean isValid() {
		if (!super.isValid()) {
			return false;
		}
		if (mCode == -1)
			return false;
		if ((mCode == CODE_OK) && (mPath == null))
			return false;
		if ((mCode != CODE_OK) && (mCode != CODE_PATH_CHANGED))
			return false;
		return true;
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
	public synchronized void characters(char[] ch, int start, int length) throws SAXException {
		if (mParsedObject != null) {
			mParsedObject.characters(ch, start, length);
		}
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
	public synchronized void endElement(String uri, String lName, String qName) throws SAXException {
		if (qName.equals(XMLRoutingTable.XML_ROUTING_TABLE)) {
			mParsedObject.endElement(uri, lName, qName);
			mParsedObject = null;
		} else if (qName.equals(XMLIndexTable.XML_INDEX_TABLE)) {
			mParsedObject.endElement(uri, lName, qName);
			mParsedObject = null;
		} else if (mParsedObject != null) {
			mParsedObject.endElement(uri, lName, qName);
		}
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
	public synchronized void startElement(String uri, String lName, String qName, Attributes attrs) throws SAXException {
		if (qName.equals(XML_SEARCH_PATH_REPLY)) {
			mPath = attrs.getValue(XML_SEARCH_PATH_REPLY_PATH);
			mCode = Integer.parseInt(attrs.getValue(XML_SEARCH_PATH_REPLY_CODE));
			mMinStorage = Integer.parseInt(attrs.getValue(XML_SEARCH_PATH_REPLY_MINSTORAGE));
		} else if (qName.equals(XMLRoutingTable.XML_ROUTING_TABLE)) {
			mRoutingTable = new XMLRoutingTable();
			mRoutingTable.startElement(uri, lName, qName, attrs);
			mParsedObject = mRoutingTable;
		} else if (qName.equals(XMLIndexTable.XML_INDEX_TABLE)) {
			mIndexTable = new DBIndexTable(getHeader().getHost());
			mXMLIndexTable = new XMLIndexTable(mIndexTable);
			mXMLIndexTable.startElement(uri, lName, qName, attrs);
			mParsedObject = mXMLIndexTable;
		} else if (mParsedObject != null) {
			mParsedObject.startElement(uri, lName, qName, attrs);
		}
	}

	/**
	 * Returns a string represantation of this message.
	 *
	 * @return a string represantation of this message.
	 */
	public String toXMLString() {
		return toXMLString(XML_TAB, XML_NEW_LINE);
	}

	/**
	 * Returns the XML representation of this object.
	 *
	 * @param prefix  the XML prefix before each element in a new line.
	 * @param newLine the new line string.
	 * @return the XML string.
	 */
	public String toXMLString(String prefix, String newLine) {
		StringBuffer strBuff;
		strBuff = new StringBuffer(100);
		strBuff.append(prefix + XML_ELEMENT_OPEN + XML_SEARCH_PATH_REPLY); // {prefix}<SearchPathReply
		strBuff.append(XML_SPACE + XML_SEARCH_PATH_REPLY_PATH + XML_ATTR_OPEN + mPath + XML_ATTR_CLOSE); // _Path="PATH"
		strBuff.append(XML_SPACE + XML_SEARCH_PATH_REPLY_MINSTORAGE + XML_ATTR_OPEN + mMinStorage + XML_ATTR_CLOSE); // _MinStorage="MINSTORAGE"
		strBuff.append(XML_SPACE + XML_SEARCH_PATH_REPLY_CODE + XML_ATTR_OPEN + mCode + XML_ATTR_CLOSE + XML_ELEMENT_CLOSE + newLine); // _Code="CODE">{newLine}

		// routing table
		if ((mCode == CODE_OK) && (mRoutingTable != null))
			strBuff.append(mRoutingTable.toXMLString(prefix + XML_TAB, newLine, true, true, true));

		// data table
		if ((mCode == CODE_OK) && (mIndexTable != null))
			strBuff.append(mXMLIndexTable.toXMLString(prefix + XML_TAB, newLine));

		strBuff.append(prefix + XML_ELEMENT_OPEN_END + XML_SEARCH_PATH_REPLY + XML_ELEMENT_CLOSE + newLine); // {prefix}</SearchPathReply>{newLine}
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
		if (mParsedObject != null) mParsedObject.startCDATA();
	}

	/**
	 * Report the end of a CDATA section.
	 *
	 * @throws org.xml.sax.SAXException The application may raise an exception.
	 * @see #startCDATA
	 */
	public void endCDATA() throws SAXException {
		if (mParsedObject != null) mParsedObject.endCDATA();
	}

	/**
	 * Returns the min storage variable
	 * @return the min storage variable
	 */
	public int getMinStorage() {
		return mMinStorage;
	}
}