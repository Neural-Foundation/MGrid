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
import pgrid.interfaces.basic.PGridP2P;
import pgrid.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * This class represents a P-Grid NaFTConnectionRegister message to be used with the P2P interface.
 *
 * @author @author <a href="mailto:<nicolas.bonvin@epfl.ch>">Nicolas Bonvin</a>
 * @version 1.0.0
 */
public class NaFTConnectionRegisterMessage extends PGridMessageImp {

	/**
	 * A part of the XML string.
	 */
	public static final String XML_NaFT_CONNECTION_REGISTER = "NaFTConnectionRegister";
	
	public static final String XML_EXTERNAL_HOST = "externalHost";
		
	/**
	 * The originating host.
	 */
	protected PGridHost mHost = null;
	

	/**
	 * Create a copy of this message.
	 *
	 * @return a copy of this message.
	 */
	public Object clone() {
		NaFTConnectionRegisterMessage msg = (NaFTConnectionRegisterMessage) super.clone();
		return msg;
	}

	/**
	 *  Default constructor
	 */
	public NaFTConnectionRegisterMessage(){
		mHost = PGridP2P.sharedInstance().getLocalHost();
	}
	
	public NaFTConnectionRegisterMessage(MessageHeader header){
		super(header);
	}
	

	/**
	 * Returns the originating host.
	 *
	 * @return the host.
	 */
	public PGridHost getHost() {
		return mHost;
	}
	

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_NaFT_CONNECTION_REGISTER;
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
		
		if (qName.equals(XML_NaFT_CONNECTION_REGISTER)) {
			// Info
		} else if (qName.equals(XML_EXTERNAL_HOST)) {
			// the external IP
			try {
				mHost = PGridHost.getHost(InetAddress.getByName(attrs.getValue(XMLPGridHost.XML_HOST_ADDRESS)), Integer.parseInt(attrs.getValue(XMLPGridHost.XML_HOST_PORT)));
				mHost.setExternalIP(InetAddress.getByName(attrs.getValue(XMLPGridHost.XML_HOST_ADDRESS)));
				mHost.setIP(null);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			
		}
	}

	
	private String internalHostToXMLString(String prefix, String newLine){
		StringBuffer strBuff = new StringBuffer(100);
		strBuff.append(prefix + XML_ELEMENT_OPEN + XML_EXTERNAL_HOST + // {prefix}<Host
				//XML_SPACE + XMLPGridHost.XML_HOST_GUID + XML_ATTR_OPEN + getHost().getGUID().toString() + XML_ATTR_CLOSE + // _GUID="GUID"
				XML_SPACE + XMLPGridHost.XML_HOST_ADDRESS + XML_ATTR_OPEN + getHost().getExternalIP().getHostAddress() + XML_ATTR_CLOSE + // _Address="ADDRESS"
				XML_SPACE + XMLPGridHost.XML_HOST_PORT + XML_ATTR_OPEN + getHost().getPort() + XML_ATTR_CLOSE); // _Port="PORT"
		strBuff.append(XML_ELEMENT_END_CLOSE + newLine); // />{newLine}
		return strBuff.toString();
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
		StringBuffer sb = new StringBuffer();
		
		sb.append(prefix).append(XML_ELEMENT_OPEN).append(XML_NaFT_CONNECTION_REGISTER).append(XML_ELEMENT_CLOSE).append(newLine);
		sb.append(internalHostToXMLString(prefix + prefix, newLine));
		sb.append(prefix).append(XML_ELEMENT_OPEN_END).append(XML_NaFT_CONNECTION_REGISTER).append(XML_ELEMENT_CLOSE).append(newLine);
		
		return sb.toString();
	}

}