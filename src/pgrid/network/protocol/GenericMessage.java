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
import p2p.basic.KeyRange;
import p2p.basic.Key;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.*;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * This class represents a P-Grid generic message to be used with the P2P interface.
 *
 * @author @author <a href="mailto:Roman Schmidt <Renault.John@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */
public class GenericMessage extends PGridMessageImp {

	/**
	 * A part of the XML string.
	 */
	public static final String XML_GENERIC = "Generic";

	/**
	 * The originating host.
	 */
	protected XMLPGridHost mHost = null;

	/**
	 * The data.
	 */
	protected byte[] mData = null;

	/**
	 * Byte array representaiton of data
	 */
	protected ArrayList<Byte> mByteArray = new ArrayList<Byte>();

	/**
	 * Creates an empty info message.
	 *
	 * @param header the message header.
	 */
	public GenericMessage(MessageHeader header) {
		super(header);
	}

	/**
	 * Default constructor
	 */
	public GenericMessage() {
	}

	/**
	 * Create a copy of this message.
	 *
	 * @return a copy of this message.
	 */
	public Object clone() {
		GenericMessage msg = (GenericMessage) super.clone();

		msg.mByteArray = (ArrayList<Byte>) (mByteArray == null?null:mByteArray.clone());
		if (mData!=null) {
			msg.mData = mData.clone();
		}
		return msg;
	}

	/**
	 * Creates a new generic message with given values.
	 *
	 * @param data the generic message content.
	 */
	public GenericMessage(byte[] data) {
		this(PGridP2P.sharedInstance().getLocalHost(), data);
	}

	/**
	 * Creates a new generic message with given values.
	 *
	 * @param host the message originator.
	 * @param data the generic message content.
	 */
	public GenericMessage(PGridHost host, byte[] data) {
		super();
		mHost = new XMLPGridHost(host);
		mData = data;
		for (int i=0; i<data.length; i++) {
			mByteArray.add(mData[i]);
		}
	}

	/**
	 * Returns the query message as array of bytes.
	 *
	 * @return the message bytes.
	 */
	public byte[] getBytes() {
		byte[] leading=null;
		byte[] data = getData();
		byte[] ending=null;

		try {
			leading = leadingToXMLString(XML_TAB, XML_NEW_LINE).getBytes("UTF-8");
			ending = endingToXMLString(XML_TAB, XML_NEW_LINE).getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
		byte[] bytes = new byte[leading.length+data.length+ending.length];
		System.arraycopy(leading,0,bytes,0,leading.length);
		System.arraycopy(data,0,bytes,leading.length,data.length);
		System.arraycopy(ending,0,bytes,leading.length+data.length,ending.length);

		return bytes;
	}

	/**
	 * Returns the originating host.
	 *
	 * @return the host.
	 */
	public PGridHost getHost() {
		return mHost.getHost();
	}

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_GENERIC;
	}

	/**
	 * Returns the data.
	 *
	 * @return the data.
	 */
	public byte[] getData() {
		if (mData == null) {
			Byte[] bytes=null;

			bytes = mByteArray.toArray(new Byte[mByteArray.size()]);
			mData = new byte[bytes.length];

			for (int i=0; i<bytes.length;i++) {
				mData[i] = bytes[i];
			}
		}
		return mData;
	}

	/**
	 * Tests if this init response message is valid.
	 *
	 * @return <code>true</code> if valid.
	 */
	public boolean isValid() {
		super.isValid();

		if (mHost == null)
			return false;
		if (mData == null)
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
		if (parsingCDATA())
			for(int i = 0; i < length; i++) {
				mByteArray.add((byte)ch[start+i]);
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
		
		if (qName.equals(XML_GENERIC)) {
			// Info
		} else if (qName.equals(XMLPGridHost.XML_HOST)) {
			// the originating host
			mHost = XMLPGridHost.getXMLHost(qName, attrs);
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
	 * Returns a string represantation of this message.
	 *
	 * @return a string represantation of this message.
	 */
	protected String leadingToXMLString(String prefix, String newLine) {
		return prefix + XML_ELEMENT_OPEN + XML_GENERIC + XML_ELEMENT_CLOSE + newLine + // >{newLine}
				mHost.toXMLString(prefix + XML_TAB, newLine, false) + // <Host .../>{newLine}
				prefix + XML_TAB + XML_CDATA_OPEN;
	}

	/**
	 * Returns a string represantation of this message.
	 *
	 * @return a string represantation of this message.
	 */
	protected String endingToXMLString(String prefix, String newLine) {
		return XML_CDATA_CLOSE + newLine + // <![CDATA[data]]>
				prefix + XML_ELEMENT_OPEN_END + XML_GENERIC + XML_ELEMENT_CLOSE + newLine; // </GenericMessage>{newLine}
	}

	/**
	 * Returns the XML representation of this object.
	 *
	 * @param prefix  the XML prefix before each element in a new line.
	 * @param newLine the new line string.
	 * @return the XML string.
	 */
	public String toXMLString(String prefix, String newLine) {
		return leadingToXMLString(prefix, newLine) + mByteArray.toString() + endingToXMLString(prefix, newLine);
	}

}