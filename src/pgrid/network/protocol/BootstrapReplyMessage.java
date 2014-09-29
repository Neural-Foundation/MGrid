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
import pgrid.XMLizable;
import pgrid.PGridHost;
import pgrid.core.XMLRoutingTable;

/**
 * This class represents a P-Grid bootstrap reply message.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class BootstrapReplyMessage extends PGridMessageImp {

	/**
	 * A part of the XML string.
	 */
	public static final String XML_BOOTSTRAP_CONSTRUCTION_DELAY = "ConstructionDelay";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_BOOTSTRAP_REPLICATION_DELAY = "ReplicationDelay";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_BOOTSTRAP_REPLY = "BootstrapReply";

	/**
	 * The bootstrap construction delay.
	 */
	private long mConstructionDelay = -1;

	/**
	 * The temporary variable during parsing.
	 */
	private XMLizable mParsedObject = null;

	/**
	 * The bootstrap replication delay.
	 */
	private long mReplicationDelay = -1;

	/**
	 * The routing table.
	 */
	private XMLRoutingTable mRoutingTable = null;

	/**
	 * Default constructor
	 */
	public BootstrapReplyMessage() {
		super();
	}


	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_BOOTSTRAP_REPLY;
	}

	/**
	 * Creates a new PGrid bootstrap reply message with the given header.
	 *
	 * @param header the message header.
	 */
	public BootstrapReplyMessage(MessageHeader header) {
		super(header);
	}

	/**
	 * Creates a new bootstrap reply with given values for bootstrapping.
	 *
	 * @param host         the creating host.
	 * @param routingTable the Routing Table of the creating host.
	 */
	public BootstrapReplyMessage(PGridHost host, XMLRoutingTable routingTable) {
		super();
		getHeader().setHost(host);
		mRoutingTable = routingTable;
	}

	/**
	 * Creates a new bootstrap reply with given values for bootstrapping.
	 *
	 * @param host         the creating host.
	 * @param routingTable the Routing Table of the creating host.
	 * @param replicationDelay the replication delay.
	 * @param constructionDelay the construction delay.
	 */
	public BootstrapReplyMessage(PGridHost host, XMLRoutingTable routingTable, long replicationDelay, long constructionDelay) {
		super();
		getHeader().setHost(host);
		mRoutingTable = routingTable;
		mConstructionDelay = constructionDelay;
		mReplicationDelay = replicationDelay;
	}

	/**
	 * Returns the construction delay.
	 *
	 * @return the delay.
	 */
	public long getConstructionDelay() {
		return mConstructionDelay;
	}

	/**
	 * Returns the construction delay.
	 *
	 * @return the delay.
	 */
	public long getReplicationDelay() {
		return mReplicationDelay;
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
		return isValid();
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
		if (qName.equals(XML_BOOTSTRAP_REPLY)) {
			String tmp = attrs.getValue(XML_BOOTSTRAP_CONSTRUCTION_DELAY);
			if (tmp != null)
				mConstructionDelay = Long.parseLong(tmp);
			tmp = attrs.getValue(XML_BOOTSTRAP_REPLICATION_DELAY);
			if (tmp != null)
				mReplicationDelay = Long.parseLong(tmp);
		} else if (qName.equals(XMLRoutingTable.XML_ROUTING_TABLE)) {
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
		strBuff.append(prefix).append(XML_ELEMENT_OPEN).append(XML_BOOTSTRAP_REPLY); // {prefix}<BootstrapReply
		if ((mConstructionDelay != -1) && (mReplicationDelay != -1)) {
			strBuff.append(XML_SPACE).append(XML_BOOTSTRAP_REPLICATION_DELAY).append(XML_ATTR_OPEN).append(mReplicationDelay).append(XML_ATTR_CLOSE); // ReplicationDelay="REPLICATION_DELAY"
			strBuff.append(XML_SPACE).append(XML_BOOTSTRAP_CONSTRUCTION_DELAY).append(XML_ATTR_OPEN).append(mConstructionDelay).append(XML_ATTR_CLOSE); // ConstructionDelay="CONSTRUCTION_DELAY"
		}
		if (mRoutingTable == null) {
			strBuff.append(XML_ELEMENT_END_CLOSE).append(newLine);
		} else {
			strBuff.append(XML_ELEMENT_CLOSE).append(newLine); // >{newLine}
			strBuff.append(mRoutingTable.toXMLString(new StringBuffer().append(prefix).append(XML_TAB).toString(), newLine, true, false, false));
			strBuff.append(prefix).append(XML_ELEMENT_OPEN_END).append(XML_BOOTSTRAP_REPLY).append(XML_ELEMENT_CLOSE).append(newLine); // {prefix}</BootstrapReply>{newLine}
		}
		return strBuff.toString();
	}

}