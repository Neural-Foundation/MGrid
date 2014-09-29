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

import pgrid.XMLizable;
import pgrid.PGridHost;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

import java.util.Hashtable;
import java.util.Vector;

/**
 * This class represent a Routing Header.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public class RouteHeader implements XMLizable, Cloneable {

	/**
	 * A part of the XML string.
	 */
	public static final String XML_ROUTE = "Route";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_KEY = "Key";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_STRATEGY = "Strategy";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_DEPTH = "Depth";

	/**
	 * Depth of the search
	 */
	protected int mDepth = -1;

	/**
	 * List of hosts
	 */
	protected Vector<PGridHost> mHosts = null;

	/**
	 * The routing strategy
	 */
	protected String mStrategy = null;

	/**
	 * Suplementary attributes
	 */
	private Hashtable<String, String> mAdditionalAttributes = new Hashtable<String, String>();

	/**
	 * Suplementary Properties. This hashmap can be used by routing strategy for application specific purposes.
	 */
	private Hashtable mProperties = new Hashtable();

	/**
	 * The key (binary represantion of the search string).
	 */
	protected String mKey = null;

	public RouteHeader() {

	}

	public RouteHeader(String key, String strategy, int depth, Vector<PGridHost> hosts, Hashtable<String, String> props) {
		mKey = key;
		mStrategy = strategy;
		mDepth = depth;

		mHosts = hosts;

		mAdditionalAttributes.put(XML_STRATEGY, strategy);
		if (key != null)
			mAdditionalAttributes.put(XML_KEY, key);
		if (mDepth != -1)
			mAdditionalAttributes.put(XML_DEPTH, mDepth+"");
		if (props != null)
			mAdditionalAttributes.putAll(props);
	}

	public String getStrategy() {
		return mStrategy;
	}

	public String getKey() {
		return mKey;
	}

	public void setKey(String key) {
		this.mKey = key;
		mAdditionalAttributes.put(XML_KEY, key.toString());
	}

	public int getDepth() {
		return mDepth;
	}

	public void setDepth(int depth) {
		this.mDepth = depth;
		mAdditionalAttributes.put(XML_DEPTH, ""+depth);
	}

	/**
	 * Returns the value to which the key is mapped; null if the key is not mapped to any value.
	 * @return the value to which the key is mapped; null if the key is not mapped to any value.
	 */
	public String getAdditionalAttribute(String key) {
		return mAdditionalAttributes.get(key);
	}

	/**
	 * Set a strategy specific attribute. The key and value will be written in the XML representation of this
	 * header therefor they should be XML complient.
	 * @param key the key
	 * @param value the value
	 */
	public void setAdditionalAttribute(String key, String value) {
		mAdditionalAttributes.put(key, value);
	}

	/**
	 * Returns the value to which the key is mapped; null if the key is not mapped to any value. The main difference between setAdditionalAttribute and this method is that
	 * properties are not included in the XML representation, therefor data will remain on the local host. Those
	 * properties can be used by routing strategy during the routing algorithm.
	 * @return the value to which the key is mapped; null if the key is not mapped to any value.
	 */
	public Object getProperty(Object key) {
		return mProperties.get(key);
	}

	/**
	 * Set a strategy specific property. The main difference between setAdditionalAttribute and this method is that
	 * properties are not included in the XML representation, therefor data will remain on the local host. Those
	 * properties can be used by routing strategy during the routing algorithm.
	 * @param key the key
	 * @param value the value
	 */
	public void setProperty(Object key, Object value) {
		mProperties.put(key, value);
	}


	/**
	 * The Parser will call this method to report each chunk of character data. SAX parsers may return all contiguous
	 * character data in a single chunk, or they may split it into several chunks; however, all of the characters in any
	 * single event must come from the same external entity so that the Locator provides useful information.
	 *
	 * @param ch	 the characters from the XML document.
	 * @param start  the start position in the array.
	 * @param length the number of characters to read from the array.
	 * @throws org.xml.sax.SAXException any SAX exception, possibly wrapping another exception.
	 */
	public void characters(char[] ch, int start, int length) throws SAXException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * The SAX parser will invoke this method at the end of every element in the XML document; there will be a
	 * corresponding startElement event for every endElement event (even when the element is empty).
	 *
	 * @param uri   the Namespace URI.
	 * @param lName the local name (without prefix), or the empty string if Namespace processing is not being performed.
	 * @param qName the qualified name (with prefix), or the empty string if qualified names are not available.
	 * @throws org.xml.sax.SAXException any SAX exception, possibly wrapping another exception.
	 */
	public void endElement(String uri, String lName, String qName) throws SAXException {
		//To change body of implemented methods use File | Settings | File Templates.
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
		if (qName.equals(XML_ROUTE)) {
			// Header
			mStrategy = attrs.getValue(XML_STRATEGY);
			mKey = attrs.getValue(XML_KEY);
			String depth = attrs.getValue(XML_DEPTH);
			if (depth != null)
				mDepth = Integer.parseInt(depth);

			// retrieve all remaining properties
			int len = attrs.getLength();
			for(int i=0; i<len ;i++) {
				mAdditionalAttributes.put(attrs.getQName(i), attrs.getValue(i));
			}
		} else if (qName.equals(XMLPGridHost.XML_HOST)) {
			if (mHosts == null) mHosts = new Vector<PGridHost>();

			PGridHost host = XMLPGridHost.getHost(qName, attrs);
			mHosts.add(host);
		}
	}

	/**
	 * Returns the XML representation of this object.
	 *
	 * @return the XML string.
	 */
	public String toXMLString() {
		return toXMLString("", XML_NEW_LINE);
	}

	/**
	 * Returns the XML representation of this object.
	 *
	 * @param prefix  the XML prefix before each element in a new line.
	 * @param newLine the new line string.
	 * @return the XML string.
	 */
	public String toXMLString(String prefix, String newLine) {
		StringBuffer buf = new StringBuffer(100);

		buf.append(prefix + XML_ELEMENT_OPEN + XML_ROUTE);

		if (!mAdditionalAttributes.isEmpty()) {
				for (String key: mAdditionalAttributes.keySet()) {
					buf.append(XML_SPACE + key + XML_ATTR_OPEN + mAdditionalAttributes.get(key) + XML_ATTR_CLOSE);
				}
			}


		if (mHosts == null)
			buf.append(XML_ELEMENT_END_CLOSE).append(newLine);
		else {
			buf.append(XML_ELEMENT_CLOSE).append(newLine);

			// custom hosts
			if (mHosts !=null) {
				for(PGridHost host: mHosts) {
					buf.append(XMLPGridHost.toXMLHost(host).toXMLString(prefix+XML_TAB, newLine));
				}
			}
			buf.append(prefix + XML_ELEMENT_OPEN_END + XML_ROUTE + XML_ELEMENT_CLOSE).append(newLine);
		}

		return buf.toString();
	}

	/**
	 * Report the start of a CDATA section.
	 * <p/>
	 * <p>The contents of the CDATA section will be reported through
	 * the regular {@link org.xml.sax.ContentHandler#characters
	 * characters} event; this event is intended only to report
	 * the boundary.</p>
	 *
	 * @throws org.xml.sax.SAXException The application may raise an exception.
	 * @see #endCDATA
	 */
	public void startCDATA() throws SAXException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Report the end of a CDATA section.
	 *
	 * @throws org.xml.sax.SAXException The application may raise an exception.
	 * @see #startCDATA
	 */
	public void endCDATA() throws SAXException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * List of hosts
	 * @return List of hosts
	 */
	public Vector<PGridHost> getHosts() {
		return mHosts;
	}

	/**
	 * Set the list of host
	 *
	 * @param hosts new list of host
	 */
	public void setHosts(Vector<PGridHost> hosts) {
		mHosts = hosts;
	}

	/**
	 * Add a host to the included hosts list.
	 */
	public void addHost(PGridHost host) {
		if (mHosts == null) mHosts = new Vector<PGridHost>();
		mHosts.add(host);
	}

	public Object clone() {
		RouteHeader rh = new RouteHeader(mKey, mStrategy, mDepth,
				null, (Hashtable<String, String>) mProperties.clone());
		rh.mAdditionalAttributes = (Hashtable<String, String>) mAdditionalAttributes.clone();
		if (mHosts != null) rh.mHosts = (Vector<PGridHost>) mHosts.clone();
		return rh;
	}

}
