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

/**
 * This class represents a P-Grid message.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class SearchPathMessage extends PGridMessageImp
{

	/**
	 * A part of the XML string.
	 */
	public static final String XML_SEARCH_PATH = "SearchPath";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_SEARCH_PATH_DYN_JOIN = "DynJoin";


	/**
	 * true if the host wants to do a dynamic join, false for dynamic balancing.
	 */
	private boolean mDynJoin = false;

	/**
	 * Host who initiated the query
	 */
	protected PGridHost mRequestingHost = null;

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_SEARCH_PATH;
	}

	/**
	 * Default constructor
	 */
	public SearchPathMessage() {
	}

	/**
	 * Creates an empty query hit message.
	 *
	 * @param header the message header.
	 */
	public SearchPathMessage(MessageHeader header) {
		super(header);
	}

	/**
	 * Creates a new path search message with given values.
	 *
	 * @param requestingHost Host looking for a new path
	 * @param dynJoin      true if the host wants to do a dynamic join, false for dynamic balancing.
	 */
	public SearchPathMessage(PGridHost requestingHost, boolean dynJoin) {
		super();
		mDynJoin = dynJoin;
		mRequestingHost = requestingHost;
	}

	/**
	 * true if the host wants to do a dynamic join, false for dynamic balancing.
	 * @return true if the host wants to do a dynamic join, false for dynamic balancing.
	 */
	public boolean isDynJoin(){
		return mDynJoin;
	}

	/**
	 * Set to true if the host wants to do a dynamic join, false for dynamic balancing.
	 * @param flag true if the host wants to do a dynamic join, false for dynamic balancing.
	 */
	public void setDynJoin(boolean flag) {
		mDynJoin = flag;
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
	 * Returns the requesting host
	 * @return the requesting host
	 */
	public PGridHost getRequestingHost() {
		return mRequestingHost;
	}

	/**
	 * Set the requesting host
	 * @param host new requesting host.
	 */
	public void setRequestingHost(PGridHost host) {
		mRequestingHost = host;
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
	 * @throws org.xml.sax.SAXException any SAX exception, possibly wrapping another exception.
	 */
	public void startElement(String uri, String lName, String qName, Attributes attrs) throws SAXException {
		if (qName.equals(XML_SEARCH_PATH)) {
			// Search path
			mDynJoin = Boolean.parseBoolean(attrs.getValue(XML_SEARCH_PATH_DYN_JOIN));
		} else if (qName.equals(XMLPGridHost.XML_HOST)) {
			// Host
			mRequestingHost = XMLPGridHost.getHost(qName, attrs);
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
	 * Returns the XML representation of this object.
	 *
	 * @param prefix  the XML prefix before each element in a new line.
	 * @param newLine the new line string.
	 * @return the XML string.
	 */
	public String toXMLString(String prefix, String newLine) {
		return prefix + XML_ELEMENT_OPEN + XML_SEARCH_PATH + // {prefix}<SearchPath
				XML_SPACE + XML_SEARCH_PATH_DYN_JOIN + XML_ATTR_OPEN + mDynJoin + XML_ATTR_CLOSE + XML_ELEMENT_CLOSE + newLine +// _DynJoin="DYNJOIN"
				new XMLPGridHost(mRequestingHost).toXMLString(prefix + XML_TAB, newLine, false) +  // <Host .../>{newLine}
			prefix + XML_ELEMENT_OPEN_END + XML_SEARCH_PATH + XML_ELEMENT_CLOSE + newLine; // </SearchPath>{newLine}
	}

}