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

import java.io.UnsupportedEncodingException;

/**
 * This class represents a P-Grid peer query message.
 *
 * @author Renault John
 * @version 1.0.0
 */
public class PeerLookupMessage extends PGridMessageImp {
	/**
	 * A part of the XML string.
	 */
	public static final String XML_PEERLOOKUP = "LookupPeer";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_PEERLOOKUP_PATH = "Path";

	/**
	 * look for the smallest peer greater or equal to the given path
	 */
	public static final int RIGHT_MOST = 0;

	/**
	 * look for the greatest peer smaller or equal to the given path
	 */
	public static final int LEFT_MOST = 1;

	/**
	 * look for a peer responsible for the path
	 */
	public static final int ANY = 2;

	/**
	 * The searcher host
	 */
	protected XMLPGridHost mInitialHost = null;

	/**
	 * Path of the searched peer
	 */
	protected String mPath = "";

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_PEERLOOKUP;
	}

	/**
	 * Default constructor
	 */
	public PeerLookupMessage() {
	}

	/**
	 * Constructor of a PeerLookupMessage
	 *
	 * @param msgHeader
	 */
	public PeerLookupMessage(MessageHeader msgHeader) {
		super(msgHeader);
	}

	/**
	 * Constructor of a PeerLookupMessage
	 */
	public PeerLookupMessage(String path, PGridHost host) {
		super();
		mInitialHost = new XMLPGridHost(host);
		mPath = path;
	}

	/**
	 * Returns the host
	 *
	 * @return the host
	 */
	public PGridHost getInitialHost() {
		return mInitialHost.getHost();
	}

	/**
	 * Returns the path
	 *
	 * @return the path
	 */
	public String getPath() {
		return mPath;
	}

	/**
	 * set the path
	 */
	public void setPath(String path) {
		mPath = path;
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
			mPath = attrs.getValue(XML_PEERLOOKUP_PATH);
			mPath = mPath.substring(0, mPath.length());
		} else if (qName.equals(XMLPGridHost.XML_HOST)) {
			// Host
			mInitialHost = XMLPGridHost.getXMLHost(qName, attrs);
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
				XML_SPACE + XML_PEERLOOKUP_PATH + XML_ATTR_OPEN + mPath + XML_ATTR_CLOSE + XML_ELEMENT_CLOSE + newLine + // _Index="Index"
				mInitialHost.toXMLString(prefix + XML_TAB, newLine, false) + // <Host .../>
				prefix + XML_ELEMENT_OPEN_END + XML_PEERLOOKUP + XML_ELEMENT_CLOSE + newLine; // </PeerQuery>

		return xmlMessage;
	}

}
