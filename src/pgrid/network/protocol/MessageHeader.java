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
import org.xml.sax.XMLReader;
import p2p.basic.GUID;
import pgrid.Constants;
import pgrid.PGridHost;
import pgrid.XMLizable;
import pgrid.network.router.Router;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.TreeSet;
import java.util.Collection;
import java.util.logging.Level;

/**
 * This class represents a PGrid header
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class MessageHeader extends pgrid.util.LexicalDefaultHandler implements PGridMessage, XMLizable {

	/**
	 * Returns only the leading part of the header.
	 */
	public static final short LEADING_PART = 1;

	/**
	 * Returns only the ending part of the header.
	 */
	public static final short ENDING_PART = 2;

	/**
	 * A part of the XML string.
	 */
	public static final String XML_HEADER = "Header";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_HEADER_CONTENT_LENGTH = "Content-Length";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_HEADER_GUID = "GUID";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_HEADER_REFERENCE = "Reference";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_HEADER_VERSION = "Version";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_HEADER_CLIENT_ADD = "Client";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_REQUESTOR_HOST = "Requestor";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_HOPS = "Hops";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_TYPE = "Type";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_PGRID = "P-Grid";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_DELEGATE_STATUS = "Delegation";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_TO_DELEGATE = "Delegate";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_HAS_BEEND_DELEGATED = "Delegated";
	
	/**
	 * A part of the XML string.
	 */
	public static final String XML_SOURCE_HOST = "Source";
	
	/**
	 * A part of the XML string.
	 */
	public static final String XML_DESTINATION_HOST = "Destination";

	/**
	 * Different delegation status:<br/>
	 * - toBeDelegated: this message has to be routed by an other peer and then sent back to this one.
	 * - hasBeenDelegated: This message is routed by a delegate peer
	 * - direct: this message is not delegated and should not be delegated.
	 */
	public static enum DelegateStatus {toBeDelegated, hasBeenDelegated, direct}


	/**
	 * The XML closing tag for a PGridP2P message header.
	 */
	public static final String CLOSING_TAG = XML_ELEMENT_OPEN_END + XML_PGRID + XML_ELEMENT_CLOSE; // </P-Grid>

	/**
	 * If message was sent from a client and re-issued by a super peer, this field is set to the client address
	 */
	private XMLPGridHost mClientHost = null;

	/**
	 * The content length of the message.
	 */
	private int mContentLen = -1;

	/**
	 * The host.
	 */
	private XMLPGridHost mHost = null;

	
	/**
	 * If this message goes through a relay (NaFT), then the relay needs to know the source host
	 */
	private XMLPGridHost mSourceHost = null;
	
	/**
	 * If this message goes through a relay (NaFT), then the relay needs to know the final destination
	 */
	private XMLPGridHost mDestinationHost = null;
	
	/**
	 * True if parsing route header
	 */
	private boolean mParsingRouteHeader = false;

	/**
	 * Parsing variable
	 */
	private boolean mParsingRequestor = false;

	/**
	 * True if parsing client address
	 */
	private boolean mParsingClientAddress = false;

	/**
	 * The message GUIDs this message is refering to.
	 */
	private TreeSet<GUID> mReferences = null;

	/**
	 * Routing header containing routing information
	 */
	private RouteHeader mRouteHeader = null;

	/**
	 * Initiator host
	 */
	protected PGridHost mRequestorHost = null;

	/**
	 * The protocol version.
	 */
	private String mVersion = null;

	/**
	 * Suplementary Properties
	 */
	private Hashtable<String, String> mAdditionalAttributes = new Hashtable<String, String>();

	/**
	 * Number of hops
	 */
	private int mHop = 0;

	/**
	 * True iff the local super peer is used as a proxy by a client peer
	 */
	private DelegateStatus mDelegated = DelegateStatus.direct;


	/**
	 * Type of the message that holds this header
	 */
	private int mType = -1;

	/**
	 * true if the message is still compressed
	 */
	private boolean mCompressed = false;

	/**
	 * GUID of this message
	 */
	private pgrid.GUID mGUID;

	/**
	 * XML Parser used to parse this message. This field is set by PGridReader.
	 */
	private XMLReader mParser = null;

	/**
	 * Creates an empty message header.
	 */
	public MessageHeader() {
	}

	/**
	 * Creates a new message header with given values.
	 *
	 * @param version    the protocol version.
	 * @param contentLen the length of the content.
	 * @param host       the requesting host.
	 */
	public MessageHeader(String version, int contentLen, PGridHost host, int type) {
		this(version, contentLen, host, null, type, null);
	}

	/**
	 * Creates a new message header with given values.
	 *
	 * @param version    the protocol version.
	 * @param contentLen the length of the content.
	 * @param host       the requesting host.
	 * @param guid		 the message reference GUID
	 * @param type		 the message type
	 * @param requestor
	 */
	public MessageHeader(String version, int contentLen, PGridHost host, GUID guid, int type, PGridHost requestor) {
		mVersion = version;
		mContentLen = contentLen;
		mHost = new XMLPGridHost(host);
		mType = type;
		generateGUID();
		if (guid == null)
			addReference(mGUID);
		else addReference(guid);

		if (requestor != host)
			mRequestorHost = requestor;
	}

	/**
	 * Returns the message header as array of bytes.
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
	 * Returns a part of the message header as array of bytes.
	 *
	 * @param part the part to return.
	 * @return a part of the message bytes.
	 */
	public byte[] getBytes(short part) {
		byte[] bytes=null;

		try {
			bytes = toXMLString(part).getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
		return bytes;
	}

	public void setRequestorHost(PGridHost host) {
		mRequestorHost = host;
	}

	public PGridHost getRequestorHost() {
		return mRequestorHost;
	}

	/**
	 * Returns the content length of the message.
	 *
	 * @return the content length.
	 */
	public int getContentLen() {
		return mContentLen;
	}

	/**
	 * Returns a desricptor for the type of message.
	 *
	 * @return the message descriptor.
	 */
	public int getDesc() {
		return mType;
	}

	/**
	 * Returns the representation string for a descriptor of a message.
	 *
	 * @return the message descriptor string.
	 */
	public String getDescString() {
		return "Header message";
	}

	/**
	 * Returns the message GUID.
	 *
	 * @return the message GUID.
	 */
	public GUID getGUID() {
		return mGUID;
	}

	/**
	 * Set the message GUID.
	 */
	public void setGUID(GUID guid) {
		mGUID = (pgrid.GUID) guid;
	}

	/**
	 * Generate a GUID for this message GUID.
	 */
	public void generateGUID() {
		mGUID = pgrid.GUID.getGUID();
	}

	/**
	 * Returns true if this message has been broadcasted
	 * @return true if this message has been broadcasted
	 */
	public boolean isBroadcasted() {
		return (mRouteHeader != null && mRouteHeader.getStrategy().equals(Router.BROADCAST_STRATEGY));
	}

	/**
	 * true if the message byte representation is compressed
	 * @return true if the message is still compressed
	 */
	public boolean isCompressed() {
		return mCompressed;
	}

	/**
	 * Set the compression flag
	 * @param flag
	 */
	public void setCompressedFlag(boolean flag) {
		mCompressed = flag;
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
	 * Returns the creating host.
	 *
	 * @return the creating host.
	 */
	public PGridHost getHost() {
		return mHost.getHost();
	}

	/**
	 * Set the host
	 *
	 * @param host is the new host
	 */
	public void setHost(PGridHost host) {
		mHost = new XMLPGridHost(host);
	}

	
	/**
	 * Returns the source host in case of relay.
	 * @return sourceHost
	 */
	public PGridHost getSourceHost(){
		if (mSourceHost == null) return null;
		return mSourceHost.getHost();
	}
	
	/**
	 * Set the source host of the message
	 * @param sourceHost
	 */
	public void setSourceHost(PGridHost sourceHost) {
		mSourceHost = new XMLPGridHost(sourceHost);
	}
	
	/**
	 * Returns the destination host in case of relay
	 * @return destinationHost
	 */
	public PGridHost getDestinationHost() {
		if (mDestinationHost == null) return null;
		return mDestinationHost.getHost();
	}

	/**
	 * Set the destination host
	 * @param destinationHost
	 */
	public void setDestinationHost(XMLPGridHost destinationHost) {
		mDestinationHost = destinationHost;
	}

	public void setDestinationHost(PGridHost destinationHost) {
		mDestinationHost = new XMLPGridHost(destinationHost);
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
	 * Adds the reference.
	 *
	 * @param guid the GUID to add as reference.
	 */
	public void addReference(GUID guid) {
		if (mReferences == null)
			mReferences = new TreeSet<GUID>();
		if (!mReferences.contains(guid))
			mReferences.add(guid);
	}

	/**
	 * Returns the message references.
	 *
	 * @return the references.
	 */
	public Collection<GUID> getReferences() {
		return mReferences;
	}

	/**
	 * Sets the references.
	 *
	 * @param refs the references.
	 */
	public void setReferences(Collection<GUID> refs) {
		if (mReferences == null) {
			mReferences = new TreeSet<GUID>();
		} else {
			mReferences.clear();
		}

		mReferences.addAll(refs);
	}

	/**
	 * Returns the Routing header
	 * @return the Routing header
	 */
	public RouteHeader getRouteHeader() {
		return mRouteHeader;
	}

	/**
	 * Set the routing header
	 * @param routeHeader
	 */
	public void setRoutingHeader(RouteHeader routeHeader) {
		this.mRouteHeader = routeHeader;
	}


	/**
	 * Returns the message length.
	 *
	 * @return the message length.
	 */
	public int getSize() {
		return getBytes().length;
	}

	/**
	 * Returns the protocol version.
	 *
	 * @return the protocol version.
	 */
	public String getVersion() {
		return mVersion;
	}

	/**
	 * Tests if this message header is valid.
	 *
	 * @return <code>true</code> if valid.
	 */
	public boolean isValid() {
		if (mVersion == null)
			return false;
		if (getContentLen() == -1)
			return false;
		if (mHost == null)
			return false;
		if (!mHost.getHost().isValid())
			return false;
		return true;
	}

	/**
	 * Sets the message header.
	 *
	 * @param header the header.
	 */
	public void setHeader(MessageHeader header) {
		mVersion = header.getVersion();
		setContentLen(header.getContentLen());
		mHost = new XMLPGridHost(header.getHost());
		mSourceHost = new XMLPGridHost(header.getSourceHost());
		mDestinationHost = new XMLPGridHost(header.getDestinationHost());
		mGUID = (pgrid.GUID) header.getGUID();
		mReferences = (TreeSet<GUID>) header.getReferences();
		mType = header.getDesc();
		mAdditionalAttributes = header.mAdditionalAttributes;
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
		if (qName.equals(XML_PGRID)) {
			// Header
			mVersion = attrs.getValue(XML_HEADER_VERSION);
			mGUID = pgrid.GUID.getGUID(attrs.getValue(XML_HEADER_GUID));
			mContentLen = Integer.parseInt(attrs.getValue(XML_HEADER_CONTENT_LENGTH));

		} else if (qName.equals(XML_HEADER)) {
			int extra = 2;

			String proxy = null;
			mHop = Integer.parseInt(attrs.getValue(XML_HOPS));
			mType = Integer.parseInt(attrs.getValue(XML_TYPE));
			if ((proxy = attrs.getValue(XML_DELEGATE_STATUS)) != null) {
				if (proxy.equals(DelegateStatus.toBeDelegated.toString()))
					mDelegated = DelegateStatus.toBeDelegated;
				else if (proxy.equals(DelegateStatus.hasBeenDelegated.toString()))
					mDelegated = DelegateStatus.hasBeenDelegated;
				// don't double decode delegate attribute in additional attr.
				++extra;
			}
			// retrieve all remaining properties
			int len = attrs.getLength();
			for(int i=extra; i<len ;i++) {
				mAdditionalAttributes.put(attrs.getQName(i), attrs.getValue(i));
			}
		} else if (qName.equals(XML_HEADER_CLIENT_ADD)) {
			mParsingClientAddress=true;
		} else if (qName.equals(XML_REQUESTOR_HOST)) {
		 	mParsingRequestor = true;
		} else if (qName.equals(XMLPGridHost.XML_HOST)) {

			// Host
			if (mParsingRouteHeader) {
				mRouteHeader.startElement(uri, lName, qName, attrs);
			} else if (mParsingClientAddress) {
				mClientHost = XMLPGridHost.getXMLHost(qName, attrs);
				mParsingClientAddress=false;
			} else if (mParsingRequestor) {
				mRequestorHost = XMLPGridHost.getXMLHost(qName, attrs).getHost();
				mParsingRequestor = false;
			}else {
				mHost = XMLPGridHost.getXMLHost(qName, attrs);
			}
			
		} else if (qName.equals(XML_SOURCE_HOST)) {	
			mSourceHost = XMLPGridHost.getXMLHost(qName, attrs);	
		} else if (qName.equals(XML_DESTINATION_HOST)) {	
			mDestinationHost = XMLPGridHost.getXMLHost(qName, attrs);
		} else if (qName.equals(XML_HEADER_REFERENCE)) {
			// Reference
			if (mReferences == null)
				mReferences = new TreeSet<GUID>();

			mReferences.add(new pgrid.GUID(attrs.getValue(XML_HEADER_GUID)));
		} else if (qName.equals(RouteHeader.XML_ROUTE)) {
			if (mRouteHeader == null)
				mRouteHeader = new RouteHeader();
			mParsingRouteHeader = true;
			mRouteHeader.startElement(uri, lName, qName, attrs);
		}
	}

	public void endElement(String uri, String lName, String qName) throws SAXException {
		super.endElement(uri, lName, qName);	//To change body of overridden methods use File | Settings | File Templates.
		if (qName.equals(RouteHeader.XML_ROUTE)) mParsingRouteHeader = false;
	}

	/**
	 * Sets the content length for the message.
	 *
	 * @param contentLen the content length.
	 */
	public void setContentLen(int contentLen) {
		mContentLen = contentLen;
	}

	/**
	 * Increment the hop counter
	 */
	public void incHops() {
		++mHop;
	}

	/**
	 * Returns the value to which the key is mapped; null if the key is not mapped to any value.
	 * @return the value to which the key is mapped; null if the key is not mapped to any value.
	 */
	public String getAdditionalAttribute(String key) {
		return mAdditionalAttributes.get(key);
	}

	/**
	 * get additionnal attributes
	 */
	public Hashtable<String, String> getAdditionalAttributes() {
		return mAdditionalAttributes;
	}

	/**
	 * Set additionnal attributes
	 */
	public void setAdditionalAttributes(Hashtable<String, String> attrs) {
		mAdditionalAttributes=attrs;
	}

	/**
	 * Set an application specific attribute. The key and value will be written in the XML representation of this
	 * header therefor they should be XML complient.
 	 * @param key the key
	 * @param value the value
	 */
	public void setAdditionalAttribute(String key, String value) {
		mAdditionalAttributes.put(key, value);
	}

	/**
	 * Returns a string represantation of this message.
	 *
	 * @return a string represantation of this message.
	 */
	public String toXMLString() {
		return toXMLString(LEADING_PART) + toXMLString(ENDING_PART);
	}

	/**
	 * Returns a string represantation of a part of this message.
	 *
	 * @param part the part to return.
	 * @return a string represantation of a part of this message.
	 */
	public String toXMLString(short part) {
		return toXMLString("", XML_NEW_LINE, part);
	}

	/**
	 * Returns the XML representation of this object.
	 *
	 * @param prefix  the XML prefix before each element in a new line.
	 * @param newLine the new line string.
	 * @return the XML string.
	 */
	public String toXMLString(String prefix, String newLine) {
		return toXMLString(prefix, newLine, LEADING_PART) + toXMLString(prefix, newLine, ENDING_PART);
	}

	/**
	 * Returns the XML representation of this object.
	 *
	 * @param prefix  the XML prefix before each element in a new line.
	 * @param newLine the new line string, e.g., \n.
	 * @param part    the leading or ending part.
	 * @return the XML string.
	 */
	public String toXMLString(String prefix, String newLine, short part) {
		if (part == LEADING_PART) {
			StringBuffer buf = new StringBuffer(100);
			buf.append(prefix + XML_ELEMENT_OPEN  + XML_PGRID + XML_SPACE + XML_HEADER_VERSION + XML_ATTR_OPEN + mVersion + XML_ATTR_CLOSE); //{prefix}<P-Grid Version="MAJOR_VERSION"
			buf.append(XML_SPACE + XML_HEADER_GUID + XML_ATTR_OPEN + mGUID.toString() + XML_ATTR_CLOSE);
			buf.append(XML_SPACE + XML_HEADER_CONTENT_LENGTH + XML_ATTR_OPEN + mContentLen + XML_ATTR_CLOSE);
			buf.append(XML_ELEMENT_CLOSE + newLine); //_Content-Length="CONTENT_LENGTH">{newLine}
			buf.append(prefix + XML_TAB + XML_ELEMENT_OPEN + XML_HEADER); //{prefix}\t<Header
			buf.append(XML_SPACE + XML_TYPE + XML_ATTR_OPEN + mType + XML_ATTR_CLOSE);
			if (mDelegated != DelegateStatus.direct)
				buf.append(XML_SPACE + XML_DELEGATE_STATUS + XML_ATTR_OPEN + mDelegated + XML_ATTR_CLOSE);
			buf.append(XML_SPACE + XML_HOPS + XML_ATTR_OPEN + mHop + XML_ATTR_CLOSE); // Hops="HOPS">{newLine}

			// append addition attr.
			if (!mAdditionalAttributes.isEmpty()) {
				for (String key: mAdditionalAttributes.keySet()) {
					buf.append(XML_SPACE + key + XML_ATTR_OPEN + mAdditionalAttributes.get(key) + XML_ATTR_CLOSE);
				}
			}
			buf.append(XML_ELEMENT_CLOSE + newLine); // Hops="HOPS">{newLine}
			
			buf.append(mHost.toXMLString(prefix + XML_TAB + XML_TAB, newLine, false)); //{prefix}\t\t<Host ...>
			
			if (getSourceHost() != null){
				buf.append(prefix + XML_TAB + XML_TAB + XML_ELEMENT_OPEN + XML_SOURCE_HOST + // {prefix}<Host
						XML_SPACE + XMLPGridHost.XML_HOST_GUID + XML_ATTR_OPEN + getSourceHost().getGUID().toString() + XML_ATTR_CLOSE + // _GUID="GUID"
						XML_SPACE + XMLPGridHost.XML_HOST_ADDRESS + XML_ATTR_OPEN + getSourceHost().getAddressString() + XML_ATTR_CLOSE + // _Address="ADDRESS"
						XML_SPACE + XMLPGridHost.XML_HOST_PORT + XML_ATTR_OPEN + getSourceHost().getPort() + XML_ATTR_CLOSE); // _Port="PORT"
				buf.append(XML_ELEMENT_END_CLOSE + newLine); // />{newLine}
			}
			
			/*
			 * FIXME: if destination host exists, its GUID should never be null. Find out why ! This is probably a bug. 
			 */
			if (getDestinationHost() != null && getDestinationHost().getGUID() == null){
				Constants.LOGGER.warning("FIXME -> GUID is null for destination host: " + getDestinationHost().getAddressString() + ":" + getDestinationHost().getPort());
			}
			
			if (getDestinationHost() != null && getDestinationHost().getGUID() != null){
				
				buf.append(prefix + XML_TAB + XML_TAB + XML_ELEMENT_OPEN + XML_DESTINATION_HOST + // {prefix}<Host
						XML_SPACE + XMLPGridHost.XML_HOST_GUID + XML_ATTR_OPEN + getDestinationHost().getGUID().toString() + XML_ATTR_CLOSE + // _GUID="GUID"
						XML_SPACE + XMLPGridHost.XML_HOST_ADDRESS + XML_ATTR_OPEN + getDestinationHost().getAddressString() + XML_ATTR_CLOSE + // _Address="ADDRESS"
						XML_SPACE + XMLPGridHost.XML_HOST_PORT + XML_ATTR_OPEN + getDestinationHost().getPort() + XML_ATTR_CLOSE); // _Port="PORT"
				buf.append(XML_ELEMENT_END_CLOSE + newLine); // />{newLine}
			} 
			
			if (mRouteHeader != null)
				buf.append(mRouteHeader.toXMLString(prefix + XML_TAB + XML_TAB, newLine));
			if (mReferences != null) {
				for (GUID guid : mReferences) {
					buf.append(prefix + XML_TAB + XML_TAB + XML_ELEMENT_OPEN + XML_HEADER_REFERENCE); //{prefix}\t\t<Reference>
					buf.append(XML_SPACE + XML_HEADER_GUID + XML_ATTR_OPEN + guid.toString() + XML_ATTR_CLOSE + XML_ELEMENT_END_CLOSE + newLine); //_GUID="GUID"/>{newLine}
				}
			}
			if (mClientHost != null) {
				buf.append(prefix + XML_TAB + XML_TAB + XML_ELEMENT_OPEN + XML_HEADER_CLIENT_ADD + XML_ELEMENT_CLOSE + newLine); //{prefix}\t\t<Client>
				buf.append(mClientHost.toXMLString(prefix + XML_TAB + XML_TAB + XML_TAB, newLine, false));
				buf.append(prefix + XML_TAB + XML_TAB + XML_ELEMENT_OPEN_END + XML_HEADER_CLIENT_ADD + XML_ELEMENT_CLOSE + newLine); //</Client>{newLine}

			}

			// requesting host
			if (mRequestorHost != null) {
				buf.append(prefix + XML_TAB + XML_TAB + XML_ELEMENT_OPEN + XML_REQUESTOR_HOST + XML_ELEMENT_CLOSE).append(newLine);
				buf.append(XMLPGridHost.toXMLHost(mRequestorHost).toXMLString(prefix+XML_TAB+XML_TAB+XML_TAB, newLine));
				buf.append(prefix + XML_TAB + XML_TAB + XML_ELEMENT_OPEN_END + XML_REQUESTOR_HOST + XML_ELEMENT_CLOSE).append(newLine);
			}


			buf.append(prefix + XML_TAB + XML_ELEMENT_OPEN_END + XML_HEADER + XML_ELEMENT_CLOSE + newLine); //{prefix}\t</Header>{newLine}
			return buf.toString();
		} else if (part == ENDING_PART)
			return prefix + CLOSING_TAG + newLine; // {prefix}</P-Grid>{newLine}
		return null;
	}


	/**
	 * Returns the number of hops this message has done
	 * @return the number of hops this message has done
	 */
	public int getHops() {
		return mHop;
	}

	/**
	 * Set the number of hops this message has done
	 */
	public void setHops(int hops) {
		mHop = hops;
	}

	public Object clone() {
		MessageHeader msg = null;

		msg = new MessageHeader(mVersion, mContentLen, mHost.getHost(), mType);
		msg.mAdditionalAttributes = (Hashtable<String, String>) mAdditionalAttributes.clone();
		msg.mCompressed = mCompressed;
		msg.mGUID = mGUID;
		msg.mHop = mHop;
		msg.mReferences = (TreeSet<GUID>) mReferences.clone();
		msg.mRouteHeader = (RouteHeader) (mRouteHeader != null ? mRouteHeader.clone() : null);
		msg.mDelegated=mDelegated;
		msg.mClientHost=mClientHost;
		msg.mRequestorHost=mRequestorHost;
		msg.mSourceHost=mSourceHost;
		msg.mDestinationHost=mDestinationHost;
		return msg;
	}

	/**
	 * clear the reference list
	 */
	public void clearReferences() {
		if (mReferences != null)
			mReferences.clear();
	}

	/**
	 * Set proxy mode
	 */
	public void setDelegateStatus(DelegateStatus mode) {
		mDelegated = mode;
	}

	public DelegateStatus getDelegateStatus() {
		return mDelegated;
	}

	/**
	 * Set the XML parser for this message. This method is used internally, do not use it.
	 * @param parser the parser
	 */
	public void setParser(XMLReader parser) {
		mParser = parser;
	}

	/**
	 * get the XML parser for this message. This method is used internally, do not use it.
	 */
	public XMLReader getParser() {
		return mParser;
	}

	/**
	 * set the client address to the given one. If a client address is provided, it means that this message has been
	 * re-issued by a super peer and the "host" address will be used to forward reply message back to the real initiator.
	 *
	 * @param host host client address.
	 */
	public void setClientAddress(PGridHost host) {
		if (host != null)
			mClientHost = new XMLPGridHost(host);
		else mClientHost = null;
	}

	/**
	 * get the client address for this message. If a client address is provided, it means that this message has been
	 * re-issued by a super peer and the "host" address will be used to forward reply message back to the real initiator.
	 *
	 */
	public PGridHost getClientAddress() {
		return (mClientHost!=null?mClientHost.getHost():null);
	}
}