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


package p2p.basic;

import java.io.Serializable;

/**
 * Represents a part of the key space, for example, the keys a
 * node is responsible for. It may be used both for range or
 * prefix-based addressing.
 */

public interface KeyRange extends Serializable {
	/**
	 * Get the lower bound of the key range.
	 *
	 * @return the first key in the range
	 */
	Key getMin();

	/**
	 * Get the upper bound of the key range.
	 *
	 * @return the last key in the range
	 */
	Key getMax();

	/**
	 * Check whether key is inside the range.
	 *
	 * @param key the key to test
	 * @return true if key is inside the key range, false otherwise
	 */
	boolean withinRange(Key key);
}
