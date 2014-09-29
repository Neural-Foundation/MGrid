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

import pgrid.*;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.core.index.IndexManager;
import pgrid.core.index.DBIndexTable;
import pgrid.core.index.DBView;

import java.util.Collection;
import java.util.Vector;
import java.util.Iterator;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

/**
 * This class represents an exchange index entries message message.
 *
 * @author @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */
public class ExchangeIndexEntriesMessage extends PGridMessageImp {

	 /**
	  *  read the default values from ini file
	  */
		private static PGridP2P mPGridP2P = PGridP2P.sharedInstance();	
		private static final String TYPE_NAME = mPGridP2P.propertyString(Properties.TYPE_NAME);	
		private static final int PORT_NUMBER = Integer.parseInt(mPGridP2P.propertyString(Properties.PORT_NUMBER));
	
	/**
	 * A part of the XML string.
	 */
	public static final String XML_EXCHANGE_ENTRIES = "ExchangeEntries";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_EXCHANGE_ENTRIES_TOTAL = "Total";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_EXCHANGE_ENTRIES_CURR = "Current";

	/**
	 * Data items vector.
	 */
	private String mXMLString = null;

	/**
	 * Data items vector.
	 */
	private DBView mDataItems = null;

	/**
	 * The temporary variable during parsing.
	 */
	private XMLizable mParsedObject = null;

	/**
	 * The storage manager.
	 */
	private IndexManager mIndexManager = null;


	/**
	 * total number of messages
	 */
	private short mTotal = 1;

	/**
	 * Current message
	 */
	private short mCurrent = 0;

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_EXCHANGE_ENTRIES;
	}

	/**
	 * Default constructor
	 */
	public ExchangeIndexEntriesMessage() {
	}

	/**
	 * Creates an empty replicate message.
	 *
	 * @param header the message header.
	 */
	public ExchangeIndexEntriesMessage(MessageHeader header) {
		super(header);
		mIndexManager = PGridP2P.sharedInstance().getIndexManager();
	}

	/**
	 * Creates a new replicate message with given values.
	 *
	 * @param dataItems the data items.
	 */
	public ExchangeIndexEntriesMessage(GUID guid, DBView dataItems, short current, short total) {
		super(guid);
		mIndexManager = PGridP2P.sharedInstance().getIndexManager();
		mDataItems = dataItems;
		mTotal = total;
		mCurrent = current;
	}

	/**
	 * Creates a new replicate message with given values.
	 *
	 */
	public ExchangeIndexEntriesMessage(GUID guid) {
		super(guid);
		mIndexManager = PGridP2P.sharedInstance().getIndexManager();
		mDataItems = null;
		mTotal = 0;
		mCurrent = 0;
	}


	/**
	 * Create a copy of this message.
	 *
	 * @return a copy of this message.
	 */
	public Object clone() {
		ExchangeIndexEntriesMessage msg =  (ExchangeIndexEntriesMessage) super.clone();

		return msg;
	}

	/**
	 * Tests if this init response message is valid.
	 *
	 * @return <code>true</code> if valid.
	 */
	public boolean isValid() {
		if (!super.isValid()) {
			return false;
		}
		if (mDataItems == null)
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
	 * @throws org.xml.sax.SAXException any SAX exception, possibly wrapping another exception.
	 */
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (mParsedObject != null)
			mParsedObject.characters(ch, start, length);
	}

	/**
	 * The SAX parser will invoke this method at the end of every element in the XML document; there will be a
	 * corresponding startElement event for every endElement event (even when the element is empty).
	 *
	 * @param uri   the Namespace URI.
	 * @param lName the local name (without prefix), or the empty string if Namespace processing is not being performed.
	 * @param qName the qualified name (with prefix), or the empty string if qualified names are not available.
	 * @throws org.xml.sax.SAXException any SAX exception, possibly wrapping another exception.
	 */
	public void endElement(String uri, String lName, String qName) throws SAXException {
		if (qName.equals(XMLIndexEntry.XML_INDEX_ITEM) && mParsedObject != null) {
			mParsedObject.endElement(uri, lName, qName);
			PGridP2P.sharedInstance().getIndexManager().getIndexTable().sequentialAdd((IndexEntry) mParsedObject);

		} else if (mParsedObject != null) {
			mParsedObject.endElement(uri, lName, qName);
		} if (qName.equals(XML_EXCHANGE_ENTRIES)) {
			PGridP2P.sharedInstance().getIndexManager().getIndexTable().flushInsert();
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
	 * @throws org.xml.sax.SAXException any SAX exception, possibly wrapping another exception.
	 */
	public void startElement(String uri, String lName, String qName, Attributes attrs) throws SAXException {
		if (qName.equals(XML_EXCHANGE_ENTRIES)) {
			mTotal = Short.parseShort(attrs.getValue(XML_EXCHANGE_ENTRIES_TOTAL));
			mCurrent = Short.parseShort(attrs.getValue(XML_EXCHANGE_ENTRIES_CURR));
		} else if (mParsedObject != null) {
			mParsedObject.startElement(uri, lName, qName, attrs);
		} else if (qName.equals(XMLIndexEntry.XML_INDEX_ITEM)) {
			if (mParsedObject == null)
				mParsedObject = (XMLIndexEntry) mIndexManager.createIndexEntry(TYPE_NAME);
			else ((XMLIndexEntry)mParsedObject).clear();
			mParsedObject.startElement(uri, lName, qName, attrs);
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
		if (mXMLString == null) {
			StringBuffer strBuff;
			strBuff = new StringBuffer(100);

			strBuff.append(prefix + XML_ELEMENT_OPEN + XML_EXCHANGE_ENTRIES + XML_SPACE + XML_EXCHANGE_ENTRIES_CURR + XML_ATTR_OPEN + mCurrent + XML_ATTR_CLOSE +
					XML_SPACE + XML_EXCHANGE_ENTRIES_TOTAL + XML_ATTR_OPEN + mTotal + XML_ATTR_CLOSE); // {prefix}<Replicate current total
			if (mDataItems != null) {
				strBuff.append(XML_ELEMENT_CLOSE + newLine); // >{newLine}
				strBuff.append(mDataItems.getIndexEntriesAsXML()); // {prefix}\t{newLine}
				strBuff.append(prefix + XML_ELEMENT_OPEN_END + XML_EXCHANGE_ENTRIES + XML_ELEMENT_CLOSE + newLine); // {prefix}</Replicate>{newLine}
			} else {
				strBuff.append(XML_ELEMENT_END_CLOSE + newLine);
				strBuff.append(prefix + "<!-- No data or data already processed -->" + newLine);
			}
			mXMLString = strBuff.toString();
		}
		return mXMLString;
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
		mCDataSection = true;
		if (mParsedObject != null) mParsedObject.startCDATA();
	}

	/**
	 * Report the end of a CDATA section.
	 *
	 * @throws org.xml.sax.SAXException The application may raise an exception.
	 * @see #startCDATA
	 */
	public void endCDATA() throws SAXException {
		mCDataSection = false;
		if (mParsedObject != null) mParsedObject.endCDATA();
	}

	/**
	 * Returns the expected number of messages in the current EXCHANGE cycle
	 * @return
	 */
	public short getTotal() {
		return mTotal;
	}

	/**
	 * Returns the current index of message in the current EXCHANGE cycle
	 * @return
	 */
	public short getCurrent() {
		return mCurrent;
	}


}
