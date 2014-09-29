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

import p2p.basic.GUID;
import p2p.index.Query;
import p2p.index.Type;

import java.io.Serializable;
import java.util.Vector;
import java.util.Collection;


/**
 * This class represents a query message.
 *
 * @author @author <a href="mailto:Renault John <Renault.John@epfl.ch>">Renault JOHN</a>
 * @version 1.0.0
 */
public abstract class AbstractQuery implements Query, Serializable {

	/**
	 * The finished status (query has been processed).
	 */
	public static final int STATUS_FINISHED = 2;

	/**
	 * The init status (query is created and prepared).
	 */
	public static final int STATUS_INIT = 0;

	/**
	 * The running status (query is processed).
	 */
	public static final int STATUS_RUNNING = 1;

	/**
	 * The message id.
	 */
	protected Vector<GUID> mRefGUIDs = new Vector<GUID>();

	/**
	 * The message id.
	 */
	protected GUID mGUID = null;

	/**
	 * The minimum speed for responding hosts.
	 */
	protected int mMinSpeed = 0;

	/**
	 * The type of search.
	 */
	protected Type mType = null;

	/**
	 * Host who initiated the query
	 */
	protected PGridHost mRequestingHost = null;

	/**
	 * The status.
	 */
	protected int mStatus = STATUS_INIT;

	/**
	 * The index of the search progress (only for Gridella).
	 */
	protected int mIndex = 0;

	/**
	 * Counts the amount of peers involved to route the query.
	 */
	protected int mHops = 0;

	/**
	 * The list of replicas the message was also sent to.
	 */
	protected Vector mReplicas = null;

	/**
	 * Creates a new empty Query.
	 */
	protected AbstractQuery() {
	}

	/**
	 * Creates a new Query with a given search string, path, index and minimum speed.
	 *
	 * @param guid     the Query guid.
	 * @param type     the type of Query.
	 * @param minSpeed the mininum connection speed for responding host.
	 * @param hops     the hop count.
	 */
	protected AbstractQuery(PGridHost host, GUID guid, p2p.index.Type type, int minSpeed, int hops) {
		mRequestingHost = host;
		mGUID = guid;
		mType = type;
		mMinSpeed = minSpeed;
		mHops = hops;
	}

	/**
	 * Returns the message id.
	 *
	 * @return the message id.
	 */
	public GUID getGUID() {
		return mGUID;
	}

	/**
	 * Returns the minimum speed.
	 *
	 * @return the minimum speed.
	 */
	public int getMinSpeed() {
		return mMinSpeed;
	}

	/**
	 * Returns the status.
	 *
	 * @return the status.
	 */
	public int getStatus() {
		return mStatus;
	}

	/**
	 * Sets the status.
	 *
	 * @param status the status.
	 */
	public void setStatus(int status) {
		mStatus = status;
	}

	/**
	 * Returns the type.
	 *
	 * @return the type.
	 */
	public Type getType() {
		return mType;
	}

	/**
	 * @param initialHost The mRequestingHost to set.
	 */
	public void setInitialHost(PGridHost initialHost) {
		mRequestingHost = initialHost;
	}

	/**
	 * Returns a string representation of the type.
	 *
	 * @return the type as string.
	 */
	public String getTypeString() {
		if (mType != null)
			return mType.toString();
		else
			return pgrid.Type.TYPE_STRING;
	}

	/**
	 * Sets the message guid.
	 *
	 * @param guid the message guid.
	 */
	public void setGUID(GUID guid) {
		mGUID = guid;
	}

	/**
	 * Sets the minimum speed.
	 *
	 * @param minSpeed the minimum speed.
	 */
	public void setMinSpeed(int minSpeed) {
		mMinSpeed = minSpeed;
	}

	/**
	 * Sets the type.
	 *
	 * @param type the type.
	 */
	public void setType(Type type) {
		mType = type;
	}

	/**
	 * Return the query string
	 *
	 * @return the query string
	 */
	public String getQueryString() {
		return getLowerBound();
	}

	/**
	 * Returns the search progress.
	 *
	 * @return the search progress.
	 */
	public int getIndex() {
		return mIndex;
	}

	/**
	 * Sets the search progress.
	 *
	 * @param index the search progress.
	 */
	public void setIndex(int index) {
		mIndex = index;
	}

	/**
	 * Returns the amount of peers involved to forward the query.
	 *
	 * @return the hop count.
	 */
	public int getHops() {
		return mHops;
	}

	/**
	 * set the number of hops
	 */
	public void setHops(int hops) {
		mHops = hops;
	}

	/**
	 * Increases the amount of peers involved to forward the query by one.
	 */
	public void incHops() {
		mHops++;
	}

	/**
	 * Returns the replicas this message was already sent too.
	 *
	 * @return the list of replicas.
	 */
	public Vector getReplicas() {
		return mReplicas;
	}

	/**
	 * Sets the list of host the query was sent already too.
	 *
	 * @param replicas list of replicas the query was sent already.
	 */
	public void setReplicas(Vector replicas) {
		mReplicas = replicas;
	}


	public PGridHost getRequestingHost() {
		return mRequestingHost;
	}


	public void setRequestingHost(PGridHost requestingHost) {
		mRequestingHost = requestingHost;
	}

	/**
	 * Add a reference to a specific GUID. All GUID listed in reference list will be sent with P-Grid messages and included
	 * in the query reply message.
	 * If GUIDs ref. are available, they will be used to register query listener instead of the main query GUID
	 * @param ref	a new GUID reference.
	 */
	public void addGUIDReference(GUID ref) {
		mRefGUIDs.add(ref);
	}

	/**
	 * Get all query references GUID. Under certain circonstances, a query can be reprenseted by multiple queries
	 * aggregated locally by the issuer. In this case, <code>getQueryReferences</code> returns the list of all queries
	 * this query refers to. <br/>
	 * A typical usage scenario would be an application which create a query Qmaster(GUID_m) which should be splitted
	 * in n queries, for instance 3. Those 3 sub queries would have GUID_m as reference GUID.
	 * GUID_m will be used to register Search listener instead of each query GUID <code>getGUID</code>, therefor, the
	 * search listener for GUID_m will be called 3 times.
	 *
	 * @return  An empty collection if no references are available or all guid references
	 */
	public Collection<GUID> getQueryReferences() {
		return mRefGUIDs;
	}

	/**
	 * Set all query references GUID to <code>refs</code>. Under certain circonstances, a query can be reprenseted by multiple queries
	 * aggregated locally by the issuer. In this case, <code>getQueryReferences</code> returns the list of all queries
	 * this query refers to. <br/>
	 * A typical usage scenario would be an application which create a query Qmaster(GUID_m) which should be splitted
	 * in n queries, for instance 3. Those 3 sub queries would have GUID_m as reference GUID.
	 * GUID_m will be used to register Search listener instead of each query GUID <code>getGUID</code>, therefor, the
	 * search listener for GUID_m will be called 3 times.
	 *
	 * @return  An empty collection if no references are available or all guid references
	 */
	public void setQueryReferences(Collection<GUID> refs) {
		mRefGUIDs = new Vector<GUID>(refs);
	}

	/**
	 * Return true if the host is responsable for this query
	 * @param host
	 * @return true if responsible
	 */
	public abstract boolean isHostResponsible(PGridHost host);

}