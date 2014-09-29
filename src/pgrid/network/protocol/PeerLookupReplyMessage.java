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
import pgrid.interfaces.basic.PGridP2P;
import pgrid.XMLizable;
import pgrid.PGridHost;
import pgrid.network.PGridMessageMapping;
import pgrid.util.LexicalDefaultHandler;

import java.io.UnsupportedEncodingException;

/**
 * This class represents a P-Grid peer query message.
 *
 * @author Renault John
 * @version 1.0.0
 */
public class PeerLookupReplyMessage extends PGridMessageImp {
	/**
	 * A part of the XML string.
	 */
	public static final String XML_PEERLOOKUP = "LookupPeerReply";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_PEERLOOKUP_CODE = "Code";

	/**
	 * The acknowledgment code Bad Request.
	 */
	private static final int CODE_BAD_REQUEST = 404;

	/**
	 * The acknowledgment code OK.
	 */
	private static final int CODE_OK = 200;

	/**
	 * The Query reply type Bad Request.
	 */
	public static final int TYPE_BAD_REQUEST = 1;

	/**
	 * The Query reply type Bad Request.
	 */
	public static final int TYPE_NO_PEER_FOUNDS = 2;

	/**
	 * The Query reply type OK.
	 */
	public static final int TYPE_OK = 0;

	/**
	 * The searched host
	 */
	protected XMLPGridHost mHost = null;

	/**
	 * Type
	 */
	protected int mType;

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_PEERLOOKUP;
	}

	/**
	 * Constructor of an acknowlegdment
	 *
	 * @param msgHeader
	 */
	public PeerLookupReplyMessage(MessageHeader msgHeader) {
		super(msgHeader);
	}

	/**
	 * Default constructor
	 */
	public PeerLookupReplyMessage() {
	}

	/**
	 * Constructor of an acknowlegdment
	 */
	public PeerLookupReplyMessage(GUID guid, PGridHost host, int type, int hops) {
		super(guid);
		mHost = new XMLPGridHost(host);
		mType = type;
		getHeader().setHops(hops);
	}

	/**
	 * Returns the host
	 *
	 * @return the host
	 */
	public PGridHost getHost() {
		return mHost.getHost();
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
		if (qName.equals(XML_PEERLOOKUP)) {
			mType = Integer.parseInt(attrs.getValue(XML_PEERLOOKUP_CODE));
		} else if (qName.equals(XMLPGridHost.XML_HOST)) {
			// Host
			mHost = XMLPGridHost.getXMLHost(qName, attrs);
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
	public String toXMLString(String prefix, String newLine) {
		String xmlMessage = prefix + XML_ELEMENT_OPEN + XML_PEERLOOKUP + // {prefix}<PeerQuery
				XML_SPACE + XML_PEERLOOKUP_CODE + XML_ATTR_OPEN + mType + XML_ATTR_CLOSE + XML_ELEMENT_CLOSE + newLine + // _Code="CODE"
				mHost.toXMLString(prefix + XML_TAB, newLine, false) + // <Host .../>
				prefix + XML_ELEMENT_OPEN_END + XML_PEERLOOKUP + XML_ELEMENT_CLOSE + newLine; // </PeerQuery>

		return xmlMessage;
	}

	/**
	 * Return the type of this lookup message
	 * @return the type of this lookup message
	 */
	public int getType() {
		return mType;
	}

	/**
	 * Returns the message GUID to which this message refere to.
	 * @return  the message GUID to which this message refere to.
	 */
	public GUID getReferencedMsgGUID() {
		return getHeader().getReferences().iterator().next();
	}
}
