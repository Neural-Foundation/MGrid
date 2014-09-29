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
import pgrid.network.PGridMessageMapping;
import pgrid.util.LexicalDefaultHandler;

import java.io.UnsupportedEncodingException;

/**
 * This class represents a P-Grid challenge response message.
 *
 * @author Renault John
 * @version 1.0.0
 */
public class ChallengeReplyMessage extends PGridMessageImp {

	/**
	 * A part of the XML string.
	 */
	public static final String XML_CHALLENGE_REPLY = "ChallengeReply";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_CHALLENGE_REPLY_GUID = "GUID";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_CHALLENGE_REPLY_RESPONSE = "Response";

	/**
	 * Challenge string
	 */
	protected String mResponse = null;

	/**
	 * Default constructor
	 */
	public ChallengeReplyMessage() {
	}

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_CHALLENGE_REPLY;
	}

	/**
	 * Constructor of a challenge - response scheme message
	 *
	 * @param msgHeader
	 */
	public ChallengeReplyMessage(MessageHeader msgHeader) {
		super(msgHeader);
	}

	/**
	 * Constructor of an challenge - response scheme message
	 *
	 * @param guid the challenge identifier.
	 * @param challenge the challenge code.
	 */
	public ChallengeReplyMessage(GUID guid, String challenge) {
		super();
		getHeader().addReference(guid);
		mResponse = challenge;
	}

	/**
	 * Returns the response string
	 *
	 * @return the response string
	 */
	public String getResponse() {
		return mResponse;
	}

	/**
	 * Tests if this init response message is valid.
	 *
	 * @return <code>true</code> if valid.
	 */
	public boolean isValid() {
		if (!super.isValid()) {
			return false;
		}
		if (mResponse == null)
			return false;
		return true;
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
		if (qName.equals(XML_CHALLENGE_REPLY)) {
			mResponse = attrs.getValue(XML_CHALLENGE_REPLY_RESPONSE);
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
		return prefix + XML_ELEMENT_OPEN + XML_CHALLENGE_REPLY + // {prefix}<ACK
				//XML_SPACE + XML_CHALLENGE_REPLY_GUID + XML_ATTR_OPEN + mGUID.toString() + XML_ATTR_CLOSE + // _GUID="GUID"
				XML_SPACE + XML_CHALLENGE_REPLY_RESPONSE + XML_ATTR_OPEN + mResponse + XML_ATTR_CLOSE + // _Challenge="Challenge"
				XML_ELEMENT_END_CLOSE + newLine; // />{newLine}
	}

}
