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
import pgrid.PGridHost;
import pgrid.XMLizable;
import pgrid.core.XMLRoutingTable;

/**
 * This class represents a P-Grid bootstrap message.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class BootstrapMessage extends PGridMessageImp {
	// TODO: add a time stamp

	/**
	 * A part of the XML string.
	 */
	public static final String XML_BOOTSTRAP = "Bootstrap";

	/**
	 * The temporary variable during parsing.
	 */
	private XMLizable mParsedObject = null;

	/**
	 * The routing table.
	 */
	private XMLRoutingTable mRoutingTable = null;

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_BOOTSTRAP;
	}

	/**
	 * Creates a new PGridP2P Exchange message.
	 */
	public BootstrapMessage() {
		super();
	}

	/**
	 * Creates a new PGridP2P Exchange message with the given header.
	 *
	 * @param header the message header.
	 */
	public BootstrapMessage(MessageHeader header) {
		super(header);
	}

	/**
	 * Creates a new exchange with given values for bootstrapping.
	 *
	 * @param host the creating host.
	 */
	public BootstrapMessage(PGridHost host) {
		super();
		getHeader().setHost(host);
	}

	/**
	 * Creates a new exchange with given values for bootstrapping.
	 *
	 * @param host the creating host.
	 * @param routingTable the routing table.
	 */
	public BootstrapMessage(PGridHost host, XMLRoutingTable routingTable) {
		this(host);
		mRoutingTable = routingTable;
	}


	/**
	 * Returns the routing table.
	 *
	 * @return the routing table.
	 */
	public XMLRoutingTable getRoutingTable() {
		return mRoutingTable;
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
		return super.isValid();
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
		if (qName.equals(XMLRoutingTable.XML_ROUTING_TABLE)) {
			mRoutingTable = new XMLRoutingTable();
			mRoutingTable.startElement(uri, lName, qName, attrs);
			mParsedObject = mRoutingTable;
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
		StringBuffer strBuff = new StringBuffer(100);

		strBuff.append(prefix).append(XML_ELEMENT_OPEN).append(XML_BOOTSTRAP); // {prefix}<Bootstrap
		if (mRoutingTable == null) {
			strBuff.append(XML_ELEMENT_END_CLOSE).append(newLine); // />{newLine}
		} else {
			strBuff.append(XML_ELEMENT_CLOSE).append(newLine); // >{newLine}
			strBuff.append(mRoutingTable.toXMLString(prefix + XML_TAB, newLine, true, false, false));
			strBuff.append(prefix).append(XML_ELEMENT_OPEN_END).append(XML_BOOTSTRAP).append(XML_ELEMENT_CLOSE).append(newLine); // {prefix}</Bootstrap>{newLine}

		}

		return strBuff.toString();
	}

}