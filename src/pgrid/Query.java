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
import p2p.basic.Key;
import p2p.basic.KeyRange;
import p2p.index.TypeHandler;
import p2p.index.Type;

import pgrid.interfaces.basic.PGridP2PFactory;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.core.index.IndexManager;

/**
 * This class represents a query message.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class Query extends AbstractQuery implements ExactQueryInterface {

	/**
	 * The search string.
	 */
	protected String mQueryString = null;

	/**
	 * The key (binary represantion of the search string).
	 */
	protected KeyRange mKey = null;

	/**
	 * Storage manager
	 */
	protected IndexManager mIndexMng = PGridP2P.sharedInstance().getIndexManager();

	/**
	 * Creates a new empty Query.
	 */
	public Query() {
		mRequestingHost = PGridP2P.sharedInstance().getLocalHost();
	}

	/**
	 * Creates a new Query for a given search string.
	 *
	 * @param type  the type of Query.
	 * @param query the search string.
	 * @param key   the key.
	 */
	public Query(PGridHost host, Type type, String query, Key key) {
		this(host, type, query, 0);
		mKey = new PGridKeyRange(key, key);
	}

	/**
	 * Creates a new Query for a given search string.
	 *
	 * @param type     the type of Query.
	 * @param query    the search string.
	 * @param minSpeed the mininum connection speed for responding host.
	 */
	public Query(PGridHost host, Type type, String query, int minSpeed) {
		mGUID = new pgrid.GUID();
		mType = type;
		mQueryString = query;
		mMinSpeed = minSpeed;
		mRequestingHost = host;


		if (mKey == null) {
			TypeHandler th = mIndexMng.getTypeHandler(type);

			if (th != null) {
				mKey = th.generateKeyRange(query, query);
			} else {
				mKey = PGridP2PFactory.sharedInstance().generateKeyRange(query,query);
			}
		}
	}

	/**
	 * Creates a new Query with a given search string, path, index and minimum speed.
	 *
	 * @param guid     the Query guid.
	 * @param type     the type of Query.
	 * @param query    the search string.
	 * @param key      the key (binary represantion of the search string).
	 * @param index    the search progress.
	 * @param minSpeed the mininum connection speed for responding host.
	 * @param hops     the hop count.
	 */
	public Query(PGridHost host, GUID guid, p2p.index.Type type, String query, Key key, int index, int minSpeed, int hops) {
		super(host, guid, type, minSpeed, hops);
		mQueryString = query;
		mKey = new PGridKeyRange(key, key);
		mIndex = index;
	}

	/**
	 * Returns the key (binary represantion of the search string).
	 *
	 * @return the key.
	 */
	public KeyRange getKeyRange() {
		if (mKey == null) {
			Key key = IndexManager.getInstance().generateKey(mType, mQueryString);
			mKey = new PGridKeyRange(key,key);
		}
		return mKey;
	}

	/**
	 * Sets the key (binary represantion of the search string).
	 *
	 * @param key the key.
	 */
	public void setKey(Key key) {
		mKey = new PGridKeyRange(key, key);
	}

	/**
	 * Returns the search string.
	 *
	 * @return the search string.
	 */
	public String getQueryString() {
		return (mQueryString != null ? mQueryString : "");
	}

	/**
	 * @see p2p.index.Query#getLowerBound()
	 */
	public String getLowerBound() {
		return getQueryString();
	}

	/**
	 * @see p2p.index.Query#getHigherBound()
	 */
	public String getHigherBound() {
		return getQueryString();
	}

	/**
	 * Sets the search string.
	 *
	 * @param query the search string.
	 */
	public void setQueryString(String query) {
		mQueryString = query;
	}

	public String getRepresentation() {
		return getQueryString();
	}

	/**
	 * Return true if the host is responsable for this query
	 * @param host
	 * @return true if the host is responsable for this query
	 */
	public boolean isHostResponsible(PGridHost host) {
		String compath = pgrid.util.Utils.commonPrefix(mKey.toString(), host.getPath());
		return ((compath.length() == mKey.getMin().size()) || (compath.length() == host.getPath().length()));
	}

	@Override
	public String getOriginalMin() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getOriginalMax() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getOrigxMin() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getOrigxMax() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getOrigyMin() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getOrigyMax() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getHits() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long setHits() {
		// TODO Auto-generated method stub
		return null;
	}

}