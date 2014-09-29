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

import pgrid.Properties;
import pgrid.QueryReply;
import pgrid.XMLIndexEntry;
import pgrid.XMLizable;
import pgrid.core.index.IndexManager;
import pgrid.interfaces.basic.PGridP2P;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import p2p.basic.GUID;

/**
 * This class represents a query reply.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class QueryReplyMessage extends PGridMessageImp {

	 /**
	  *  read the default values from ini file
	  */
		private static PGridP2P mPGridP2P = PGridP2P.sharedInstance();	
		private static final String TYPE_NAME = mPGridP2P.propertyString(Properties.TYPE_NAME);	
		private static final int PORT_NUMBER = Integer.parseInt(mPGridP2P.propertyString(Properties.PORT_NUMBER));
	
	/**
	 * The Query Hit reply code Bad Request.
	 */
	private static final int CODE_BAD_REQUEST = 400;

	/**
	 * The Query Hit reply code File Not Found.
	 */
	private static final int CODE_NOT_FOUND = 404;

	/**
	 * The Query Hit reply code OK.
	 */
	private static final int CODE_OK = 200;

	/**
	 * A part of the XML string.
	 */
	public static final String XML_QUERY_REPLY = "QueryReply";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_QUERY_REPLY_CODE = "Code";
	
	/**
	 * A part of the XML string.
	 */
	private static final String XML_QUERY_REPLY_HITS="QueryReplyHits";
	
	/**
	 * A part of the XML string.
	 */
	private static final String XML_HITS="Hits";

	/**
	 * The Query Hit reply code.
	 */
	private int mCode = -1;

	/**
	 * The temporary variable during parsing.
	 */
	private XMLizable mParsedObject = null;
	
	/**
	 * A temp string used during parsing a data item.
	 */
	
	private String mTmpString = null;
	/**
	 * Query reply object
	 */
	QueryReply mQueryReply = null;

	/**
	 * Data items manager
	 */
	private IndexManager mIndexManager = null;

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_QUERY_REPLY;
	}         

	/**
	 * Default constructor
	 */
	public QueryReplyMessage() {
	}

	/**
	 * Creates a query reply message with the given header.
	 *
	 * @param header the message header.
	 */
	public QueryReplyMessage(MessageHeader header) {
		super(header);
		mIndexManager = PGridP2P.sharedInstance().getIndexManager();
		mQueryReply = new QueryReply();
	}

	/**
	 * Creates a new query reply message with given values.
	 *
	 * @param guid      the GUID of the Query Reply.
	 * @param type      the query reply type.
	 * @param resultSet the result set of found files.
	 */
	public QueryReplyMessage(p2p.basic.GUID guid, int type, Collection resultSet) {
	
	/*super(guid);
	
		mQueryReply = new QueryReply(guid, type, resultSet);
	
		mIndexManager = PGridP2P.sharedInstance().getIndexManager();

		if (type == QueryReply.TYPE_OK)
			mCode = CODE_OK;
		else if (type == QueryReply.TYPE_NOT_FOUND)
			mCode = CODE_NOT_FOUND;*/
	}


	/**
	 * Creates a new query reply message with given values.
	 *
	 * @param guid      the GUID of the Query Reply.
	 * @param type      the query reply type.
	 * @param hits 		 the number of query reply hits
	 */
	public QueryReplyMessage(p2p.basic.GUID guid, int type, int hits) {
		super(guid);
		mQueryReply = new QueryReply(guid, type, hits);
		mIndexManager = PGridP2P.sharedInstance().getIndexManager();

		if (type == QueryReply.TYPE_OK) {
					mCode = CODE_OK;
		}
				else if (type == QueryReply.TYPE_NOT_FOUND)
			mCode = CODE_NOT_FOUND;
	}


	/**
	 * Tests if this query hit message is valid.
	 *
	 * @return <code>true</code> if valid.
	 */
