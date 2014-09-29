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

import p2p.basic.Key;
import p2p.basic.KeyRange;
import pgrid.util.PathComparator;

/**
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.2.0
 */
public class PGridKeyRange implements KeyRange {

	/**
	 * Bound string separator used in the toString method
	 */
	public static final String SEPARATOR = "-";

	/**
	 * Path comparator
	 */
	protected static PathComparator pathComparator = new PathComparator();

	/**
	 * Lower bound of the range
	 */
	protected Key mLowerKey;

	/**
	 * Higher bound of the range
	 */
	protected Key mHigherKey;

	/**
	 * Constructor for a key range
	 *
	 * @param lower  bound of the range
	 * @param higher bound of the range
	 */
	public PGridKeyRange(Key lower, Key higher) {
		mLowerKey = lower;
		mHigherKey = higher;
	}

	/**
	 * Constructor for a key range out of a string
	 *
	 * @param key  the key range
	 */
	public PGridKeyRange(String key) {
		String[] keys = key.split(PGridKeyRange.SEPARATOR);

		mLowerKey = new PGridKey(keys[0]);
		mHigherKey = new PGridKey(keys[1]);
	}

	/**
	 * @see p2p.basic.KeyRange#getMin()
	 */
	public Key getMin() {
		return mLowerKey;
	}

	/**
	 * @see p2p.basic.KeyRange#getMax()
	 */
	public Key getMax() {
		return mHigherKey;
	}

	public String toString() {
		return mLowerKey.toString()+SEPARATOR+mHigherKey.toString();
	}

	/**
	 * @see p2p.basic.KeyRange#withinRange(p2p.basic.Key)
	 */
	public boolean withinRange(Key key) {
		String strKey = key.toString();

		return ((pathComparator.compare(strKey, mLowerKey.toString()) >= 0 &&
				pathComparator.compare(strKey, mHigherKey.toString()) <= 0));
	}

}
