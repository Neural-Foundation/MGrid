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
import p2p.basic.KeyRange;
import pgrid.interfaces.index.PGridIndexFactory;
import pgrid.*;
import pgrid.Type;

/**
 * This class represents a Gridella range query message.
 *
 * @author <a href="mailto:Renault JOHN <renault.john@epfl.ch>">Renault JOHN</a>
 */
public class RangeQueryMessage extends PGridMessageImp {

	/**
	 * A part of the XML string.
	 */
	public static final String XML_QUERY = "RangeQuery";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_QUERY_INDEX = "Index";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_QUERY_PREFIX = "Prefix";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_LOWER_BOUND_KEY = "LowerBoundKey";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_HIGHER_BOUND_KEY = "HigherBoundKey";
	
	/**
	 * A part of the XML string.
	 */
	private static final String XML_X_MIN = "XMinimum";
	
	/**
	 * A part of the XML string.
	 */
	private static final String XML_X_MAX = "XMaximum";
	
	/**
	 * A part of the XML string.
	 */
	private static final String XML_Y_MIN = "YMinimum";
	
	/**
	 * A part of the XML string.
	 */
	private static final String XML_Y_MAX = "YMaximum";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_QUERY_MINSPEED = "MinSpeed";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_QUERY_TYPE = "Type";

	/**
	 * A part of the XML string.
	 */
	private static final String XML_KEYWORD = "Keyword";
	
	/**
	 * A part of the XML string.
	 */
	private static final String HITS = "Hits";

	/**
	 * Temporary int use for parsing
	 */
	private int mFirstParsed = 0;

	/**
	 * The range query message.
	 */
	private RangeQuery mRangeQuery = null;

	/**
	 * The temporary variable during parsing.
	 */
	private XMLizable mParsedObject = null;

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	protected String getXMLMessageName() {
		return XML_QUERY;
	}

	/**
	 * Default constructor
	 */
	public RangeQueryMessage() {
		mRangeQuery = new RangeQuery();
		mRangeQuery.setPrefix("");

		mRangeQuery.setGUID(getHeader().getReferences().iterator().next());
		mRangeQuery.setAlgorithm(getHeader().getRouteHeader().getStrategy());
	}

	/**
	 * Creates an empty query hit message.
	 *
	 * @param header the message header.
	 */
	public RangeQueryMessage(MessageHeader header) {
		super(header);
		mRangeQuery = new RangeQuery();
		mRangeQuery.setPrefix("");
	}

	/**
	 * Create a copy of this message.
	 *
	 * @return a copy of this message.
	 */
	public Object clone() {
		RangeQueryMessage msg = (RangeQueryMessage) super.clone();

		msg.mFirstParsed = 0;
		msg.mParsedObject = null;
		msg.mRangeQuery = new RangeQuery(mRangeQuery.getGUID(),mRangeQuery.getType(), mRangeQuery.getAlgorithm(),
				mRangeQuery.getLowerBound(), mRangeQuery.getHigherBound(), mRangeQuery.getKeyRange(),
				mRangeQuery.getPrefix(), mRangeQuery.getMinSpeed(), mRangeQuery.getHops(), mRangeQuery.getRequestingHost());

		return msg;
	}

	/**
	 * Set the message header.
	 */
	public void setHeader(MessageHeader header) {
		super.setHeader(header);	//To change body of overridden methods use File | Settings | File Templates.

		mRangeQuery.setGUID(header.getReferences().iterator().next());
		mRangeQuery.setAlgorithm(header.getRouteHeader().getStrategy());
	}

	/**
	 * Creates a new query message with given values.
	 *
	 * @param guid      the guid of the query.
	 * @param type     	the type of the query.
	 * @param minQuery  the lower bound search string.
	 * @param maxQuery  the higher bound search string.
	 * @param rq      the key (binary represantation of the search query).
	 * @param index    the search progress.
	 * @param minSpeed the minimal speed for responding hosts.
	 * @param hops     the hop count.
	 */
	public RangeQueryMessage(GUID guid, p2p.index.Type type, int hops, String algorithm, String minQuery, String maxQuery, KeyRange rq, int index, String prefix, int minSpeed, PGridHost initialHost) {
		super(guid);
		mRangeQuery = new RangeQuery(guid, type, hops, algorithm, minQuery, maxQuery, rq, index, prefix, minSpeed, initialHost);
		mRangeQuery.setPrefix(prefix);
		getHeader().setGUID(guid);
		getHeader().setRequestorHost(initialHost);
	}
	
