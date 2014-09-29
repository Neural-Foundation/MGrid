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

import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This <code>Tokenizer</code> uses the Java {@link java.util.StringTokenizer} to tokenize given strings by a given
 * separator.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0 2003/03/14
 * @see java.util.StringTokenizer
 */
public class Tokenizer {

	/**
	 * Create a new <code>Tokenizer</code>.
	 */
	protected Tokenizer() {
		// do nothing
	}

	/**
	 * Splits the given string by whitespaces in an array of strings.
	 *
	 * @param input the string to split.
	 * @return the delivered string splitted in an array of strings.
	 */
	public static String[] tokenize(String input) {
		return tokenize(input, " ");
	}

	/**
	 * Splits the given string by a given separator in an array of strings.
	 *
	 * @param input     the string to split.
	 * @param separator the splitting string.
	 * @return the delivered string splitted in an array of strings.
	 */
	public static String[] tokenize(String input, String separator) {
		Vector vector = new Vector();
		StringTokenizer strTokens = new StringTokenizer(input, separator);
		String[] strings;

		while (strTokens.hasMoreTokens())
			vector.addElement(strTokens.nextToken());
		strings = new String[vector.size()];
		for (int i = 0; i < strings.length; i++)
			strings[i] = (String)vector.get(i);
		return strings;
	}

}