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
package test.gridella;


import p2p.basic.Key;
import p2p.basic.GUID;
import p2p.basic.Peer;
import p2p.index.Type;

import java.io.File;

/**
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */
public abstract class FileIndexEntry extends pgrid.interfaces.index.XMLSimpleIndexEntry {

	/**
	 * Data item type
	 */
	private Type mFileType = null;

	/**
	 * The file for this data item.
	 */
	private File mFile = null;

	/**
	 * The extra informations for this file.
	 */
	protected String mInfos = "";

	/**
	 * The file name.
	 */
	protected String mName = "";

	/**
	 * The desc.
	 */
	protected String mDesc = "";

	/**
	 * The file path.
	 */
	protected String mPath = "";

	/**
	 * The QoS for this file.
	 */
	protected int mQoS = 0;

	/**
	 * The file size.
	 */
	protected int mSize = 0;

	/**
	 * Creates a new empty PGridP2P data item.
	 */
	protected FileIndexEntry() {
	}

	/**
	 * Create a new IdentityIndexEntry
	 *
	 * @param guid      the unique id.
	 * @param type      the data type.
	 * @param key       the key for this file name.
	 * @param peer      the storing peer.
	 * @param data      the data.
	 */
	public FileIndexEntry(GUID guid, Type type, Key key, Peer peer, Object data) {
		super(guid, type, key, peer, data);
		setGUID(pgrid.GUID.getGUID(guid.toString()));
	}

	/**
	 * Creates a new PGridP2P data item with all parameters.
	 *
	 * @param host  the storing host.
	 * @param qoS   the QoS of the storing host.
	 * @param key   the key for this file name.
	 * @param path  the file path.
	 * @param name  the file name.
	 * @param size  the file size.
	 * @param infos the extra informations for this file.
	 * @param desc  the file description.
	 */
	/*public FileIndexEntry(PGridHost host, int qoS, Key key, String path, String name, int size, String infos, String desc) {
		super((pgrid.DataType)mFileType, host, key, desc);
		mQoS = qoS;
		mPath = path;
		mName = name;
		mSize = size;
		mInfos = infos;
	}*/

	/**
	 * Compares this object with the specified object for order. Returns a negative integer, zero, or a positive integer
	 * as this object is less than, equal to, or greater than the specified object.
	 *
	 * @param obj     the Object to be compared.
	 * @param withKey if we should take the key in account
	 * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the
	 *         specified object.
	 */
	public int compareTo(Object obj, boolean withKey) {
		if (obj == null)
			return Integer.MIN_VALUE;
		if (obj.getClass() != this.getClass())
			return Integer.MIN_VALUE;
		FileIndexEntry item = (FileIndexEntry)obj;

		String localKey = "";
		String remoteKey = "";
		if (withKey) {
			localKey = COLON + mKey;
			remoteKey = COLON + item.getKey();
		}
		//return (mHost.getGUID().toString() + AT + localKey + COLON + mData).compareTo(item.getPeer().getGUID().toString() + AT + remoteKey + COLON + item.getData());
		return (mHost.getGUID().toString() + AT + getPath() + getName() + getData() + localKey).compareTo(item.getPeer().getGUID().toString() + AT + item.getPath() + item.getName() + item.getData() + remoteKey);
	}


	public boolean equals(Object obj) {
		return (compareTo(obj) == 0);
	}

	public int hashCode() {
		return (mHost.getGUID().toString() + AT + getPath() + getName() + getData() + COLON + mKey).hashCode();
	}

	/**
	 * Tests if the delivered item is equal to this.
	 *
	 * @param item the item to compare.
	 * @return ip address of the storing host.
	 */
	public boolean isEqual(FileIndexEntry item) {
		if (item == null)
			return false;
		if ((item.getType().equals(mType)) &&
				(item.getPeer().equals(mHost)) &&
				(item.getQoS() == mQoS) &&
				(item.getPath().equals(mPath)) &&
				(item.getName().equals(mName)) &&
				(item.getSize() == mSize))
			return true;
		else
			return false;
	}

	/**
	 * Returns the data.
	 *
	 * @return the data.
	 */
	public Object getData() {
		return mData;
	}

	/**
	 * Returns the local file.
	 *
	 * @return the local file.
	 */
	public File getFile() {
		return mFile;
	}

	/**
	 * Sets the local file.
	 *
	 * @param file the local file.
	 */
	public void setFile(File file) {
		mFile = file;
	}

	/**
	 * Returns the extra file informations.
	 *
	 * @return the infos.
	 */
	public String getInfos() {
		return mInfos;
	}

	/**
	 * Set the extra file informations.
	 *
	 * @param info infos.
	 */
	public void setInfos(String info) {
		mInfos=info;
	}

	/**
	 * Returns the file name.
	 *
	 * @return the file name.
	 */
	public String getName() {
		return (mName != null ? mName : "");
	}

	/**
	 * Sets the filename.
	 *
	 * @param filename the filename.
	 */
	public void setName(String filename) {
		mName = filename;
	}

	/**
	 * Returns the file path.
	 *
	 * @return the file path.
	 */
	public String getPath() {
		return mPath;
	}

	/**
	 * set the file path.
	 *
	 * @param path the file path.
	 */
	public void setPath(String path) {
		mPath = path;
	}

	/**
	 * Returns the QoS of the storing host.
	 *
	 * @return the QoS of the storing host.
	 */
	public int getQoS() {
		return mQoS;
	}

	/**
	 * Set the QoS of the storing host.
	 *
	 */
	public void setQoS(int qos) {
		mQoS = qos;
	}

	public void setSize(int size) {
		this.mSize = size;
	}



	/**
	 * Returns the file size.
	 *
	 * @return the file size.
	 */
	public int getSize() {
		return mSize;
	}

	/**
	 * @see pgrid.IndexEntry#getSignature()
	 */
	public String getSignature() {
		return getPeer().getGUID() + "\t" + getKey() + "\t" + getPath() + "\t" + getName();
	}

	public String getDesc() {
		return mDesc;
	}

	public void setDesc(String desc) {
		this.mDesc = desc;
	}



}