	/**
	 * Creates a new query message with given values.
	 *
	 * @param guid      the guid of the query.
	 * @param type     	the type of the query.
	 * @param minQuery  the lower bound search string.
	 * @param maxQuery  the higher bound search string.
	 * @param origxMin the original query x minimum
	 * @param origxMax the original query x maximum
	 * @param origyMin the original query y minimum
	 * @param origyMax the original query y maximum
	 * @param rq      the key (binary represantation of the search query).
	 * @param index    the search progress.
	 * @param minSpeed the minimal speed for responding hosts.
	 * @param hops     the hop count.
	 */
	public RangeQueryMessage(GUID guid, p2p.index.Type type, int hops, String algorithm, 
			String minQuery, String maxQuery, Long origxMin, Long origxMax, Long origyMin, Long origyMax, 
			KeyRange rq, int index, String prefix, int minSpeed, PGridHost initialHost, Long hits) {
		super(guid);
		mRangeQuery = new RangeQuery(guid, type, hops, algorithm, minQuery, maxQuery, origxMin,  origxMax,
				 origyMin, origyMax ,rq, index, prefix, minSpeed, initialHost);
		

		mRangeQuery.setPrefix(prefix);
		getHeader().setGUID(guid);
		getHeader().setRequestorHost(initialHost);
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
		if (mRangeQuery.getGUID() == null)
			return false;
		if (mRangeQuery.getType() == null)
			return false;
		if (mRangeQuery.getLowerBound() == null)
			return false;
		if (mRangeQuery.getHigherBound() == null)
			return false;
		if(mRangeQuery.getOrigxMin() == null)
			return false;
		if(mRangeQuery.getOrigxMax() == null) 
			return false;
		if(mRangeQuery.getOrigyMin() == null) 
			return false;
		if(mRangeQuery.getOrigyMax() == null )
			return false;
		if (mRangeQuery.getKeyRange() == null)
			return false;
		if (mRangeQuery.getMinSpeed() == -1)
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
		String parsed;

		if (parsingCDATA()) {
			parsed = String.valueOf(ch, start, length);
			if (mFirstParsed == 1) {
				if (mRangeQuery.getLowerBound() == null) {
					mRangeQuery.setLowerBound(parsed);
				} else {
					if (parsed.length() > 0)
						mRangeQuery.setLowerBound(mRangeQuery.getLowerBound().concat(parsed));
				}
			} else {
				if (mRangeQuery.getHigherBound() == null) {
					mRangeQuery.setHigherBound(parsed);
				} else {
					if (parsed.length() > 0)
						mRangeQuery.setHigherBound(mRangeQuery.getHigherBound().concat(parsed));
				}
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
			mRangeQuery.setType((Type) PGridIndexFactory.sharedInstance().getTypeByString(attrs.getValue(XML_QUERY_TYPE)));
			mRangeQuery.setKeyRange(new PGridKeyRange(new PGridKey(attrs.getValue(XML_LOWER_BOUND_KEY)),
					new PGridKey(attrs.getValue(XML_HIGHER_BOUND_KEY))));
			
			mRangeQuery.setOrigxMin(Long.parseLong(attrs.getValue(XML_X_MIN)));
			mRangeQuery.setOrigxMax(Long.parseLong(attrs.getValue(XML_X_MAX)));
			mRangeQuery.setOrigyMin(Long.parseLong(attrs.getValue(XML_Y_MIN)));
			mRangeQuery.setOrigyMax(Long.parseLong(attrs.getValue(XML_Y_MAX)));
			
			String minSpeed = attrs.getValue(XML_QUERY_MINSPEED);

			if (minSpeed == null)
				mRangeQuery.setMinSpeed(0);
			else
				mRangeQuery.setMinSpeed(Integer.parseInt(minSpeed));

			mRangeQuery.setIndex(Integer.parseInt(attrs.getValue(XML_QUERY_INDEX)));

			mRangeQuery.setPrefix(attrs.getValue(XML_QUERY_PREFIX));

		} else if (qName.equals(XML_KEYWORD)) {
			mFirstParsed++;
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
		if (qName.equals(XML_QUERY)) {
			mRangeQuery.setGUID(getHeader().getGUID());
			mRangeQuery.setInitialHost(getHeader().getRequestorHost());
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
		String xmlMessage = prefix + XML_ELEMENT_OPEN + XML_QUERY + // {prefix}<RangeQuery
				XML_SPACE + XML_QUERY_TYPE + XML_ATTR_OPEN + mRangeQuery.getTypeString() + XML_ATTR_CLOSE + // _Type="TYPE"
				XML_SPACE + XML_QUERY_INDEX + XML_ATTR_OPEN + mRangeQuery.getIndex() + XML_ATTR_CLOSE + // _Index="INDEX"
				XML_SPACE + XML_QUERY_PREFIX + XML_ATTR_OPEN + mRangeQuery.getPrefix() + XML_ATTR_CLOSE + // _Prefix="PREFIX"
				XML_SPACE + XML_LOWER_BOUND_KEY + XML_ATTR_OPEN + mRangeQuery.getKeyRange().getMin() + XML_ATTR_CLOSE + // _FirstKey="KEY"
				XML_SPACE + XML_HIGHER_BOUND_KEY + XML_ATTR_OPEN + mRangeQuery.getKeyRange().getMax() + XML_ATTR_CLOSE + // _SecondKey="SECOND_KEY"
				XML_SPACE + XML_X_MIN + XML_ATTR_OPEN + mRangeQuery.getOrigxMin() + XML_ATTR_CLOSE + // _Xmin="XMIN"
				XML_SPACE + XML_X_MAX + XML_ATTR_OPEN + mRangeQuery.getOrigxMax() + XML_ATTR_CLOSE + // _Xmax="XMAX"
				XML_SPACE + XML_Y_MIN + XML_ATTR_OPEN + mRangeQuery.getOrigyMin() + XML_ATTR_CLOSE + // _Ymin="YMIN"
				XML_SPACE + XML_Y_MAX + XML_ATTR_OPEN + mRangeQuery.getOrigyMax() + XML_ATTR_CLOSE + // _Ymax="YMAX"
				XML_SPACE + XML_QUERY_MINSPEED + XML_ATTR_OPEN + mRangeQuery.getMinSpeed() + XML_ATTR_CLOSE + XML_ELEMENT_CLOSE + newLine + // _MinSpeed="MINSPEED"
				prefix + XML_TAB + XML_ELEMENT_OPEN + XML_KEYWORD + XML_ELEMENT_CLOSE + // <Keyword>
				XML_CDATA_OPEN + mRangeQuery.getLowerBound() + XML_CDATA_CLOSE + // <![CDATA[QUERY-STRING]]>
				XML_ELEMENT_OPEN_END + XML_KEYWORD + XML_ELEMENT_CLOSE + newLine + // </ Keyword>
				prefix + XML_TAB + XML_ELEMENT_OPEN + XML_KEYWORD + XML_ELEMENT_CLOSE + // <Keyword>
				XML_CDATA_OPEN + mRangeQuery.getHigherBound() + XML_CDATA_CLOSE + // <![CDATA[QUERY-STRING]]>
				XML_ELEMENT_OPEN_END + XML_KEYWORD + XML_ELEMENT_CLOSE + newLine + // </ Keyword>
				prefix + XML_ELEMENT_OPEN_END + XML_QUERY + XML_ELEMENT_CLOSE + newLine; // </RangeQuery>
		return xmlMessage;
	}
	
/*	public String toXMLString(String prefix, String newLine) {
		String xmlMessage = prefix + XML_ELEMENT_OPEN + XML_QUERY + // {prefix}<RangeQuery
				XML_SPACE + XML_QUERY_TYPE + XML_ATTR_OPEN + mRangeQuery.getTypeString() + XML_ATTR_CLOSE + // _Type="TYPE"
				XML_SPACE + XML_QUERY_INDEX + XML_ATTR_OPEN + mRangeQuery.getIndex() + XML_ATTR_CLOSE + // _Index="INDEX"
				XML_SPACE + XML_QUERY_PREFIX + XML_ATTR_OPEN + mRangeQuery.getPrefix() + XML_ATTR_CLOSE + // _Prefix="PREFIX"
				XML_SPACE + XML_LOWER_BOUND_KEY + XML_ATTR_OPEN + mRangeQuery.getKeyRange().getMin() + XML_ATTR_CLOSE + // _FirstKey="KEY"
				XML_SPACE + XML_HIGHER_BOUND_KEY + XML_ATTR_OPEN + mRangeQuery.getKeyRange().getMax() + XML_ATTR_CLOSE + // _SecondKey="SECOND_KEY"
				XML_SPACE + XML_QUERY_MINSPEED + XML_ATTR_OPEN + mRangeQuery.getMinSpeed() + XML_ATTR_CLOSE + XML_ELEMENT_CLOSE + newLine + // _MinSpeed="MINSPEED"
				prefix + XML_TAB + XML_ELEMENT_OPEN + XML_KEYWORD + XML_ELEMENT_CLOSE + // <Keyword>
				XML_CDATA_OPEN + mRangeQuery.getLowerBound() + XML_CDATA_CLOSE + // <![CDATA[QUERY-STRING]]>
				XML_ELEMENT_OPEN_END + XML_KEYWORD + XML_ELEMENT_CLOSE + newLine + // </ Keyword>
				prefix + XML_TAB + XML_ELEMENT_OPEN + XML_KEYWORD + XML_ELEMENT_CLOSE + // <Keyword>
				XML_CDATA_OPEN + mRangeQuery.getHigherBound() + XML_CDATA_CLOSE + // <![CDATA[QUERY-STRING]]>
				XML_ELEMENT_OPEN_END + XML_KEYWORD + XML_ELEMENT_CLOSE + newLine + // </ Keyword>
				prefix + XML_ELEMENT_OPEN_END + XML_QUERY + XML_ELEMENT_CLOSE + newLine; // </RangeQuery>
		return xmlMessage;
	}
*/
	/**
	 * Get the message content.
	 *
	 * @return a binary representation of the message
	 */
	public byte[] getData() {
		return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
	}


	/**
	 * Return the buisiness logic object
	 * @return the buisiness logic object
	 */
	public RangeQuery getQuery() {
		return mRangeQuery;
	}
}
