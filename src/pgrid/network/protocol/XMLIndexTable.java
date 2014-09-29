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
import pgrid.*;
import pgrid.core.index.DBIndexTable;
import pgrid.core.index.Signature;
import pgrid.core.index.IndexManager;
import pgrid.interfaces.basic.PGridP2P;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.ArrayList;

/**
 * This class extends the {@link pgrid.core.index.IndexTable} with XML functionality.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
//@todo restrict access level	
public class XMLIndexTable implements XMLizable, Cloneable {

	 /**
	  *  read the default values from ini file
	  */
		private static PGridP2P mPGridP2P = PGridP2P.sharedInstance();	
		private static final String TYPE_NAME = mPGridP2P.propertyString(Properties.TYPE_NAME);	
		private static final int PORT_NUMBER = Integer.parseInt(mPGridP2P.propertyString(Properties.PORT_NUMBER));
	
	/**
	 * A part of the XML string.
	 */
	public static final String XML_INDEX_TABLE = "IndexTable";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_INDEX_TABLE_SIGNATURE = "Signature";

	/**
	 * The represented data table.
	 */
	private IndexManager mIndexManager = null;

	/**
	 * The data items.
	 */
	private Collection mIndexItems = null;

	/**
	 * The data table.
	 */
	private DBIndexTable mIndexTable = null;

	/**
	 * The temporary variable during parsing.
	 */
	private XMLizable mParsedObject = null;

	/**
	 * The signature of the data table.
	 */
	private Signature mSignature = null;

	/**
	 * The temporary variable during parsing.
	 */
	private ArrayList mTmpIndexItems = null;

	/**
	 * Construct a new Data Table.
	 * @param dataTable the data table.
	 */
	public XMLIndexTable(DBIndexTable dataTable) {
		mIndexManager = PGridP2P.sharedInstance().getIndexManager();
		mIndexTable = dataTable;
		mSignature = mIndexTable.getSignature();
	}

	/**
	 * Construct a new Data Table.
	 * @param dataItems the data items.
	 * @param sign the data table signature.
	 */
	public XMLIndexTable(Collection dataItems, Signature sign) {
		mIndexManager = PGridP2P.sharedInstance().getIndexManager();
		mIndexItems = dataItems;
		mSignature = sign;
	}

	/**
	 * Construct a new Data Table.
	 * @param dataItems the data items.
	 * @param sign the data table signature.
	 */
	public XMLIndexTable(DBIndexTable dataItems, Signature sign) {
		mIndexManager = PGridP2P.sharedInstance().getIndexManager();
		mIndexTable = dataItems;
		mSignature = sign;
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
		if (qName.equals(XML_INDEX_TABLE)) {
			mParsedObject = null;
			mIndexTable.flushInsert();
			mIndexItems = mTmpIndexItems;
			mIndexTable.setSignature(mSignature);
		} else if (mParsedObject != null && qName.equals(XMLIndexEntry.XML_INDEX_ITEM)) {
			mParsedObject.endElement(uri, lName, qName);
			
			mIndexTable.sequentialAdd((IndexEntry) mParsedObject);
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
	public void startElement(String uri, String lName, String qName, Attributes attrs) throws SAXException {
		if (qName.equals(XML_INDEX_TABLE)) {
			// Data Table
			String sign = attrs.getValue(XML_INDEX_TABLE_SIGNATURE);
			if (sign != null) {
				mSignature = new Signature(sign);
			} else {
				Constants.LOGGER.warning("Routing table without signature!");
			}
			mTmpIndexItems = new ArrayList();
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
	 * Returns the XML representation of this object.
	 *
	 * @return the XML string.
	 */
	public String toXMLString() {
		return toXMLString("", XML_NEW_LINE);
	}

	/**
	 * Returns the XML representation of this object.
	 *
	 * @param prefix    the prefix string for all new lines.
	 * @param newLine   the new line string, e.g., \n.
	 * @return the XML string.
	 */
	public String toXMLString(String prefix, String newLine) {
		if ((prefix == null) || (newLine == null))
			throw new NullPointerException();

		StringBuffer strBuff;
		strBuff = new StringBuffer(100);

		strBuff.append(prefix + XML_ELEMENT_OPEN + XML_INDEX_TABLE); // {prefix}<RoutingTable
		strBuff.append(XML_SPACE + XML_INDEX_TABLE_SIGNATURE + XML_ATTR_OPEN + mSignature.toString() + XML_ATTR_CLOSE); // _Signature="SIGNATURE"

		if (mIndexTable != null) {
			strBuff.append(XML_ELEMENT_CLOSE + newLine); // >{newLine}
			strBuff.append(mIndexTable.getIndexEntriesAsXML());
			strBuff.append(prefix + XML_ELEMENT_OPEN_END + XML_INDEX_TABLE + XML_ELEMENT_CLOSE + newLine); // {prefix}</RoutingTable>{newLine}
		} else strBuff.append(XML_ELEMENT_END_CLOSE + newLine); // />{newLine}

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

}
