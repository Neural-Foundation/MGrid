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

package pgrid;

/**
 * This interface represents an XMLIndexEntry
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 */
public interface XMLIndexEntry extends XMLizable {

	/**
	 * A part of the XML string.
	 */
	public static final String XML_INDEX_ITEM = "IndexEntry";

	/**
	 * A part of the XML string.
	 */
//	public static final String XML_INDEX_ITEM_DATA = "Data";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_INDEX_ITEM_GUID = "GUID";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_INDEX_ITEM_KEY = "Key";

	/**
	 * A part of the XML string.
	 */
//	public static final String XML_INDEX_ITEM_TYPE = "Type";
	
	/**
	 * A part of the XML string.
	 */
//	public static final String XML_INDEX_ITEM_X = "X";
	
	/**
	 * A part of the XML string.
	 */
//	public static final String XML_INDEX_ITEM_Y = "Y";
	
	/**
	 * A part of the XML string.
	 */
	public static final String XML_INDEX_ITEM_ID = "ID";
	
	/**
	 * A part of the XML string.
	 */
	public static final String XML_NUMBER_OF_HITS = "Hits";

	/**
	 * Clear content of this object. This method can be used in situation where only a single object is used for
	 * decoding multiple objects.
	 */
	public void clear();

	/**
	 * Returns a string represantation of this result set.
	 *
	 * @param prefix    a string prefix for each line.
	 * @param newLine   the string for a new line, e.g. \n.
	 * @param signature add a signature
	 * @return a string represantation of this result set.
	 */
	public String toXMLString(String prefix, String newLine, boolean signature);

}