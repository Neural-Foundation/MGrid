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
import pgrid.interfaces.basic.PGridP2P;
import pgrid.Properties;
import pgrid.XMLIndexEntry;
import pgrid.XMLizable;
import pgrid.core.index.IndexManager;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * This class represents a Gridella replica message.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class ReplicateMessage extends PGridMessageImp {

	 /**
	  *  read the default values from ini file
	  */
		private static PGridP2P mPGridP2P = PGridP2P.sharedInstance();	
		private static final String TYPE_NAME = mPGridP2P.propertyString(Properties.TYPE_NAME);	
		private static final int PORT_NUMBER = Integer.parseInt(mPGridP2P.propertyString(Properties.PORT_NUMBER));
	
	/**
	 * A part of the XML string.
	 */
	public static final String XML_REPLICATE = "Replicate";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_REPLICATE_GUID = "GUID";

	/**
	 * Data items vector.
	 */
	private Collection mDataItems = new Vector();

	/**
	 * The temporary variable during parsing.
	 */
	private XMLizable mParsedObject = null;

	/**
	 * The storage manager.
	 */
	private IndexManager mIndexManager = null;

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_REPLICATE;
	}

	/**
	 * Default constructor
	 */
	public ReplicateMessage() {
	}

	/**
	 * Creates an empty replicate message.
	 *
	 * @param header the message header.
	 */
	public ReplicateMessage(MessageHeader header) {
		super(header);
		mIndexManager = PGridP2P.sharedInstance().getIndexManager();
	}

	/**
	 * Creates a new replicate message with given values.
	 *
	 * @param dataItems the data items.
	 */
	public ReplicateMessage(Collection dataItems) {
		super();
		mIndexManager = PGridP2P.sharedInstance().getIndexManager();
		mDataItems = dataItems;
	}

	/**
	 * Create a copy of this message.
	 *
	 * @return a copy of this message.
	 */
	public Object clone() {
		ReplicateMessage msg =  (ReplicateMessage) super.clone();
		msg.mDataItems = (Collection) ((Vector)mDataItems).clone();

		return msg;
	}

	/**
	 * Returns the dataitems collection.
	 *
	 * @return the dataitems.
	 */
	public Collection getIndexEntries() {
		return mDataItems;
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
		if (mDataItems.size() == 0)
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
	 * @throws SAXException any SAX exception, possibly wrapping another exception.
	 */
	public void endElement(String uri, String lName, String qName) throws SAXException {
		if (qName.equals(XMLIndexEntry.XML_INDEX_ITEM)) {
			mDataItems.add(mParsedObject);
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
	 * @throws org.xml.sax.SAXException any SAX exception, possibly wrapping another exception.
	 */
	public void startElement(String uri, String lName, String qName, Attributes attrs) throws SAXException {
		if (qName.equals(XML_REPLICATE)) {

		} else if (qName.equals(XMLIndexEntry.XML_INDEX_ITEM)) {
			// TODO get rid of the createDataItem()
			mParsedObject = (XMLIndexEntry) mIndexManager.createIndexEntry(TYPE_NAME);
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
		StringBuffer strBuff;
		if (mDataItems == null)
			strBuff = new StringBuffer(100);
		else
			strBuff = new StringBuffer(mDataItems.size() * 100);
		strBuff.append(prefix + XML_ELEMENT_OPEN + XML_REPLICATE); // {prefix}<Replicate
		strBuff.append(XML_ELEMENT_CLOSE + newLine); // >{newLine}
		for (Iterator it = mDataItems.iterator(); it.hasNext();) {
			strBuff.append(((XMLIndexEntry)it.next()).toXMLString(prefix + XML_TAB, newLine)); // {prefix}\t<IndexEntry ...>{newLine}
		}
		strBuff.append(prefix + XML_ELEMENT_OPEN_END + XML_REPLICATE + XML_ELEMENT_CLOSE + newLine); // {prefix}</Replicate>{newLine}
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

}