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
 * This class stores unique IDs and provides some basic access methods.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class GUID extends pgrid.util.guid.GUID implements p2p.basic.GUID {

	/**
	 * Construct a new GUID.
	 * This constructor should only be used to create GUIDs, which were never used by other objects again.
	 */
	public GUID() {
		super();
	}

	/**
	 * Constructs an unique ID object from the given string.
	 * This constructor should only be used to create GUIDs, which were never used by other objects again.
	 *
	 * @param v a string representing a GUID
	 */
	public GUID(String v) {
		super(v);
	}

	/**
	 * Construct a new GUID.
	 * This constructor should only be used to create GUIDs, which were never used by other objects again.
	 *
	 * @param seed the seed to be used
	 */
	public GUID(byte[] seed) {
		super(seed);
	}

	/**
	 * Returns a GUID.
	 *
	 * @return the GUID.
	 */
	static public GUID getGUID() {
		GUID g = new GUID();
		return g;
	}

	/**
	 * Returns a GUID for the given string.
	 *
	 * @param guid the string representing the guid.
	 * @return the GUID.
	 */
	static public GUID getGUID(String guid) {
		GUID g = new GUID(guid);
		return g;
	}

	/**
	 * Returns the value of the unique ID.
	 *
	 * @return returns a byte array the represents the unique ID.
	 */
	public byte[] getBytes() {
		return super.getBytes();
	}

	/**
	 * Sets the value of the unique ID.
	 *
	 * @param newId the new value of the unique ID.
	 */
	public void setId(byte[] newId) {
		super.setId(newId);
	}

	/**
	 * Returns a unique string representation of this unique ID. The byte array
	 * that represents the unique ID is stepped through byte per byte and each
	 * byte is converted into its hex representation (padded with leading
	 * zeros).
	 *
	 * @return the unique string representation of the unique ID.
	 */
	public String toString() {
		return super.toString();
	}

	/**
	 * Compares this object with the specified object for order. Returns a negative integer, zero, or a positive integer
	 * as this object is less than, equal to, or greater than the specified object.
	 *
	 * @param obj the Object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the
	 *         specified object.
	 */
	public int compareTo(Object obj) {
		return super.compareTo(obj);
	}

	/**
	 * Compares two unique IDs for equality. The result is <code>true</code> if
	 * and only if the argument is not null and is a <code>GUID</code> object
	 * that represents the same unique id as this object.
	 *
	 * @param obj the object to compare with.
	 * @return <code>true</code> if the objects are the same; false otherwise.
	 */
	public boolean equals(GUID obj) {
		return super.equals(obj);
	}

	/**
	 * Compares two unique IDs for equality. The result is <code>true</code> if
	 * and only if the argument is not null and is a <code>GUID</code> object
	 * that represents the same unique id as this object.
	 *
	 * @param obj the object to compare with.
	 * @return <code>true</code> if the objects are the same; false otherwise.
	 */
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	/**
	 * Returns a hash code value for this unique id based on its value.
	 *
	 * @return a hash code value for this unique id.
	 */
	public int hashCode() {
		return super.hashCode();
	}

	/**
	 * Returns the size of this GUID.
	 *
	 * @return the size of this GUID.
	 */
	public int getSize() {
		return super.getSize();
	}


}