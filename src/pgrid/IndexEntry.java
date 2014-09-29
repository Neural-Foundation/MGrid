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

import mgrid.core.MGridUtils;
import mgrid.core.Point;
import p2p.basic.Key;
import p2p.basic.GUID;
import p2p.basic.Peer;
import p2p.index.Type;
import pgrid.util.LexicalDefaultHandler;

/**
 * This class represents an index entry.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public abstract class IndexEntry extends LexicalDefaultHandler implements p2p.index.IndexEntry, Comparable, Cloneable {

	/**
	 * The '@' symbol.
	 */
	protected static final String AT = "@";

	/**
	 * The ':' symbol.
	 */
	protected static final String COLON = ":";

	/**
	 * The data item description.
	 */
	protected String mData = null;

	/**
	 * The Point item description.
	 */
	
	 protected Point mPoint = null;

	 /**
	 * The Point:X item description.
	*/
		
	 protected Long mPointX = null;
	 
	/**
	* The Point:Y item description.
	*/
			
	protected Long mPointY = null;
		 
	/**
	 * The Point:X item description.
	*/
				
	protected Long mPointID = null;

	/**
	 * The global unique id of the data item.
	 */
	protected GUID mGUID = null;

	/**
	 * The storing host.
	 */
	protected PGridHost mHost = null;

	/**
	 * The key for this file.
	 */
	protected Key mKey = null;

	/**
	 * The type of the file.
	 */
	protected p2p.index.Type mType = null;

	/**
	 * Creates a new empty PGridP2P data item.
	 */
	protected IndexEntry() {
	}

	/**
	 * Creates a new PGridP2P data item with all parameters.
	 *
	 * @param type the type of entry.
	 * @param host the storing host.
	 * @param key  the key for this file name.
	 * @param data the data.
	 */
	public IndexEntry(Type type, PGridHost host, Key key, String data) {
		mGUID = pgrid.GUID.getGUID();
		mType = type;
		mHost = host;
		mKey = key;
		mData = data;
	}
	
	/**
	 * Creates a new PGridP2P data item with all parameters.
	 *
	 * @param type the type of entry.
	 * @param host the storing host.
	 * @param key  the key for this file name.
	 * @param data the data.
	 */
	public IndexEntry(Type type, PGridHost host, Key key, Point data) {
		mGUID = pgrid.GUID.getGUID();
		mType = type;
		mHost = host;
		mKey = key;
		mPoint = data;
		mData = key.toString();
	}

	public boolean equals(Object obj) {
		return (compareTo(obj) == 0);
	}

	/**
	 * Returns the global unique id.
	 *
	 * @return the id.
	 */
	public GUID getGUID() {
		return mGUID;
	}

	/**
	 * Sets the global unique id.
	 *
	 * @param guid the id.
	 */
	public void setGUID(GUID guid) {
		mGUID = guid;
	}

	/**
	 * Returns the the storing host.
	 *
	 * @return the the storing host.
	 */
	public Peer getPeer() {
		return mHost;
	}

	/**
	 * Sets the storing host.
	 *
	 * @param host the host.
	 */
	public void setPeer(PGridHost host) {
		mHost = host;
	}

	/**
	 * Returns the key for the file name.
	 *
	 * @return the key.
	 */
	public Key getKey() {
		return mKey;
	}

	/**
	 * Sets the key for the file name.
	 *
	 * @param key the key.
	 */
	public void setKey(Key key) {
		mKey = key;
	}


	/**
	 * Returns the type of entry.
	 *
	 * @return the type.
	 */
	public Type getType() {
		return mType;
	}

	/**
	 * Returns a string representation of the data item's type.
	 *
	 * @return the type string.
	 */
	public String getTypeString() {
		if (mType == null)
			return pgrid.Type.TYPE_STRING;
		else
			return mType.toString();
	}

	/**
	 * Sets the data item type.
	 *
	 * @param type the type.
	 */
	public void setType(Type type) {
		mType = type;
	}

	/**
	 * Returns the file description.
	 *
	 * @return the file description.
	 */
	public Object getData() {
		return mData;
	}


	/**
	 * Returns the Point
	 *
	 * @return Point.
	 */
	public Point getPoint() {
		return mPoint;
	}
	
	/**
	 * Sets the point
	 *
	 * @param point the point
	 */
	public void setPoint(Point point) {
		mPoint = point;
	}
	
	public Long getmPointX() {
		return mPointX;
	}

	public void setmPointX(Long mPointX) {
		this.mPointX = mPointX;
	}

	public Long getmPointY() {
		return mPointY;
	}

	public void setmPointY(Long mPointY) {
		this.mPointY = mPointY;
	}

	public Long getmPointID() {
		return mPointID;
	}

	public void setmPointID(Long mPointID) {
		this.mPointID = mPointID;
	}

	/**
	 * Sets the data.
	 *
	 * @param data the data.
	 */
	public void setData(Object data) {
		mData = (String)data;
	}



	/**
	 * returns the signature of this data item.
	 *
	 * @return the signature of this data item
	 */
	public abstract String getSignature();

	/**
	 * Compares this object with the specified object for order. Returns a negative integer, zero, or a positive integer
	 * as this object is less than, equal to, or greater than the specified object.
	 *
	 * @param obj the Object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the
	 *         specified object.
	 */
	public int compareTo(Object obj) {
		return compareTo(obj, true);
	}

	/**
	 * Compares this object with the specified object for order. Returns a negative integer, zero, or a positive integer
	 * as this object is less than, equal to, or greater than the specified object.
	 *
	 * @param obj     the Object to be compared.
	 * @param withKey if we should take the key in account
	 * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the
	 *         specified object.
	 */
	abstract public int compareTo(Object obj, boolean withKey);


	public Object clone() {
		Object o = null;
		try {
			o = super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}

		return o;
	}

}