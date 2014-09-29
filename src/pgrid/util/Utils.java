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

import pgrid.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import com.google.common.primitives.Ints;
import com.google.uzaygezen.core.BitVector;
import com.google.uzaygezen.core.BitVectorFactories;
import com.google.uzaygezen.core.CompactHilbertCurve;
import com.google.uzaygezen.core.MultiDimensionalSpec;
import com.google.uzaygezen.core.SpaceFillingCurve;

/**
 * This class causes periodic Exchanges.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class Utils {

	/**
	 * The antilog table file.
	 */
	private static final String ANTILOG_FILE = "antilogTable.dat";

	/**
	 * The log table file.
	 */
	private static final String LOG_FILE = "logTable.dat";

	/**
	 * The antilog table.
	 */
	private static int[] mAntilogTable = null;

	/**
	 * The log table.
	 */
	private static int[] mLogTable = null;

	/**
	 * Constructs the Utils facility.
	 */
	public Utils() {
		if ((mAntilogTable == null) || (mLogTable == null)) {
			mAntilogTable = new int[131071];
			mLogTable = new int[65536];
			try {
				// read antilog table
				InputStream inStream = getClass().getResourceAsStream("/" + ANTILOG_FILE);
				if (inStream == null) {
					Constants.LOGGER.severe("Antilog table '" + ANTILOG_FILE + "' not found!");
					System.exit(-1);
				}
				BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
				String inputLine;
				int idx = 1;
				while ((inputLine = in.readLine()) != null) {
					mAntilogTable[idx++] = Integer.parseInt(inputLine);
				}
				in.close();

				// read log table
				inStream = getClass().getResourceAsStream("/" + LOG_FILE);
				if (inStream == null) {
					Constants.LOGGER.severe("Log table '" + LOG_FILE + "' not found!");
					System.exit(-1);
				}
				in = new BufferedReader(new InputStreamReader(inStream));
				idx = 1;
				while ((inputLine = in.readLine()) != null) {
					mLogTable[idx++] = Integer.parseInt(inputLine);
				}
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	/**
	 * Returns the common prefix of two deliverd strings.
	 *
	 * @param str1 the first string.
	 * @param str2 the second string.
	 * @return the common prefix string.
	 */
	public static String commonPrefix(String str1, String str2) {
		if ((str1.length() == 0) || (str2.length() == 0))
			return "";
		String prefix = "";
		int length;
		if (str1.length() < str2.length()) {
			length = str1.length();
		} else {
			length = str2.length();
		}
		for (int i = 1; i <= length; i++) {
			if (str1.substring(0, i).equals(str2.substring(0, i))) {
				prefix = str1.substring(0, i);
			} else {
				break;
			}
		}
		return prefix;
	}


}