/*	public boolean isValid() {
		if (!super.isValid()) {
			return false;
		}
		if (mCode == -1)
			return false;
		if ((mCode == CODE_OK) && (mQueryReply.getResultSet() == null))
			return false;
		if ((mCode >= 0) && (mCode != CODE_OK) && (mQueryReply.getResultSet() != null))
			return false;
		return true;
	}*/
	
	public boolean isValid() {
		if (!super.isValid()) {
			return false;
		}
		if (mCode == -1)
			return false;
		if ((mCode == CODE_OK) )
			return false;
		if ((mCode >= 0) && (mCode != CODE_OK) )
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
				// Query Reply Result
				mQueryReply.getResultSet().add(mParsedObject);
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
		if (qName.equals(XML_QUERY_REPLY)) {
			// Query Reply
			mCode = Integer.parseInt(attrs.getValue(XML_QUERY_REPLY_CODE));
			if (mCode == CODE_OK)
				mQueryReply.setType(QueryReply.TYPE_OK);
			else if (mCode == CODE_NOT_FOUND)
				mQueryReply.setType(QueryReply.TYPE_NOT_FOUND);
			else if (mCode == CODE_BAD_REQUEST)
				mQueryReply.setType(QueryReply.TYPE_BAD_REQUEST);
			if (mCode == CODE_OK) {
				mQueryReply.setResultSet(new Vector());
			}
		} else if (qName.equals(XMLIndexEntry.XML_INDEX_ITEM)) {
			// Query Reply Result
		//	mParsedObject = (XMLIndexEntry) mIndexManager.createIndexEntry(TYPE_NAME);
		//	mParsedObject.startElement(uri, lName, qName, attrs);
		} else if (qName.equals(XML_QUERY_REPLY_HITS)) {
	
			mQueryReply.setmHits(Integer.parseInt(attrs.getValue(XML_HITS)));
		}
		else if (mParsedObject != null) {
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
		strBuff = new StringBuffer(500);
		strBuff.append(prefix + XML_ELEMENT_OPEN + XML_QUERY_REPLY); // {prefix}<QueryReply
		strBuff.append(XML_SPACE + XML_QUERY_REPLY_CODE + XML_ATTR_OPEN + mCode + XML_ATTR_CLOSE); // _Code="CODE"
		
	
		if (mQueryReply.getmHits() != 0) {
			
			strBuff.append(XML_ELEMENT_CLOSE + newLine); // >{newLine}
		
			strBuff.append(prefix + XML_ELEMENT_OPEN + XML_QUERY_REPLY_HITS + XML_SPACE+ XML_HITS
					+ XML_ATTR_OPEN + mQueryReply.getmHits()+ XML_ATTR_CLOSE +  XML_ELEMENT_END_CLOSE +newLine);
			strBuff.append(prefix + XML_ELEMENT_OPEN_END + XML_QUERY_REPLY + XML_ELEMENT_CLOSE + newLine); // {prefix}</QueryReply>{newLine}
		} else {
			strBuff.append(XML_ELEMENT_END_CLOSE + newLine); // />{newLine}
		}
	
		return strBuff.toString();
	}
	/**
	 * Returns the XML representation of this object.
	 *
	 * @param prefix  the XML prefix before each element in a new line.
	 * @param newLine the new line string.
	 * @return the XML string.
	 *//*
	public String toXMLString(String prefix, String newLine) {
		StringBuffer strBuff;

		if (mQueryReply.getResultSet() == null) {
	
			strBuff = new StringBuffer(100);
		}
		else
			
			
		strBuff = new StringBuffer(mQueryReply.getResultSet().size() * 500);
		strBuff.append(prefix + XML_ELEMENT_OPEN + XML_QUERY_REPLY); // {prefix}<QueryReply
		strBuff.append(XML_SPACE + XML_QUERY_REPLY_CODE + XML_ATTR_OPEN + mCode + XML_ATTR_CLOSE); // _Code="CODE"
		
	
		if (mQueryReply.getResultSet() != null) {
			
			strBuff.append(XML_ELEMENT_CLOSE + newLine); // >{newLine}
		
			strBuff.append(prefix + XML_ELEMENT_OPEN + XML_QUERY_REPLY_HITS + XML_ELEMENT_CLOSE);
			strBuff.append(mQueryReply.getResultSet().size());
			System.out.println("res "+mQueryReply.getResultSet().size());
			strBuff.append(XML_ELEMENT_OPEN_END + XML_QUERY_REPLY_HITS + XML_ELEMENT_CLOSE + newLine);
		//	for (Iterator it = mQueryReply.getResultSet().iterator(); it.hasNext();) {
		//		strBuff.append(((XMLIndexEntry)it.next()).toXMLString(prefix + XML_TAB, newLine));		}
			strBuff.append(prefix + XML_ELEMENT_OPEN_END + XML_QUERY_REPLY + XML_ELEMENT_CLOSE + newLine); // {prefix}</QueryReply>{newLine}
		} else {
			strBuff.append(XML_ELEMENT_END_CLOSE + newLine); // />{newLine}
		}
	
		return strBuff.toString();
	}*/
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
	 * Returns the message GUID to which this message refere to.
	 * @return  the message GUID to which this message refere to.
	 */
	public GUID getReferencedMsgGUID() {
		return getHeader().getReferences().iterator().next();
	}

	/**
	 * Returns the buisiness logic object
	 * @return the buisiness logic object
	 */
	public QueryReply getQueryReply() {
		return  mQueryReply;
	}

	/**
	 * Create a copy of this message.
	 *
	 * @return a copy of this message.
	 */
	public Object clone() {
		QueryReplyMessage msg = (QueryReplyMessage) super.clone();
		msg.mQueryReply = new QueryReply();
		return msg;
	}
}