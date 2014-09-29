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
package pgrid.util;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.SAXException;

/**
 * Class description goes here
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public class LexicalDefaultHandler extends DefaultHandler implements LexicalHandler {

	/**
	 * True if in a CDATA section
	 */
	protected boolean mCDataSection = false;
	
	protected boolean mXDataSection = false;
	
	protected boolean mYDataSection = false;
	
	protected boolean mIDDataSection = false;

	/**
	 * Report the start of DTD declarations, if any.
	 * <p/>
	 * <p>This method is intended to report the beginning of the
	 * DOCTYPE declaration; if the document has no DOCTYPE declaration,
	 * this method will not be invoked.</p>
	 * <p/>
	 * <p>All declarations reported through
	 * {@link org.xml.sax.DTDHandler DTDHandler} or
	 * {@link org.xml.sax.ext.DeclHandler DeclHandler} events must appear
	 * between the startDTD and {@link #endDTD endDTD} events.
	 * Declarations are assumed to belong to the internal DTD subset
	 * unless they appear between {@link #startEntity startEntity}
	 * and {@link #endEntity endEntity} events.  Comments and
	 * processing instructions from the DTD should also be reported
	 * between the startDTD and endDTD events, in their original
	 * order of (logical) occurrence; they are not required to
	 * appear in their correct locations relative to DTDHandler
	 * or DeclHandler events, however.</p>
	 * <p/>
	 * <p>Note that the start/endDTD events will appear within
	 * the start/endDocument events from ContentHandler and
	 * before the first
	 * {@link org.xml.sax.ContentHandler#startElement startElement}
	 * event.</p>
	 *
	 * @param name	 The document type name.
	 * @param publicId The declared public identifier for the
	 *                 external DTD subset, or null if none was declared.
	 * @param systemId The declared system identifier for the
	 *                 external DTD subset, or null if none was declared.
	 * @throws org.xml.sax.SAXException The application may raise an
	 *                                  exception.
	 * @see #endDTD
	 * @see #startEntity
	 */
	public void startDTD(String name, String publicId, String systemId) throws SAXException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Report the end of DTD declarations.
	 * <p/>
	 * <p>This method is intended to report the end of the
	 * DOCTYPE declaration; if the document has no DOCTYPE declaration,
	 * this method will not be invoked.</p>
	 *
	 * @throws org.xml.sax.SAXException The application may raise an exception.
	 * @see #startDTD
	 */
	public void endDTD() throws SAXException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Report the beginning of some internal and external XML entities.
	 * <p/>
	 * <p>The reporting of parameter entities (including
	 * the external DTD subset) is optional, and SAX2 drivers that
	 * support LexicalHandler may not support it; you can use the
	 * <code
	 * >http://xml.org/sax/features/lexical-handler/parameter-entities</code>
	 * feature to query or control the reporting of parameter entities.</p>
	 * <p/>
	 * <p>General entities are reported with their regular names,
	 * parameter entities have '%' prepended to their names, and
	 * the external DTD subset has the pseudo-entity name "[dtd]".</p>
	 * <p/>
	 * <p>When a SAX2 driver is providing these events, all other
	 * events must be properly nested within start/end entity
	 * events.  There is no additional requirement that events from
	 * {@link org.xml.sax.ext.DeclHandler DeclHandler} or
	 * {@link org.xml.sax.DTDHandler DTDHandler} be properly ordered.</p>
	 * <p/>
	 * <p>Note that skipped entities will be reported through the
	 * {@link org.xml.sax.ContentHandler#skippedEntity skippedEntity}
	 * event, which is part of the ContentHandler interface.</p>
	 * <p/>
	 * <p>Because of the streaming event model that SAX uses, some
	 * entity boundaries cannot be reported under any
	 * circumstances:</p>
	 * <p/>
	 * <ul>
	 * <li>general entities within attribute values</li>
	 * <li>parameter entities within declarations</li>
	 * </ul>
	 * <p/>
	 * <p>These will be silently expanded, with no indication of where
	 * the original entity boundaries were.</p>
	 * <p/>
	 * <p>Note also that the boundaries of character references (which
	 * are not really entities anyway) are not reported.</p>
	 * <p/>
	 * <p>All start/endEntity events must be properly nested.
	 *
	 * @param name The name of the entity.  If it is a parameter
	 *             entity, the name will begin with '%', and if it is the
	 *             external DTD subset, it will be "[dtd]".
	 * @throws org.xml.sax.SAXException The application may raise an exception.
	 * @see #endEntity
	 * @see org.xml.sax.ext.DeclHandler#internalEntityDecl
	 * @see org.xml.sax.ext.DeclHandler#externalEntityDecl
	 */
	public void startEntity(String name) throws SAXException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Report the end of an entity.
	 *
	 * @param name The name of the entity that is ending.
	 * @throws org.xml.sax.SAXException The application may raise an exception.
	 * @see #startEntity
	 */
	public void endEntity(String name) throws SAXException {
		//To change body of implemented methods use File | Settings | File Templates.
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
		mCDataSection = true;
	}

	/**
	 * Report the end of a CDATA section.
	 *
	 * @throws org.xml.sax.SAXException The application may raise an exception.
	 * @see #startCDATA
	 */
	public void endCDATA() throws SAXException {
		mCDataSection = false;
	}

	/**
	 * Report an XML comment anywhere in the document.
	 * <p/>
	 * <p>This callback will be used for comments inside or outside the
	 * document element, including comments in the external DTD
	 * subset (if read).  Comments in the DTD must be properly
	 * nested inside start/endDTD and start/endEntity events (if
	 * used).</p>
	 *
	 * @param ch	 An array holding the characters in the comment.
	 * @param start  The starting position in the array.
	 * @param length The number of characters to use from the array.
	 * @throws org.xml.sax.SAXException The application may raise an exception.
	 */
	public void comment(char ch[], int start, int length) throws SAXException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * True if in a CDATA section
	 */
	public boolean parsingCDATA() {
		return mCDataSection;
	}
}
