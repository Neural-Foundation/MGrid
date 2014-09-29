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
import p2p.basic.Key;
import pgrid.PGridKey;
import pgrid.Properties;
import pgrid.XMLIndexEntry;
import pgrid.XMLizable;
import pgrid.IndexEntry;
import pgrid.core.index.CSVIndexTable;
import pgrid.core.index.IndexManager;
import pgrid.core.index.DBIndexTable;
import pgrid.interfaces.basic.PGridP2P;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * This class represents a data modifier message.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class IndexModifierMessage extends PGridMessageImp {

	 /**
	  *  read the default values from ini file
	  */
		private static PGridP2P mPGridP2P = PGridP2P.sharedInstance();	
		private static final String TYPE_NAME = mPGridP2P.propertyString(Properties.TYPE_NAME);	
		private static final int PORT_NUMBER = Integer.parseInt(mPGridP2P.propertyString(Properties.PORT_NUMBER));
	
	/**
	 * A part of the XML string.
	 */
	public static final String XML_INDEXMODIFIER = "IndexModifier";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_INDEXMODIFIER_KEY = "Key";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_INDEXMODIFIER_MODE = "Mode";

	private static int num = 0;
	/**
	 * Data items to insert vector.
	 */
	//private Collection mIndexItems = new Vector();
//	private DBIndexTable mItems = new DBIndexTable();
	private CSVIndexTable mItems;

	/**
	 * The Storage Manager.
	 */
	private IndexManager mIndexManager = PGridP2P.sharedInstance().getIndexManager();

	/**
	 * The common key of all data items.
	 */
	private Key mKey = null;

	/**
	 * The temporary variable during parsing.
	 */
	private XMLizable mParsedObject = null;

	/**
	 * Inser, update or delete
	 */
	private short mMode = -1;

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_INDEXMODIFIER;
	}

	/**
	 * Default constructor
	 */
	public IndexModifierMessage() {
		mItems = new CSVIndexTable("IM_"+(++num)+".csv",false);
	}


	/**
	 * Creates an empty update message.
	 *
	 * @param header the message header.
	 */
	public IndexModifierMessage(MessageHeader header) {
		super(header);
		mItems = new CSVIndexTable("IM_Recv_"+(++num)+".csv",false);
	}

	/**
	 * Create a copy of this message.
	 *
	 * @return a copy of this message.
	 */
	public Object clone() {
		IndexModifierMessage msg = (IndexModifierMessage) super.clone();

//		msg.mItems = new DBIndexTable();
		msg.mItems = new CSVIndexTable("IM_Clone_"+(++num)+".csv",false);
//		msg.mItems.duplicate(mItems);

		return msg;
	}

	/**
	 * Creates a new data modifier message with given values.
	 *
	 * @param guid      the guid of the query.
	 * @param key       the common prefix of all data items.
	 * @param mode		the mode of the modification: insert, update or delete
	 * @param dataItems the data items.
	 */
	public IndexModifierMessage(pgrid.GUID guid, Key key, short mode, Collection dataItems) {
		super(guid);
		mItems = new CSVIndexTable("IM_"+(++num)+".csv",false);
		mKey = key;
		try{
			mItems.openFileForWriting();
			for (Object dataItem : dataItems) {
				mItems.addIndexEntry((IndexEntry)dataItem);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			mItems.closeFileForWriting();
		}
//		mItems.addAll(dataItems);
		mMode = mode;
	}


	/**
	 * Creates a new data modifier message with given values.
	 *
	 * @param guid      the guid of the query.
	 * @param key       the common prefix of all data items.
	 * @param mode		the mode of the modification: insert, update or delete
	 * @param dataItems the data items.
	 */
	public IndexModifierMessage(pgrid.GUID guid, Key key, short mode, CSVIndexTable csvItems) {
		super(guid);
		mItems = new CSVIndexTable("IM_"+(++num)+".csv",false);
		mKey = key;
		try{
			csvItems.openFileForReading();
			mItems.openFileForWriting();
			IndexEntry dataItem = null;
			while((dataItem = (IndexEntry)csvItems.getNextIndexEntry())!=null) {
				mItems.addIndexEntry(dataItem);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			mItems.closeFileForWriting();
			csvItems.closeFileOnReading();
		}
//		mItems.addAll(dataItems);
		mMode = mode;
	}

//	/**
//	* Creates a new data modifier message with given values.
//	*
//	* @param guid      the guid of the query.
//	* @param key       the common prefix of all data items.
//	* @param mode		the mode of the modification: insert, update or delete
//	* @param dataItems the data items.
//	*/
//	public IndexModifierMessage(pgrid.GUID guid, Key key, short mode, DBIndexTable dataItems) {
//	super(guid);
//	mKey = key;
//	mItems.addAll(dataItems);
//	mMode = mode;
//	}

	/**
	 * Returns the dataitems as a collection.
	 *
	 * @return the dataitems.
	 */
	public Collection<IndexEntry> getDataItems() {
		Collection<IndexEntry> tmpItems = new ArrayList<IndexEntry>();
		try {
			mItems.openFileForReading();
			IndexEntry ie = null;
			while((ie = (IndexEntry)mItems.getNextIndexEntry()) != null){
				tmpItems.add(ie);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			mItems.closeFileOnReading();
		}

		return tmpItems;
	}
	public Collection getIndexItemsByLevel(int level) {
		// return index item per level
		return null;
	}
//	public DBIndexTable getIndexTable(){
//	return mItems;
//	}
	public CSVIndexTable getIndexTable(){
		return mItems;
	}



	/**
	 * Returns the key representing the common prefix of all data items.
	 *
	 * @return the key.
	 */
	public Key getKey() {
		return mKey;
	}

	/**
	 * return the distribution mode
	 * @return the distribution mode
	 */
	public short getMode() {
		return mMode;
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
		if (mKey == null)
			return false;
		if (mItems == null)
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
		if (qName.equals(XMLIndexEntry.XML_INDEX_ITEM) && mParsedObject != null) {
			mParsedObject.endElement(uri, lName, qName);
//			mItems.sequentialAdd((IndexEntry) mParsedObject);
			try {
				mItems.openFileForWriting();
				mItems.addIndexEntry((IndexEntry) mParsedObject);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally{
				mItems.closeFileForWriting();
			}

			mParsedObject = null;
		} else if (mParsedObject != null) {
			mParsedObject.endElement(uri, lName, qName);
		} if (qName.equals(XML_INDEXMODIFIER)) {
//			mItems.flushInsert();
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
		if (qName.equals(XML_INDEXMODIFIER)) {
			mKey = new PGridKey(attrs.getValue(XML_INDEXMODIFIER_KEY));
			mMode = Short.parseShort(attrs.getValue(XML_INDEXMODIFIER_MODE));
		} else if (mParsedObject != null) {
			mParsedObject.startElement(uri, lName, qName, attrs);
		} else if (qName.equals(XMLIndexEntry.XML_INDEX_ITEM)) {
			if (mParsedObject == null)
				mParsedObject = (XMLIndexEntry) mIndexManager.createIndexEntry(TYPE_NAME);
			else ((XMLIndexEntry)mParsedObject).clear();

			mParsedObject.startElement(uri, lName, qName, attrs);
		} else if (mParsedObject != null) mParsedObject.startElement(uri, lName, qName, attrs);

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
		int size = 128;

		strBuff = new StringBuffer(size);

		strBuff.append(prefix + XML_ELEMENT_OPEN + XML_INDEXMODIFIER); // {prefix}<DataModifier
		//strBuff.append(XML_SPACE + XML_INDEXMODIFIER_GUID + XML_ATTR_OPEN + mGUID.toString() + XML_ATTR_CLOSE); // _GUID="GUID"
		strBuff.append(XML_SPACE + XML_INDEXMODIFIER_KEY + XML_ATTR_OPEN + mKey + XML_ATTR_CLOSE); // _Key="KEY"
		strBuff.append(XML_SPACE + XML_INDEXMODIFIER_MODE + XML_ATTR_OPEN + mMode + XML_ATTR_CLOSE); // _Mode="MODE"
		strBuff.append(XML_ELEMENT_CLOSE + newLine); // >{newLine}

		strBuff.append(mItems.getIndexEntriesAsXML());

		strBuff.append(prefix + XML_ELEMENT_OPEN_END + XML_INDEXMODIFIER + XML_ELEMENT_CLOSE + newLine); // {prefix}</Insert>{newLine}
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

	protected void finalize() throws Throwable {
		try {
			if (mItems != null) mItems.delete();
		} finally {
			super.finalize();
		}
	}

}