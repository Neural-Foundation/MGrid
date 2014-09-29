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
import pgrid.Constants;
import pgrid.Exchange;
import pgrid.PGridHost;
import pgrid.XMLizable;
import pgrid.core.XMLRoutingTable;
import pgrid.core.index.CSVIndexTable;
import pgrid.core.index.DBIndexTable;
import pgrid.core.index.Signature;
import pgrid.core.index.TempDBIndexTable;
import pgrid.interfaces.utils.IFileStreamingMessage;
import pgrid.util.Compression;

import java.io.File;
import java.util.Collection;

/**
 * This class represents an exchange reply message.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class ExchangeReplyMessage extends PGridMessageImp implements IFileStreamingMessage {

	/**
	 * A part of the XML string.
	 */
	public static final String XML_EXCHANGE_REPLY = "ExchangeReply";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_EXCHANGE_REPLY_GUID = "GUID";

	private static final String XML_CSV_FILE_NAME = "csvFileName";
	private static final String XML_CSV_FILE_SIZE = "csvFileSize";
	private static final String XML_CSV_SIGNATURE = "csvFileSignature";

	
	/**
	 * A part of the XML string.
	 */
	private static final String XML_EXCHANGE_REPLY_LEN_CURRENT = "CurrentLength";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_EXCHANGE_REPLY_RANDOM_NUMBER = "RandomNumber";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_EXCHANGE_REPLY_RECURSION = "Recursion";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_EXCHANGE_REPLY_MINSTORAGE = "MinStorage";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_EXCHANGE_REPLY_REPLICA_ESTIMATE = "ReplicaEstimate";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_EXCHANGE_SEP_DATA = "Splitted";

	/**
	 * true if data is sent in separeted messages
	 */
	private boolean mSeparatedIndexEntries = false;

	/**
	 * The exchange message.
	 */
	private Exchange mExchange = null;

	/**
	 * The temporary variable during parsing.
	 */
	private XMLizable mParsedObject = null;

	/**
	 * The data table as XML.
	 */
	private XMLIndexTable mXMLIndexTable = null;

	/**
	 * The data table as CSV.
	 */
	private CSVIndexTable mCSVIndexTable = null;

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_EXCHANGE_REPLY;
	}

	/**
	 * Creates a new PGridP2P Exchange message with the given header.
	 *
	 * @param header the message header.
	 */
	public ExchangeReplyMessage(MessageHeader header) {
		super(header);
		mExchange = new Exchange();
		mExchange.setHost(getHeader().getHost());
	}

	/**
	 * Default constructor
	 */
	public ExchangeReplyMessage() {
	}

	/**
	 * Creates a new exchange message with given values.
	 *
	 * @param guid         the message guid.
	 * @param host         the message creating host.
	 * @param recursion    the recursion.
	 * @param lCurrent     the current common length.
	 * @param replicaEst   the replication estimate.
	 * @param routingTable the Routing Table for this host.
	 * @param dataItems    the list of data items.
	 * @param sign         the data table signature.
	 * @param separatedIndexEntries   true if data is sent in seperated index entries messages
	 */
	public ExchangeReplyMessage(GUID guid, PGridHost host, int recursion, int lCurrent, int minStorage, double replicaEst,
			XMLRoutingTable routingTable, DBIndexTable dataItems, Signature sign, boolean separatedIndexEntries) {
		super(guid);
		getHeader().setHost(host);
		mExchange = new Exchange(guid, recursion, lCurrent, minStorage, replicaEst, routingTable, dataItems);
		mXMLIndexTable = new XMLIndexTable(dataItems, sign);
	}

	/**
	 * Creates a new exchange message with given values.
	 *
	 * @param guid         the message guid.
	 * @param host         the message creating host.
	 * @param recursion    the recursion.
	 * @param lCurrent     the current common length.
	 * @param replicaEst   the replication estimate.
	 * @param routingTable the Routing Table for this host.
	 * @param dataItems    the list of data items.
	 * @param sign         the data table signature.
	 * @param separatedIndexEntries   true if data is sent in seperated index entries messages
	 */
	public ExchangeReplyMessage(GUID guid, PGridHost host, int recursion, int lCurrent, int minStorage, double replicaEst,
			XMLRoutingTable routingTable, CSVIndexTable csvdataItems, Signature sign, boolean separatedIndexEntries) {
		super(guid);
		getHeader().setHost(host);
		mExchange = new Exchange(guid, recursion, lCurrent, minStorage, replicaEst, routingTable, csvdataItems);
		mSignature = sign.toString();

		mCSVIndexTable = csvdataItems;

		mFileName = mCSVIndexTable.getJustFileName();
		String mComprFileName = mFileName+".zip";
		String mComprFilePath = Constants.CSV_DIR+mComprFileName;
		Compression.compressFile(mFileName,mComprFileName );
		mFileSize = new File(mComprFilePath).length();

		this.getHeader().setAdditionalAttribute("FileLength", mFileSize+"");
		this.getHeader().setAdditionalAttribute("FileName", mFileName);

	}

	/**
	 * Create a copy of this message.
	 *
	 * @return a copy of this message.
	 */
	public Object clone() {
		ExchangeReplyMessage msg = (ExchangeReplyMessage) super.clone();
		msg.mExchange = new Exchange(mExchange.getGUID(), mExchange.getRecursion(),
				mExchange.getLenCurrent(), mExchange.getMinStorage(), mExchange.getReplicaEstimate(),
				mExchange.getRoutingTable(), mCSVIndexTable);

		return msg;
	}

	/**
	 * Tests if this init response message is valid.
	 *
	 * @return <code>true</code> if valid.
	 */
	public boolean isValid() {

		return super.isValid();
	}

	/**
	 * Returns the exchange GUID.
	 *
	 * @return the message GUID.
	 */
	public GUID getGUID() {
		return mExchange.getGUID();  //To change body of implemented methods use File | Settings | File Templates.
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
		if (qName.equals(XML_EXCHANGE_REPLY)) {
			// Exchange
			String guidStr = attrs.getValue(XML_EXCHANGE_REPLY_GUID);
			if (guidStr != null)
				mExchange.setGUID(new pgrid.GUID(attrs.getValue(XML_EXCHANGE_REPLY_GUID)));
			
			mFileName = attrs.getValue(XML_CSV_FILE_NAME);
			mExchange.setIndexTable(new CSVIndexTable(mFileName));
			mFileSize = Long.parseLong(attrs.getValue(XML_CSV_FILE_SIZE));
			mSignature = attrs.getValue(XML_CSV_SIGNATURE);
			
			String recStr = attrs.getValue(XML_EXCHANGE_REPLY_RECURSION);
			if (recStr == null)
				mExchange.setRecursion(0);
			else
				mExchange.setRecursion(Integer.parseInt(recStr));
			String lCurrStr = attrs.getValue(XML_EXCHANGE_REPLY_LEN_CURRENT);
			if (lCurrStr == null)
				mExchange.setLenCurrent(0);
			else
				mExchange.setLenCurrent(Integer.parseInt(lCurrStr));
			String minStorage = attrs.getValue(XML_EXCHANGE_REPLY_MINSTORAGE);
			if (minStorage == null)
				mExchange.setMinStorage(0);
			else
				mExchange.setMinStorage(Integer.parseInt(minStorage));
			String rndNmbrStr = attrs.getValue(XML_EXCHANGE_REPLY_RANDOM_NUMBER);
			if (rndNmbrStr == null)
				mExchange.setRandomNumber(Double.MIN_VALUE);
			else
				mExchange.setRandomNumber(Double.parseDouble(rndNmbrStr));
			String replicaEstStr = attrs.getValue(XML_EXCHANGE_REPLY_REPLICA_ESTIMATE);
			if (replicaEstStr == null)
				mExchange.setReplicaEstimate(0);
			else
				mExchange.setReplicaEstimate(Double.parseDouble(replicaEstStr));
			String splitted = attrs.getValue(XML_EXCHANGE_SEP_DATA);
			if (splitted != null)
				mSeparatedIndexEntries = Boolean.valueOf(splitted).booleanValue();
		} else if (qName.equals(XMLRoutingTable.XML_ROUTING_TABLE)) {
			mExchange.setRoutingTable(new XMLRoutingTable());
			mExchange.getRoutingTable().startElement(uri, lName, qName, attrs);
			mParsedObject = mExchange.getRoutingTable();
		} else if (qName.equals(XMLIndexTable.XML_INDEX_TABLE)) {
//			mExchange.setIndexTable(new DBIndexTable(mExchange.getHost()));
			mExchange.setIndexTable(new TempDBIndexTable(mExchange.getHost()));
			mXMLIndexTable = new XMLIndexTable(mExchange.getIndexTable());
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
	public synchronized String toXMLString() {
		return toXMLString(XML_TAB, XML_NEW_LINE);
	}

	/**
	 * Returns the XML representation of this object.
	 *
	 * @param prefix  the XML prefix before each element in a new line.
	 * @param newLine the new line string.
	 * @return the XML string.
	 */
	public synchronized String toXMLString(String prefix, String newLine) {
		StringBuffer strBuff;
		strBuff = new StringBuffer(100);
		strBuff.append(prefix + XML_ELEMENT_OPEN + XML_EXCHANGE_REPLY); // {prefix}<Exchange
		strBuff.append(XML_SPACE + XML_EXCHANGE_REPLY_GUID + XML_ATTR_OPEN + mExchange.getGUID().toString() + XML_ATTR_CLOSE); // _GUID="GUID"
		strBuff.append(XML_SPACE + XML_CSV_FILE_NAME + XML_ATTR_OPEN + mFileName + XML_ATTR_CLOSE +
					XML_SPACE + XML_CSV_FILE_SIZE + XML_ATTR_OPEN + mFileSize + XML_ATTR_CLOSE);
		strBuff.append(XML_SPACE + XML_CSV_SIGNATURE + XML_ATTR_OPEN + mSignature + XML_ATTR_CLOSE);
		if (mExchange.getRandomNumber() != Double.MIN_VALUE)
			strBuff.append(XML_SPACE + XML_EXCHANGE_REPLY_RANDOM_NUMBER + XML_ATTR_OPEN + String.valueOf(mExchange.getRandomNumber()) + XML_ATTR_CLOSE); // _RandomNumber="RANDOM NUMBER"
		strBuff.append(XML_SPACE + XML_EXCHANGE_REPLY_RECURSION + XML_ATTR_OPEN + mExchange.getRecursion() + XML_ATTR_CLOSE); // _Recursion="RECURSION"
		strBuff.append(XML_SPACE + XML_EXCHANGE_REPLY_LEN_CURRENT + XML_ATTR_OPEN + mExchange.getLenCurrent() + XML_ATTR_CLOSE); // _CurrentLength="LEN_CURRENT"
		strBuff.append(XML_SPACE + XML_EXCHANGE_REPLY_MINSTORAGE + XML_ATTR_OPEN + mExchange.getMinStorage() + XML_ATTR_CLOSE); // _MinStorage="MinStorage"
		strBuff.append(XML_SPACE + XML_EXCHANGE_REPLY_REPLICA_ESTIMATE + XML_ATTR_OPEN + mExchange.getReplicaEstimate() + XML_ATTR_CLOSE); // _ReplicaEstimate="REPLICA_ESTIMATE"
		strBuff.append(XML_SPACE + XML_EXCHANGE_SEP_DATA + XML_ATTR_OPEN + mSeparatedIndexEntries + XML_ATTR_CLOSE);
		strBuff.append(XML_ELEMENT_CLOSE + newLine); // >{newLine}

		// routing table
		strBuff.append(mExchange.getRoutingTable().toXMLString(prefix + XML_TAB, newLine, true, true, true));

//		// data table
//		strBuff.append(mXMLIndexTable.toXMLString(prefix + XML_TAB, newLine)); 
		
		//@todo deal with TableSignature

		strBuff.append(prefix + XML_ELEMENT_OPEN_END + XML_EXCHANGE_REPLY + XML_ELEMENT_CLOSE + newLine); // {prefix}</Exchange>{newLine}
		return strBuff.toString();
	}

	/**
	 * Get the message content.
	 *
	 * @return a binary representation of the message
	 */
	public byte[] getData() {
		return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
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
	 * Return the exchange buisiness logic object.
	 * @return the exchange buisiness logic object.
	 */
	public Exchange getExchange() {
		return mExchange;
	}


	/**
	 * Returns true if index entries are not included in this message but will be sent in separated messages.
	 * @return true if index entries are not included in this message but will be sent in separated messages.
	 */
	public boolean hasSeparetedDataMessages() {
		return mSeparatedIndexEntries;
	}

	private String mFileName;
	private long mFileSize;
	private String mSignature;
	
	public String getFileName() {
		// TODO Auto-generated method stub
		return mFileName;
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
		mExchange.setIndexTable(new TempDBIndexTable(mExchange.getHost()));
		// TODO Auto-generated method stub
		mExchange.getIndexTable().setSignature(new Signature(mSignature));
		mCSVIndexTable = new CSVIndexTable(mFileName);
		try {
			mCSVIndexTable.openFileForReading();
			String line = null;
			while((line = mCSVIndexTable.getNextLineNoCheck()) != null){
				try {
					((TempDBIndexTable)mExchange.getIndexTable()).sequentialAdd(line.split(",")[0],line.split(",")[1]);
				} catch (Exception e) {
					// TODO: handle exception
					System.err.println("line : "+line);
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}finally{
			mCSVIndexTable.closeFileOnReading();
			mCSVIndexTable.delete(); //Deleting the receivedRandomSubset
		}

		
	}

}