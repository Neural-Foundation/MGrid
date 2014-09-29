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
import pgrid.Constants;

public class GetFile extends PGridMessageImp {

	/**
	 * A part of the XML string.
	 */
	public static final String XML_GET_FILE = "GetFile";
	public static final String XML_FILE_NAME = "FileName";
	private String mFileName;
	
	public GetFile(){
		
	}
	
	public GetFile(MessageHeader header){
		super(header);
	}
	
	public GetFile(String fileName){
		mFileName = fileName;
	}

	@Override
	protected String getXMLMessageName() {
		return XML_GET_FILE;
	}

	public String getFileName() {
		return mFileName;
	}

	public String getFilePath() {
		return Constants.DOWNLOAD_DIR + mFileName;
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

		strBuff.append(prefix + XML_ELEMENT_OPEN + XML_GET_FILE + XML_SPACE + XML_FILE_NAME + XML_ATTR_OPEN + mFileName + XML_ATTR_CLOSE); // {prefix}<Replicate current total
		strBuff.append(XML_ELEMENT_CLOSE + newLine); // >{newLine}
		strBuff.append(prefix + XML_ELEMENT_OPEN_END + XML_GET_FILE + XML_ELEMENT_CLOSE + newLine); // {prefix}</Replicate>{newLine}
		
		return strBuff.toString();
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
		if (qName.equals(XML_GET_FILE)) {
			// Sanitize the file name in order to avoid directory traversal
			mFileName = attrs.getValue(XML_FILE_NAME).replaceAll("\\.\\./", "");;
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
		GetFile msg =  (GetFile) super.clone();
		return msg;
	}
}
