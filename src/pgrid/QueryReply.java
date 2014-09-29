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

import java.util.Collection;
import java.util.Vector;

/**
 * This class represents a query reply.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class QueryReply {

	/**
	 * The Query reply type Bad Request.
	 */
	public static final int TYPE_BAD_REQUEST = 2;

	/**
	 * The Query reply type File Not Found.
	 */
	public static final int TYPE_NOT_FOUND = 1;

	/**
	 * The Query reply type OK.
	 */
	public static final int TYPE_OK = 0;

	/**
	 * The Query Id.
	 */
	protected p2p.basic.GUID mGUID = null;

	/**
	 * The result set.
	 */
	protected Collection mResultSet = null;

	/**
	 * The Query reply.
	 */
	protected int mType = -1;
	
	
	private int mHits ;

	/**
	 * Creates an empty query reply.
	 */
	public QueryReply() {
	}

	/**
	 * Creates a new query reply with given values.
	 *
	 * @param guid      the GUID of the Query Reply.
	 * @param type      the type of query reply.
	 * @param resultSet the result set of found files.
	 */
	public QueryReply(GUID guid, int type, Collection resultSet) {
		mGUID = guid;
		mType = type;
		mResultSet = resultSet;
	}

	/**
	 * Creates a new query reply with given values.
	 *
	 * @param guid      the GUID of the Query Reply.
	 * @param type      the type of query reply.
	 * @param hits 			the number of hits of Query Reply
	 */
	public QueryReply(GUID guid, int type, int hits) {
		mGUID = guid;
		mType = type;
		mHits = hits;
	}

	/**
	 * Returns the number of hits.
	 *
	 * @return the number of hits.
	 */
	public int getHits() {
		if (mResultSet == null)
			return 0;
		return mResultSet.size();
	}

	/**
	 * Returns the query id.
	 *
	 * @return the query id.
	 */
	public GUID getGUID() {
		return mGUID;
	}

	/**
	 * Returns a result of the result set, selected by the index.
	 *
	 * @param index the index of the result in the result set.
	 * @return the result.
	 */
	public IndexEntry getResult(int index) {
		return (IndexEntry)new Vector(mResultSet).get(index);
	}

	/**
	 * Returns the result set.
	 *
	 * @return the result set.
	 */
	public Collection getResultSet() {
		return mResultSet;
	}

	/**
	 * Set the result set.
	 */
	public void setResultSet(Collection r) {
		mResultSet = r;
	}

	/**
	 * Returns the query reply hits.
	 *
	 * @return the query reply hits.
	 */
	public int getmHits() {
		return mHits;
	}

	/**
	 * Set the query reply type.
	 */
	public void setmHits(int hits) {
		mHits = hits;
	}
	/**
	 * Returns the query reply type.
	 *
	 * @return the query reply type.
	 */
	public int getType() {
		return mType;
	}

	/**
	 * Set the query reply type.
	 */
	public void setType(int type) {
		mType = type;
	}
}