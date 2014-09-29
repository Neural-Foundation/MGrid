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
package pgrid.core.maintenance.identity;

import p2p.basic.Key;
import p2p.index.Type;
import pgrid.IndexEntry;
import pgrid.PGridHost;

/**
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */
public abstract class IdentityIndexEntry extends IndexEntry {

	/**
	 * Public key of the remote host
	 */
	protected String mPublicKey = null;

	/**
	 * Encoded signature containing the ID, address, public key, and timestamp
	 */
	protected String mSignature = null;

	/**
	 * Timestamp of insertion. Used agains DoS attack
	 */
	protected long mTimeStamp;

	/**
	 * Signature separator
	 */
	public String SEPARATOR = "\t";

	/**
	 * Empty constructor
	 */
	public IdentityIndexEntry() {

	}

	/**
	 * Create a new IdentityIndexEntry
	 *
	 * @param host      the storing host.
	 * @param key       the key for this file name.
	 * @param publicKey The public key of the host
	 * @param timestamp The timestamp of the host
	 */
	public IdentityIndexEntry(Type type, PGridHost host, Key key, String publicKey, long timestamp, String desc) {
		super(type, host, key, desc);
		mPublicKey = publicKey;
		mTimeStamp = timestamp;
		mSignature = host.toString() + SEPARATOR + timestamp;

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
		if (obj == null)
			return Integer.MIN_VALUE;
		if (obj.getClass() != this.getClass())
			return Integer.MIN_VALUE;
		IndexEntry item = (IndexEntry)obj;
		return (mHost.getGUID().toString() + AT + mPublicKey + mSignature + mTimeStamp + COLON + mKey)
				.compareTo(item.getPeer().getGUID().toString() + AT + mPublicKey + mSignature + mTimeStamp + COLON + item.getKey());
	}


	public int hashCode() {
		return (mHost.getGUID().toString() + AT + mPublicKey + mSignature + mTimeStamp + COLON + mKey).hashCode();
	}

	/**
	 * Tests if the delivered item is equal to this.
	 *
	 * @param item the item to compare.
	 * @return ip address of the storing host.
	 */
	public boolean isEqual(IdentityIndexEntry item) {
		if (item == null)
			return false;
		if ((item.getType().equals(mType)) &&
				(item.getPeer().equals(mHost)) &&
				(item.getSignature().equals(mSignature)) &&
				(item.getPublicKey().equals(mPublicKey)) &&
				(item.getTimeStamp() == mTimeStamp))
			return true;
		else
			return false;
	}

	/**
	 * Returns the public key
	 *
	 * @return the public key
	 */
	public String getPublicKey() {
		return mPublicKey;
	}

	/**
	 * Returns the signature
	 *
	 * @return the signature
	 */
	public String getSignature() {
		return mSignature;
	}

	/**
	 * Set the signature
	 */
	public void setSignature(String signature) {
		mSignature = signature;
	}

	/**
	 * Returns the timestamp
	 *
	 * @return the timestamp
	 */
	public long getTimeStamp() {
		return mTimeStamp;
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
	public int compareTo(Object obj, boolean withKey) {
		if (obj == null)
			return Integer.MIN_VALUE;
		if (obj.getClass() != this.getClass())
			return Integer.MIN_VALUE;
		IdentityIndexEntry item = (IdentityIndexEntry)obj;

		String localKey = "";
		String remoteKey = "";
		if (withKey) {
			localKey = COLON + mKey;
			remoteKey = COLON + item.getKey();
		}

		return (mHost.getGUID().toString() + AT + getSignature() + localKey).compareTo(item.getPeer().getGUID().toString() + AT + item.getSignature() + remoteKey);
	}

}
