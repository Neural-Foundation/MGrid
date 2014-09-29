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

import java.io.Serializable;

/**
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.2.0
 */
public class PGridKey implements Key, Serializable {

	/**
	 * String representation of the key
	 */
	protected String mKey;

	/**
	 * Constructor
	 *
	 * @param key
	 */
	public PGridKey(String key) {
		mKey = key;
	}

	/**
	 * @see p2p.basic.Key#getBytes()
	 */
	public byte[] getBytes() {
		return mKey.getBytes();
	}

	/**
	 * Append two keys together
	 *
	 * @param toAppend the key to append
	 * @return the new key
	 */
	public Key append(Key toAppend) {
		mKey = mKey + toAppend.toString();
		return this;
	}

	/**
	 * Append two keys together
	 *
	 * @param toAppend the key to append
	 * @return the new key
	 */
	public Key append(String toAppend) {
		mKey = mKey + toAppend;
		return this;
	}

	/**
	 * @see p2p.basic.Key#size()
	 */
	public int size() {
		return mKey.length();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return mKey;
	}

	public boolean equals(Object o) {
		if (!(o instanceof PGridKey)) 
			return false;
		return ((PGridKey)o).mKey.equals(mKey);
	}

}
