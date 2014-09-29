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
/**
 * $Id: IndexModifierMessage.java,v 1.2 2005/11/07 16:56:38 rschmidt Exp $
 *
 * Copyright (c) 2002 The P-Grid Team,
 *                    All Rights Reserved.
 *
 * This file is part of the P-Grid package.
 * P-Grid homepage: http://www.p-grid.org/
 *
 * The P-Grid package is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This package is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file LICENSE.
 * If not you can find the GPL at http://www.gnu.org/copyleft/gpl.html
 */

package pgrid.network.protocol;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import p2p.basic.Key;
import pgrid.Constants;
import pgrid.PGridKey;
import pgrid.XMLIndexEntry;
import pgrid.XMLizable;
import pgrid.IndexEntry;
import pgrid.core.index.CSVIndexTable;
import pgrid.core.index.IndexManager;
import pgrid.core.index.DBIndexTable;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.interfaces.utils.IFileStreamingMessage;
import pgrid.util.Compression;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

/**
 * This class represents a data modifier message.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class IndexModifierCSVMessage extends PGridMessageImp implements IFileStreamingMessage{

	/**
	 * A part of the XML string.
	 */
	public static final String XML_INDEXMODIFIER = "IndexModifierCSV";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_INDEXMODIFIER_KEY = "Key";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_INDEXMODIFIER_MODE = "Mode";

	public static final String XML_CSV_FILE_NAME = "FileName";

	public static final String XML_CSV_FILE_SIZE = "FileLength";


	private static int num = 0;
	/**
	 * Data items to insert vector.
	 */
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

	private long mFileSize;

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
	public IndexModifierCSVMessage() {
	}


	/**
	 * Creates an empty update message.
	 *
	 * @param header the message header.
	 */
	public IndexModifierCSVMessage(MessageHeader header) {
		super(header);
	}

	/**
	 * Create a copy of this message.
	 *
	 * @return a copy of this message.
	 */
	public Object clone() {
		IndexModifierCSVMessage msg = (IndexModifierCSVMessage) super.clone();
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
	public IndexModifierCSVMessage(pgrid.GUID guid, Key key, short mode, CSVIndexTable csvItems) {
		super(guid);
		mItems = new CSVIndexTable("IMcsv_"+randomString()+"_"+(++num)+".csv",false);
		mKey = key;
		if(csvItems != null){
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
		}
		mMode = mode;

		Compression.compressFile(getFileName(),getFileName()+".zip" );
		mFileSize = new File(getFilePath()+".zip").length();
		this.getHeader().setAdditionalAttribute("FileLength", getFileSize()+"");
		this.getHeader().setAdditionalAttribute("FileName", getFileName());

	}

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
//			mParsedObject.endElement(uri, lName, qName);
////			mItems.sequentialAdd((IndexEntry) mParsedObject);
//			try {
//			mItems.openFileForWriting();
//			mItems.addIndexEntry((IndexEntry) mParsedObject);
//			} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			} finally{
//			mItems.closeFileForWriting();
//			}

//			mParsedObject = null;
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
			mItems = new CSVIndexTable(attrs.getValue(XML_CSV_FILE_NAME),true);
		} else if (mParsedObject != null) {
			mParsedObject.startElement(uri, lName, qName, attrs);
		} else if (qName.equals(XMLIndexEntry.XML_INDEX_ITEM)) {
//			if (mParsedObject == null)
//			mParsedObject = (XMLIndexEntry) mIndexManager.createIndexEntry(attrs.getValue(XMLIndexEntry.XML_INDEX_ITEM_TYPE));
//			else ((XMLIndexEntry)mParsedObject).clear();

//			mParsedObject.startElement(uri, lName, qName, attrs);
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
		strBuff.append(XML_SPACE + XML_CSV_FILE_NAME + XML_ATTR_OPEN + getFileName() + XML_ATTR_CLOSE +	XML_SPACE + XML_CSV_FILE_SIZE + XML_ATTR_OPEN + getFileSize() + XML_ATTR_CLOSE);
		strBuff.append(XML_ELEMENT_CLOSE + newLine); // >{newLine}

//		strBuff.append(mItems.getIndexEntriesAsXML());

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
			if (mItems != null){
				Constants.LOGGER.finest("Deleting CSVFile : "+mItems.getJustFileName());
				mItems.delete();
			}
			File compressedFile = new File(getFilePath()+".zip");
			if(compressedFile.exists()){
				Constants.LOGGER.finest("Deleting CSVFile : "+compressedFile.getName());
				compressedFile.delete();
			}
		} finally {
			super.finalize();
		}
	}

	public String getFileName() {
		// TODO Auto-generated method stub
		return mItems.getJustFileName();
	}

	public String getFilePath() {
		// TODO Auto-generated method stub
		return Constants.CSV_DIR+getFileName();
	}

	public long getFileSize() {
		// TODO Auto-generated method stub
		return mFileSize;
	}

	public void notifyEnd() {
		// TODO Auto-generated method stub

	}

	String randomString(){
		String s="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		int len=6;
		char[] c = new char[len];
		Random r = new Random();
		for (int i = 0; i < len; i++) {
			c[i]=s.charAt(r.nextInt(1000)%s.length());
		}
		return new String(c);
	}

	private CSVIndexTable getMItems() {
		return mItems;
	}

}