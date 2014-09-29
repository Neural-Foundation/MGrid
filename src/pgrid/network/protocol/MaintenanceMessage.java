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
import pgrid.PGridHost;
import pgrid.interfaces.basic.PGridP2P;

/**
 * Class description goes here
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public class MaintenanceMessage  extends PGridMessageImp {

	/**
	 * A part of the XML string.
	 */
	public static final String XML_MAINTENANCE = "Maintenance";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_CODE = "Code";

	/**
	 * Maintenance code
	 */
	protected int mCode;

	/**
	 * Host
	 */
	protected PGridHost mHost;

	/**
	 * Peer retracted from super peer status
	 */
	public static final int CODE_NOT_SUPERPEER = 403;

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_MAINTENANCE;  //To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Default constructor
	 */
	public MaintenanceMessage() {
		super();
	}

	/**
	 * Constructor of an acknowlegdment
	 *
	 * @param msgHeader
	 */
	public MaintenanceMessage(MessageHeader msgHeader) {
		super(msgHeader);
	}

	/**
	 * Constructor of an acknowlegdment
	 *
	 * @param code the ack code.
	 */
	public MaintenanceMessage(int code) {
		super ();
		mCode = code;
		mHost = PGridP2P.sharedInstance().getLocalHost();
	}

	/**
	 * Constructor of an acknowlegdment
	 *
	 * @param guid the ack identifier.
	 * @param code the ack code.
	 */
	public MaintenanceMessage(GUID guid, int code) {
		super (guid);
		mCode = code;
		mHost = PGridP2P.sharedInstance().getLocalHost();
	}

	/**
	 * Returns a string represantation of the message.
	 *
	 * @return a string represantation of the message.
	 */
	public String toXMLString() {
		return toXMLString(XML_TAB, XML_NEW_LINE);
	}

	/**
	 * Returns host
	 * @return the host
	 */
	public PGridHost getHost() {
		return mHost;
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
	public synchronized void startElement(String uri, String lName, String qName, Attributes attrs) throws SAXException {
		if (qName.equals(XML_MAINTENANCE)) {
			//mGUID = new pgrid.GUID(attrs.getValue(XML_ACK_GUID));
			mCode = Integer.parseInt(attrs.getValue(XML_CODE));
		} else if (qName.equals(XMLPGridHost.XML_HOST)) {
			// Host
			mHost = XMLPGridHost.getHost(qName, attrs);
		}
	}

	/**
	 * Returns the XML representation of this object.
	 *
	 * @param prefix  the XML prefix before each element in a new line.
	 * @param newLine the new line string.
	 * @return the XML string.
	 */
	public String toXMLString(String prefix, String newLine) {
		String msg = prefix + XML_ELEMENT_OPEN + XML_MAINTENANCE + // {prefix}<Maintenance
				XML_SPACE + XML_CODE + XML_ATTR_OPEN + mCode + XML_ATTR_CLOSE; // _Code="CODE"
		msg += XML_ELEMENT_CLOSE + newLine; // />{newLine}
		msg += XMLPGridHost.toXMLHost(mHost).toXMLString(prefix + XML_TAB, newLine) + newLine +
			prefix + XML_ELEMENT_OPEN_END + XML_MAINTENANCE +XML_ELEMENT_CLOSE + newLine; // />{newLine}

		return msg;
	}

	/**
	 * Return maintenance message code
	 * @return maintenance message code
	 */
	public int getCode() {
		return mCode;
	}
}
