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
import pgrid.XMLizable;
import pgrid.Constants;
import pgrid.util.Tokenizer;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.io.UnsupportedEncodingException;

/**
 * This class represents a Gridella init response message. It is send as
 * response to a valid greeting message from a remote host.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class InitResponseMessage implements PGridMessage, XMLizable {

	/**
	 * A ": " string.
	 */
	private static final String COLON_SPACE = ": ";

	/**
	 * A part of the XML string.
	 */
	public static final String HEADER_COMPRESSION = "Compression";

	/**
	 * A part of the XML string.
	 */
	public static final String HEADER_GUID = "GUID";

	/**
	 * The response string.
	 */
	private static final String RESPONSE = "P-GRID OK";

	/**
	 * The additional headers.
	 */
	private Hashtable mHeaders = null;

	/**
	 * The response string.
	 */
	private String mResponse = null;

	/**
	 * Creates a new Gridella init response message with the standard response
	 * string.
	 *
	 * @param guid the GUID of the sending host.
	 */
	public InitResponseMessage(GUID guid) {
		mResponse = RESPONSE;
		setHeaderField(HEADER_GUID, guid.toString());
	}

	/**
	 * Creates a new Gridella init response message with the delivered response
	 * string.
	 *
	 * @param response the response string.
	 */
	public InitResponseMessage(String response) {
		String[] lines = Tokenizer.tokenize(response, XML_NEW_LINE);
		if (lines.length < 1)
			return;
		mResponse = new String(lines[0]);
		mHeaders = new Hashtable();
		for (int i = 1; i < lines.length; i++) {
			String key = lines[i].substring(0, lines[i].indexOf(COLON_SPACE));
			String value = lines[i].substring(lines[i].indexOf(COLON_SPACE) + COLON_SPACE.length());
			setHeaderField(key, value);
		}
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
	}

	/**
	 * Returns the init response message as array of bytes.
	 *
	 * @return the message bytes.
	 */
	public byte[] getBytes() {
		byte[] bytes=null;

		try {
			bytes = toXMLString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
		return bytes;
	}

	/**
	 * Returns a desricptor for the type of message.
	 *
	 * @return the message descriptor.
	 */
	public int getDesc() {
		return 3;
	}

	/**
	 * Returns the representation string for a descriptor of a message.
	 *
	 * @return the message descriptor string.
	 */
	public String getDescString() {
		return "Init message reply";
	}

	/**
	 * Returns the GUID of the sending host.
	 *
	 * @return the GUID.
	 */
	public GUID getGUID() {
		return null;
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
	 * Returns the message header.
	 *
	 * @return the header.
	 */
	public MessageHeader getHeader() {
		return null;
	}

	/**
	 * Set the message header.
	 *
	 * @param header the new message header
	 */
	public void setHeader(MessageHeader header) {
		// nothing
	}

	/**
	 * Returns the header field value for the given key.
	 *
	 * @param key the header field key.
	 * @return the header field value.
	 */
	public String getHeaderField(String key) {
		if (mHeaders == null)
			return null;
		return (String)mHeaders.get(key);
	}

	/**
	 * Sets a header field with the given value.
	 *
	 * @param key   the header field key.
	 * @param value the header field value.
	 */
	public void setHeaderField(String key, String value) {
		if (mHeaders == null)
			mHeaders = new Hashtable();
		mHeaders.put(key, value);
	}

	/**
	 * Returns the message length.
	 *
	 * @return the message length.
	 */
	public int getSize() {
		return toXMLString().length();
	}

	/**
	 * Tests if this init response message is valid.
	 *
	 * @return <code>true</code> if valid.
	 */
	public boolean isValid() {
		if (mResponse.equals(RESPONSE))
			return true;
		return false;
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
	}

	/**
	 * Returns a string represantation of this message.
	 *
	 * @return a string represantation of this message.
	 */
	public String toXMLString() {
		return toXMLString("", XML_NEW_LINE);
	}

	/**
	 * Returns the XML representation of this object.
	 *
	 * @param prefix  the XML prefix before each element in a new line.
	 * @param newLine the new line string.
	 * @return the XML string.
	 */
	public String toXMLString(String prefix, String newLine) {
		StringBuffer buf = new StringBuffer();
		buf.append(prefix + mResponse + newLine);
		Collection keys = mHeaders.keySet();
		for (Iterator it = keys.iterator(); it.hasNext();) {
			String key = (String)it.next();
			String value = (String)mHeaders.get(key);
			buf.append(key + COLON_SPACE + value.toString() + newLine);
		}
		buf.append(newLine + newLine);
		return buf.toString();
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
	}

	/**
	 * Report the end of a CDATA section.
	 *
	 * @throws org.xml.sax.SAXException The application may raise an exception.
	 * @see #startCDATA
	 */
	public void endCDATA() throws SAXException {
	}

	/**
	 * Create a copy of this message.
	 *
	 * @return a copy of this message.
	 */
	public Object clone() {
		PGridMessage msg = null;
		try {
			msg = (PGridMessage) super.clone();
		} catch (CloneNotSupportedException e) {
			Constants.LOGGER.warning("Message "+this.getDescString()+"is not fully clonable.");
		}

		return msg;
	}
		

}