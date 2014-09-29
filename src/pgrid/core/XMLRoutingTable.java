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

package pgrid.core;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import pgrid.XMLizable;
import pgrid.PGridHost;
import pgrid.network.protocol.XMLPGridHost;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * This class extends the {@link pgrid.core.RoutingTable} with XML functionality.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class XMLRoutingTable extends RoutingTable implements XMLizable {

	/**
	 * A part of the XML string.
	 */
	public static final String XML_ROUTING_TABLE = "RoutingTable";

	/**
	 * A part of the XML string.
	 */
	static final String XML_ROUTING_TABLE_FIDGETS = "Fidgets";

	/**
	 * A part of the XML string.
	 */
	static final String XML_ROUTING_TABLE_REFS = "References";

	/**
	 * A part of the XML string.
	 */
	static final String XML_ROUTING_TABLE_REFS_LEVEL = "Level";

	/**
	 * A part of the XML string.
	 */
	static final String XML_ROUTING_TABLE_REPLICAS = "Replicas";

	/**
	 * The temporary variable during parsing.
	 */
	private Collection mTmpHosts = null;

	/**
	 * The temporary variable during parsing.
	 */
	private int mTmpLevel = 0;

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
		if (qName.equals(XML_ROUTING_TABLE_FIDGETS)) {
			// Fidget Hosts
			setFidgets(mTmpHosts);
			mTmpHosts = null;
		} else if (qName.equals(XML_ROUTING_TABLE_REFS)) {
			// Reference Hosts
			setLevel(mTmpLevel, mTmpHosts);
			mTmpHosts = null;
		} else if (qName.equals(XML_ROUTING_TABLE_REPLICAS)) {
			// Replica Hosts
			setReplicas(mTmpHosts);
			mTmpHosts = null;
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
	 * @throws SAXException any SAX exception, possibly wrapping another exception.
	 */
	public void startElement(String uri, String lName, String qName, Attributes attrs) throws SAXException {
		if (qName.equals(XML_ROUTING_TABLE_FIDGETS)) {
			mTmpHosts = new Vector();
		} else if (qName.equals(XML_ROUTING_TABLE_REFS)) {
			// Reference Hosts
			mTmpHosts = new Vector();
			mTmpLevel = Integer.parseInt(attrs.getValue(XML_ROUTING_TABLE_REFS_LEVEL));
		} else if (qName.equals(XML_ROUTING_TABLE_REPLICAS)) {
			mTmpHosts = new Vector();
		} else if (qName.equals(XMLPGridHost.XML_HOST)) {
			// a PGridHost Reference (Fidget, Reference, or Replica)
			PGridHost ref = XMLPGridHost.getHost(qName, attrs);
			if (ref.isValid()) {
				if (mTmpHosts == null) {
					// local host
					setLocalHost(ref);
					setLevels(mLocalHost.getPath().length() - 1);
				} else {
					mTmpHosts.add(ref);
				}
			}
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
		return toXMLString(prefix, newLine, true, true, true);
	}

	/**
	 * Returns the XML representation of this object.
	 *
	 * @param prefix   the XML prefix before each element in a new line.
	 * @param newLine  the new line string.
	 * @param fidgets  if the fidgets should be included.
	 * @param refs     if the refs should be included.
	 * @param replicas if the replicas should be included.
	 * @return the XML string.
	 */
	public String toXMLString(String prefix, String newLine, boolean fidgets, boolean refs, boolean replicas) {

		StringBuffer strBuff = new StringBuffer(500);
		acquireReadLock();
		try {
			strBuff.append(prefix + XML_ELEMENT_OPEN + XML_ROUTING_TABLE + XML_ELEMENT_CLOSE + newLine); // {prefix}<RoutingTable>{newLine}
			strBuff.append(XMLPGridHost.toXMLHost(mLocalHost).toXMLString(prefix + XML_TAB, newLine, true)); // QUESTION Renault, do you really need it? , true));
			if ((fidgets) && (mFidgets.size() > 0)) {
				strBuff.append(prefix + XML_TAB + XML_ELEMENT_OPEN + XML_ROUTING_TABLE_FIDGETS + XML_ELEMENT_CLOSE + newLine); // {prefix}\t<Fidgets>{newLine}
				for (Iterator it = mFidgets.iterator(); it.hasNext();) {
					PGridHost host = (PGridHost)it.next();
					strBuff.append(XMLPGridHost.toXMLHost(host).toXMLString(prefix + XML_TAB + XML_TAB, newLine, true));
				}
				strBuff.append(prefix + XML_TAB + XML_ELEMENT_OPEN_END + XML_ROUTING_TABLE_FIDGETS + XML_ELEMENT_CLOSE + newLine); // {prefix}\t</Fidgets>{newLine}
			}

			if ((refs) && (mLevels.size() > 0)) {
				int level = 0;
				for (Iterator it = mLevels.iterator(); it.hasNext();) {
					Collection hosts = (Collection)it.next();
					strBuff.append(prefix + XML_TAB + XML_ELEMENT_OPEN + XML_ROUTING_TABLE_REFS // {prefix}\t<References
							+ XML_SPACE + XML_ROUTING_TABLE_REFS_LEVEL + XML_ATTR_OPEN + (level++) + XML_ATTR_CLOSE + XML_ELEMENT_CLOSE + newLine); // _Level="LEVEL">{newLine}
					for (Iterator it2 = hosts.iterator(); it2.hasNext();) {
						PGridHost host = (PGridHost)it2.next();
						strBuff.append(XMLPGridHost.toXMLHost(host).toXMLString(prefix + XML_TAB + XML_TAB, newLine, true));
					}
					strBuff.append(prefix + XML_TAB + XML_ELEMENT_OPEN_END + XML_ROUTING_TABLE_REFS + XML_ELEMENT_CLOSE + newLine); // {prefix}\t</References>{newLine}
					if (level >= mLocalHost.getPath().length()) break;
				}
			}


			if ((replicas) && (mReplicas.size() > 0)) {
				strBuff.append(prefix + XML_TAB + XML_ELEMENT_OPEN + XML_ROUTING_TABLE_REPLICAS + XML_ELEMENT_CLOSE + newLine); // {prefix}\t<Replicas>{newLine}
				for (Iterator it = mReplicas.iterator(); it.hasNext();) {
					PGridHost host = (PGridHost)it.next();
					strBuff.append(XMLPGridHost.toXMLHost(host).toXMLString(prefix + XML_TAB + XML_TAB, newLine, true));
				}
				strBuff.append(prefix + XML_TAB + XML_ELEMENT_OPEN_END + XML_ROUTING_TABLE_REPLICAS + XML_ELEMENT_CLOSE + newLine); // {prefix}\t</Replicas>{newLine}
			}

			strBuff.append(prefix + XML_ELEMENT_OPEN_END + XML_ROUTING_TABLE + XML_ELEMENT_CLOSE + newLine); // {prefix}</RoutingTable>{newLine}
		} finally {
			releaseReadLock();
		}

		return strBuff.toString();
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
	}

	/**
	 * Report the end of a CDATA section.
	 *
	 * @throws org.xml.sax.SAXException The application may raise an exception.
	 * @see #startCDATA
	 */
	public void endCDATA() throws SAXException {
	}

	public Object clone() throws CloneNotSupportedException {
		Object clone;
		acquireReadLock();
		try {
			clone = super.clone();
		} finally{
			releaseReadLock();
		}
		return clone;
	}

}
