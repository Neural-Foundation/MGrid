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
import p2p.basic.Key;
import pgrid.*;
import pgrid.Query;
import pgrid.Type;
import pgrid.interfaces.index.PGridIndexFactory;

import java.util.Iterator;
import java.util.Vector;

/**
 * This class represents a Gridella query message.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class QueryMessage extends PGridMessageImp {

	/**
	 * A part of the XML string.
	 */
	public static final String XML_QUERY = "Query";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_QUERY_MINSPEED = "MinSpeed";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_QUERY_REPLICAS = "Replicas";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_QUERY_TYPE = "Type";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_QUERY_KEYWORD = "Keyword";

	/**
	 * Query Object
	 */
	private Query mQuery = null;

	/**
	 * Flag to indicate if a host is the requesting host or a replica during parsing a message.
	 */
	private boolean mReplicaFlag = false;

	/**
	 * The temporary variable during parsing.
	 */
	private XMLizable mParsedObject = null;

	/**
	 * Std constructor
	 */
	public QueryMessage() {
		super();
		mQuery = new Query();
	}

	/**
	 * Creates an empty query hit message.
	 *
	 * @param header the message header.
	 */
	public QueryMessage(MessageHeader header) {
		super(header);
		mQuery = new Query();
	}

	/**
	 * Set the message header.
	 */
	public void setHeader(MessageHeader header) {
		super.setHeader(header);	//To change body of overridden methods use File | Settings | File Templates.
		mQuery.setGUID(getHeader().getGUID());
	}

	/**
	 * Create a copy of this message.
	 *
	 * @return a copy of this message.
	 */
	public Object clone() {
		QueryMessage msg = (QueryMessage) super.clone();
		msg.mQuery = new Query(mQuery.getRequestingHost(), mQuery.getGUID(), mQuery.getType(),mQuery.getQueryString(),
				new PGridKey(getHeader().getRouteHeader().getKey()), mQuery.getIndex(), mQuery.getMinSpeed(), mQuery.getHops());

		return msg;
	}

	/**
	 * Creates a new query message with given values.
	 *
	 * @param guid        the guid of the query.
	 * @param type        the type of the query.
	 * @param query       the search string.
	 * @param key         the key (binary represantation of the search query).
	 * @param index       the search progress.
	 * @param minSpeed    the minimal speed for responding hosts.
	 * @param initialHost the requesting and initiating host.
	 */
	public QueryMessage(GUID guid, p2p.index.Type type, String query, Key key, int index, int minSpeed, PGridHost initialHost, int hops, Vector replicas) {
		super(guid);
		mQuery = new Query(initialHost, guid, type, query, key, index, minSpeed, hops);
		mQuery.setReplicas(replicas);
		getHeader().setGUID(guid);
		getHeader().setRequestorHost(initialHost);
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
		super.isValid();

		if (mQuery.getGUID() == null)
			return false;
		if (mQuery.getType() == null)
			return false;
		if (mQuery.getLowerBound() == null)
			return false;
		if (mQuery.getKeyRange() == null)
			return false;
		if (mQuery.getMinSpeed() == -1)
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
	 * @throws SAXException any SAX exception, possibly wrapping another exception.
	 */
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (parsingCDATA()) {
			if (mQuery.getQueryString() == null) {
				mQuery.setQueryString(String.valueOf(ch, start, length));
			} else {
				String append = String.valueOf(ch, start, length);
				if (append.length() > 0)
					mQuery.setQueryString(mQuery.getQueryString().concat(append));
			}
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
	 * @throws org.xml.sax.SAXException any SAX exception, possibly wrapping another exception.
	 */
	public void startElement(String uri, String lName, String qName, Attributes attrs) throws SAXException {
		if (qName.equals(XML_QUERY)) {

			// Query
			mQuery.setType((Type) PGridIndexFactory.sharedInstance().getTypeByString(attrs.getValue(XML_QUERY_TYPE)));
			String minSpeed = attrs.getValue(XML_QUERY_MINSPEED);
			if (minSpeed == null)
				mQuery.setMinSpeed(0);
			else
				mQuery.setMinSpeed(Integer.parseInt(minSpeed));
		} else if (qName.equals(XML_QUERY_REPLICAS)) {
			mQuery.setReplicas(new Vector());
			mReplicaFlag = true;
		} else if (qName.equals(XML_QUERY_KEYWORD)) {

		} else if (qName.equals(XMLPGridHost.XML_HOST)) {
			// Host
			if (mReplicaFlag) {
				XMLPGridHost host = new XMLPGridHost();
				host.startElement(uri, lName, qName, attrs);
				if (host.getHost().isValid())
					mQuery.getReplicas().add(host);
			}
		}
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
		if (qName.equals(XML_QUERY_REPLICAS)) {
			mReplicaFlag = false;
		} else if (qName.equals(XML_QUERY)) {
			mQuery.setGUID(getHeader().getGUID());
			mQuery.setKey(new PGridKey(getHeader().getRouteHeader().getKey()));
			mQuery.setRequestingHost(getHeader().getRequestorHost());
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
		StringBuffer strBuff = new StringBuffer();
		strBuff.append(prefix + XML_ELEMENT_OPEN + XML_QUERY + // {prefix}<Query
				XML_SPACE + XML_QUERY_TYPE + XML_ATTR_OPEN + mQuery.getTypeString() + XML_ATTR_CLOSE + // _Type="TYPE"
				//XML_SPACE + XML_QUERY_INDEX + XML_ATTR_OPEN + mIndex + XML_ATTR_CLOSE + // _Index="INDEX"
				//XML_SPACE + XML_LOWER_BOUND_KEY + XML_ATTR_OPEN + mQuery.getKeyRange().getMin() + XML_ATTR_CLOSE + // _LowerBound="KEY"
				//XML_SPACE + XML_HIGHER_BOUND_KEY + XML_ATTR_OPEN + mQuery.getKeyRange().getMax() + XML_ATTR_CLOSE + // _HigherBound="KEY"
				XML_SPACE + XML_QUERY_MINSPEED + XML_ATTR_OPEN + mQuery.getMinSpeed() + XML_ATTR_CLOSE + XML_ELEMENT_CLOSE + newLine);// _MinSpeed="MINSPEED"
				//XML_SPACE + XML_QUERY_HOPS + XML_ATTR_OPEN + mHops + XML_ATTR_CLOSE + XML_ELEMENT_CLOSE + newLine + // _Hops="HOPS">{newLine}
				//new XMLPGridHost(mQuery.getRequestingHost()).toXMLString(prefix + XML_TAB, newLine, false)); // <Host .../>{newLine}
		if (mQuery.getReplicas() != null) {
			if (mQuery.getReplicas().size() == 0) {
				strBuff.append(prefix + XML_TAB + XML_ELEMENT_OPEN + XML_QUERY_REPLICAS + XML_ELEMENT_END_CLOSE + newLine); // {prefix}\t<Replicas/>{newLine}
			} else {
				strBuff.append(prefix + XML_TAB + XML_ELEMENT_OPEN + XML_QUERY_REPLICAS + XML_ELEMENT_CLOSE + newLine); // {prefix}\t<Replicas>{newLine}
				for (Iterator it = mQuery.getReplicas().iterator(); it.hasNext();) {
					Object next = it.next();
					strBuff.append(((XMLPGridHost)next).toXMLString(prefix + XML_TAB + XML_TAB, newLine, false));
				}
				strBuff.append(prefix + XML_TAB + XML_ELEMENT_OPEN_END + XML_QUERY_REPLICAS + XML_ELEMENT_CLOSE + newLine); // {prefix}\t</Replicas>{newLine}
			}
		}
		strBuff.append(prefix + XML_TAB + XML_ELEMENT_OPEN + XML_QUERY_KEYWORD + XML_ELEMENT_CLOSE + // <Keyword>
				XML_CDATA_OPEN + mQuery.getQueryString() + XML_CDATA_CLOSE + // <![CDATA[QUERY-STRING]]>
				XML_ELEMENT_OPEN_END + XML_QUERY_KEYWORD + XML_ELEMENT_CLOSE + newLine + // </ Keyword>{newLine}
				prefix + XML_ELEMENT_OPEN_END + XML_QUERY + XML_ELEMENT_CLOSE + newLine); // </Query>{newLine}
		return strBuff.toString();
	}

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_QUERY;
	}

	/**
	 * Return the buisiness logic object
	 * @return the buisiness logic object
	 */
	public Query getQuery() {
		return mQuery;
	}

}