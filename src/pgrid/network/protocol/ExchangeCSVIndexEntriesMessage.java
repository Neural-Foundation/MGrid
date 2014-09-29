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

import java.io.File;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import pgrid.Constants;
import pgrid.XMLIndexEntry;
import pgrid.interfaces.utils.IFileStreamingMessage;
import pgrid.util.Compression;

public class ExchangeCSVIndexEntriesMessage extends PGridMessageImp implements IFileStreamingMessage {

	/**
	 * A part of the XML string.
	 */
	public static final String XML_EXCHANGE_CSV_ENTRIES = "ExchangeCSVEntries";
	public static final String XML_CSV_FILE_NAME = "csvFileName";
	public static final String XML_CSV_FILE_SIZE = "csvFileSize";
	
	
	private String mFileName;
	private String mComprFileName;
	private String mComprFilePath;
	private long mComprFileSize;
	
	private String mXMLString; 
	
	public ExchangeCSVIndexEntriesMessage(){
		
	}
	
	public ExchangeCSVIndexEntriesMessage(MessageHeader header){
		super(header);
	}
	
	public ExchangeCSVIndexEntriesMessage(String fileName){
		mFileName = fileName;

		mComprFileName = mFileName+".zip";
		mComprFilePath = Constants.CSV_DIR+mComprFileName;
		Compression.compressFile(fileName,mComprFileName );
		mComprFileSize = new File(mComprFilePath).length();

		this.getHeader().setAdditionalAttribute("FileLength", mComprFileSize+"");
		this.getHeader().setAdditionalAttribute("FileName", mFileName);
	}

	@Override
	protected String getXMLMessageName() {
		return XML_EXCHANGE_CSV_ENTRIES;
	}

	public String getFileName() {
		return mFileName;
	}

	public String getFilePath() {
		return Constants.CSV_DIR+mFileName;
	}

	public long getFileSize() {
		return mComprFileSize;
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

			strBuff.append(prefix + XML_ELEMENT_OPEN + XML_EXCHANGE_CSV_ENTRIES + XML_SPACE + XML_CSV_FILE_NAME + XML_ATTR_OPEN + mFileName + XML_ATTR_CLOSE +
					XML_SPACE + XML_CSV_FILE_SIZE + XML_ATTR_OPEN + mComprFileSize + XML_ATTR_CLOSE); // {prefix}<Replicate current total
			strBuff.append(XML_ELEMENT_CLOSE + newLine); // >{newLine}
			strBuff.append(prefix + XML_ELEMENT_OPEN_END + XML_EXCHANGE_CSV_ENTRIES + XML_ELEMENT_CLOSE + newLine); // {prefix}</Replicate>{newLine}
			mXMLString = strBuff.toString();
		}
		return mXMLString;
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
		if (qName.equals(XML_EXCHANGE_CSV_ENTRIES)) {
			mFileName = attrs.getValue(XML_CSV_FILE_NAME);
//			mComprFileName = mFileName;
//			mComprFilePath = Constants.CSV_DIR+mComprFileName;
			mComprFileSize = Long.parseLong(attrs.getValue(XML_CSV_FILE_SIZE));
		} 
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * Create a copy of this message.
	 *
	 * @return a copy of this message.
	 */
	public Object clone() {
		ExchangeCSVIndexEntriesMessage msg =  (ExchangeCSVIndexEntriesMessage) super.clone();

		return msg;
	}

	public void notifyEnd() {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * CleanUp the csvFiles 
	 */
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		super.finalize();
		try {
			File csvFile = new File(getFilePath());
			if(csvFile.exists()){
				csvFile.delete();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		
	}
}
