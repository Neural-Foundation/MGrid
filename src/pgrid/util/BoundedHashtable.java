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

import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

/**
 * The Bounded Hashtable is a FIFO queue bounded to a given amount of elements.
 * A full queue will remove the first element to add the new element.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class BoundedHashtable extends Hashtable {

	/**
	 * The default capacity.
	 */
	public static final int DEFAULT_CAPACITY = 100;

	/**
	 * The capacity.
	 */
	private int mCapacity = DEFAULT_CAPACITY;

	/**
	 * The add history.
	 */
	private Vector mHistory = new Vector();

	/**
	 * Creates a new bounded queue with the default capacity.
	 */
	public BoundedHashtable() {
		super(DEFAULT_CAPACITY);
	}

	/**
	 * Creates a new bounded queue with the given capacity.
	 *
	 * @param capacity the capacity.
	 */
	public BoundedHashtable(int capacity) {
		super(capacity);
		mCapacity = capacity;
	}

	/**
	 * Clears this hashtable so that it contains no keys.
	 */
	public synchronized void clear() {
		mHistory.clear();
		super.clear();
	}

	/**
	 * Creates a shallow copy of this hashtable. All the structure of the
	 * hashtable itself is copied, but the keys and values are not cloned.
	 * This is a relatively expensive operation.
	 *
	 * @return a clone of the hashtable.
	 */
	public synchronized Object clone() {
		try {
			return Cloner.clone(this);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Maps the specified <code>key</code> to the specified
	 * <code>value</code> in this hashtable. Neither the key nor the
	 * value can be <code>null</code>. <p>
	 * <p/>
	 * The value can be retrieved by calling the <code>get</code> method
	 * with a key that is equal to the original key.
	 *
	 * @param key   the hashtable key.
	 * @param value the value.
	 * @return the previous value of the specified key in this hashtable,
	 *         or <code>null</code> if it did not have one.
	 * @throws java.lang.NullPointerException if the key or value is
	 *                                        <code>null</code>.
	 * @see java.lang.Object#equals(java.lang.Object)
	 * @see #get(java.lang.Object)
	 */
	public synchronized Object put(Object key, Object value) {
		if (super.size() == mCapacity)
			super.remove(mHistory.remove(0));
		mHistory.add(value);
		return super.put(key, value);
	}

	/**
	 * Copies all of the mappings from the specified Map to this Hashtable
	 * These mappings will replace any mappings that this Hashtable had for any
	 * of the keys currently in the specified Map.
	 *
	 * @param t Mappings to be stored in this map.
	 * @throws java.lang.NullPointerException if the specified map is null.
	 * @since 1.2
	 */
	public synchronized void putAll(Map t) {
		if ((super.size() + t.size()) >= mCapacity) {
			for (int i = super.size() + t.size(); i >= mCapacity; i--)
				super.remove(mHistory.remove(0));
		}
		mHistory.addAll(t.values());
		super.putAll(t);
	}

	/**
	 * Removes the key (and its corresponding value) from this
	 * hashtable. This method does nothing if the key is not in the hashtable.
	 *
	 * @param key the key that needs to be removed.
	 * @return the value to which the key had been mapped in this hashtable,
	 *         or <code>null</code> if the key did not have a mMapping.
	 * @throws java.lang.NullPointerException if the key is <code>null</code>.
	 */
	public synchronized Object remove(Object key) {
		Object o = super.remove(key);
		mHistory.remove(o);
		return o;
	}

}
