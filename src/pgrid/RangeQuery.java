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
import p2p.basic.KeyRange;
import p2p.index.Type;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.interfaces.basic.PGridP2PFactory;

/**
 * This class represent a range query message
 *
 * @author <a href="mailto:Renault JOHN <renault.john@epfl.ch>">Renault JOHN</a>
 */
public class RangeQuery extends AbstractQuery implements RangeQueryInterface {

	/**
	 * Sequential algorithm which consist in finding dynamicaly
	 * the next neighbor and send the range query to it.
	 */
	public static String MINMAX_ALGORITHM = "MinMax";

	/**
	 * Parallel algorithm which consist in sending the range query
	 * to all sub-tree this peer is responsible of.
	 */
	public static String SHOWER_ALGORITHM = "Shower";

	/**
	 * Bounds keys of the range query
	 */
	protected KeyRange mBoundsKeys = null;

	/**
	 * The first search string.
	 */
	protected String mFirstQueryString = null;

	/**
	 * The second search string.
	 */
	protected String mSecondQueryString = null;

	/**
	 * Indicate which algorithm to choose. Legal values are  sequential or parallel
	 */
	protected String mAlgorithm = null;

	/**
	 * prefix of the expected path for the current range query
	 */
	protected String mPrefix = "";

	/**
	 * Range query separator
	 */
	public static final String SEPARATOR = "@:-:@";

	/**
	 * original X lower bound
	 */
	protected Long morigxMin; 
	
	/**
	 * original X upper bound
	 */
	protected Long morigxMax;
	
	/**
	 * original Y lower bound
	 */
	protected Long morigyMin;
	
	/**
	 * original Y upper bound
	 */
	protected Long morigyMax; 
	
	/**
	 * number of hits returned by range query
	 */
	protected Long mHits;
	
	
	/**
	 * Creates a new empty range Query.
	 */
	public RangeQuery() {
		mRequestingHost = PGridP2P.sharedInstance().getLocalHost();
	}
	
	/**
	 * @param returns the original x lower bound
	 */
	public Long getOrigxMin() {
		return morigxMin;
	}

	/**
	 * @param sets the original x upper bound
	 */
	public void setOrigxMin(Long origxMin) {
		morigxMin = origxMin;
	}
	
	/**
	 * @param returns the original x upper bound
	 */
	public Long getOrigxMax() {
		return morigxMax;
	}

	/**
	 * @param sets the original x lower bound
	 */
	public void setOrigxMax(Long origxMax) {
		morigxMax = origxMax;
	}
	
	/**
	 * @param returns the original y lower bound
	 */
	public Long getOrigyMin() {
		return morigyMin;
	}
	
	/**
	 * @param sets the original y lower bound
	 */
	public void setOrigyMin(Long origyMin) {
		morigyMin = origyMin;
	}

	/**
	 * @param returns the original y upper bound
	 */
	public Long getOrigyMax() {
		return morigyMax;
	}

	/**
	 * @param sets the original y upper bound
	 */
	public void setOrigyMax(Long origyMax) {
		morigyMax = origyMax;
	}

	/**
	 * @return Returns the key range.
	 */
	public KeyRange getKeyRange() {
		return mBoundsKeys;
	}

	/**
	 * @param kr The HigherBoundKey to set.
	 */
	public void setKeyRange(KeyRange kr) {
		mBoundsKeys = kr;
	}

	/**
	 * @return Returns the mAlgorithm.
	 */
	public String getAlgorithm() {
		return mAlgorithm;
	}

	/**
	 * @param algorithm The mAlgorithm to set.
	 */
	public void setAlgorithm(String algorithm) {
		mAlgorithm = algorithm;
	}

	/**
	 * @return Returns the SecondQueryString.
	 */
	public String getLowerBound() {
		return mFirstQueryString;
	}

	/**
	 * set the lower bound query
	 */
	public void setLowerBound(String bound) {
		mFirstQueryString = bound;
	}


	/**
	 * @return Returns the SecondQueryString.
	 */
	public String getHigherBound() {
		return mSecondQueryString;
	}

	/**
	 * set the higher bound query
	 */
	public void setHigherBound(String bound) {
		mSecondQueryString = bound;
	}

	public String getPrefix() {
		return mPrefix;
	}

	public void setPrefix(String prefix) {
		mPrefix = prefix;
	}
	
	public Long getHits() {
		return mHits;
	}
	
	public void setHits (Long hits) {
		mHits = hits;
	}
	/**
	 * Construct a range query message
	 *
	 * @param guid        the unique range query number
	 * @param type        the type of the query
	 * @param minQuery    least bound
	 * @param maxQuery    max bound
	 * @param kr          bounds of the range query
	 * @param minSpeed    the connection speed
	 * @param hops        number of hops taken by this query
	 * @param initialHost host that should recieve the result set
	 */
	public RangeQuery(GUID guid, Type type, String algorithm, String minQuery, String maxQuery, KeyRange kr, String prefix, int minSpeed, int hops, PGridHost initialHost) {
		super(initialHost, guid, type, minSpeed, hops);
		mGUID = guid;
		mType = type;
		mFirstQueryString = minQuery;
		mSecondQueryString = maxQuery;
		mBoundsKeys = kr;
		mIndex = 0;
		mAlgorithm = algorithm;
		mPrefix = prefix;
	}
	
	/**
	 * Construct a range query message
	 *
	 * @param guid        the unique range query number
	 * @param type        the type of the query
	 * @param minQuery    least bound
	 * @param maxQuery    max bound
	 * @param origxMin the original query x minimum
	 * @param origxMax the original query x maximum
	 * @param origyMin the original query y minimum
	 * @param origyMax the original query y maximum
	 * @param kr          bounds of the range query
	 * @param minSpeed    the connection speed
	 * @param hops        number of hops taken by this query
	 * @param initialHost host that should recieve the result set
	 */
	public RangeQuery(GUID guid, Type type, String algorithm, String minQuery, String maxQuery, 
			 Long origxMin, Long origxMax, Long origyMin, Long origyMax, KeyRange kr, String prefix, int minSpeed, int hops, PGridHost initialHost) {
		super(initialHost, guid, type, minSpeed, hops);
		mGUID = guid;
		mType = type;
		mFirstQueryString = minQuery;
		mSecondQueryString = maxQuery;
		morigxMin = origxMin;
		morigxMax = origxMax;
		morigyMin = origyMin;
		morigyMax = origyMax;
		mBoundsKeys = kr;
		mIndex = 0;
		mAlgorithm = algorithm;
		mPrefix = prefix;
	}
	
	/**
	 * Construct a range query message
	 *
	 * @param guid        the unique range query number
	 * @param type        the type of the query
	 * @param minQuery    least bound
	 * @param maxQuery    max bound
	 * @param origxMin the original query x minimum
	 * @param origxMax the original query x maximum
	 * @param origyMin the original query y minimum
	 * @param origyMax the original query y maximum
	 * @param kr          bounds of the range query
	 * @param minSpeed    the connection speed
	 * @param hops        number of hops taken by this query
	 * @param initialHost host that should recieve the result set
	 */
	public RangeQuery(GUID guid, Type type, String algorithm, String minQuery, String maxQuery, 
			 Long origxMin, Long origxMax, Long origyMin, Long origyMax, KeyRange kr, String prefix, int minSpeed, int hops, PGridHost initialHost, Long hits) {
		super(initialHost, guid, type, minSpeed, hops);
		mGUID = guid;
		mType = type;
		mFirstQueryString = minQuery;
		mSecondQueryString = maxQuery;
		morigxMin = origxMin;
		morigxMax = origxMax;
		morigyMin = origyMin;
		morigyMax = origyMax;
		mBoundsKeys = kr;
		mIndex = 0;
		mAlgorithm = algorithm;
		mPrefix = prefix;
		mHits = hits;
	}

	/**
	 * Construct a range query message
	 *
	 * @param guid        the unique range query number
	 * @param type        the type of the query
	 * @param algorithm   to use
	 * @param minQuery    least bound
	 * @param maxQuery    max bound
	 * @param index       represent the resolved portion of the query
	 * @param minSpeed    the connection speed
	 * @param initialHost host that should recieve the result set
	 */
	public RangeQuery(GUID guid, p2p.index.Type type, int hops, String algorithm, String minQuery, String maxQuery, KeyRange kr, int index, String prefix, int minSpeed, PGridHost initialHost) {
		this(guid, type, algorithm, minQuery, maxQuery, kr, prefix, minSpeed, hops, initialHost);
		mIndex = index;
	}
	
	/**
	 * Construct a range query message
	 *
	 * @param guid        the unique range query number
	 * @param type        the type of the query
	 * @param algorithm   to use
	 * @param minQuery    least bound
	 * @param maxQuery    max bound
	 * @param origxMin the original query x minimum
	 * @param origxMax the original query x maximum
	 * @param origyMin the original query y minimum
	 * @param origyMax the original query y maximum
	 * @param index       represent the resolved portion of the query
	 * @param minSpeed    the connection speed
	 * @param initialHost host that should recieve the result set
	 */
	public RangeQuery(GUID guid, p2p.index.Type type, int hops, String algorithm, String minQuery, String maxQuery,
			 Long origxMin, Long origxMax, Long origyMin, Long origyMax,KeyRange kr, int index, String prefix, int minSpeed, PGridHost initialHost) {
		this(guid, type, algorithm, minQuery, maxQuery,origxMin, origxMax,origyMin,origyMax, kr, prefix, minSpeed, hops, initialHost);
		mIndex = index;
	}
	
	
	
	/**
	 * Construct a range query message
	 *
	 * @param guid        the unique range query number
	 * @param type        the type of the query
	 * @param algorithm   to use
	 * @param minQuery    least bound
	 * @param maxQuery    max bound
	 * @param origxMin the original query x minimum
	 * @param origxMax the original query x maximum
	 * @param origyMin the original query y minimum
	 * @param origyMax the original query y maximum
	 * @param index       represent the resolved portion of the query
	 * @param minSpeed    the connection speed
	 * @param initialHost host that should recieve the result set
	 */
	public RangeQuery(GUID guid, p2p.index.Type type, int hops, String algorithm, String minQuery, String maxQuery,
			 Long origxMin, Long origxMax, Long origyMin, Long origyMax,KeyRange kr, int index, String prefix, int minSpeed, PGridHost initialHost, Long hits) {
		this(guid, type, algorithm, minQuery, maxQuery,origxMin, origxMax,origyMin,origyMax, kr, prefix, minSpeed, hops, initialHost, hits);
		mIndex = index;
	}
	

	/**
	 * Creates a new Query for a given search string. It is assumed that this peer
	 * is the initiator of the query and its address will be bound with the query as
	 * the destination for the result set. This constructor should not be used to route
	 * a query.
	 *
	 * @param type     the type of Query.
	 * @param firstQuery  lower bound string
	 * @param secondQuery higher bound string
	 * @param minSpeed the mininum connection speed for responding host.
	 */
	public RangeQuery(Type type, KeyRange key, String firstQuery, String secondQuery, int minSpeed) {
		mGUID = new pgrid.GUID();
		mType = type;
		mFirstQueryString = firstQuery;
		mSecondQueryString = secondQuery;
		mBoundsKeys = key;
		mMinSpeed = minSpeed;
		mRequestingHost = PGridP2P.sharedInstance().getLocalHost();
		mIndex = 0;
		mAlgorithm = PGridP2P.sharedInstance().propertyString(Properties.RANGE_QUERY_ALGORITHM);
		mHops = 1;
		mPrefix = "";
	}

	/**
	 * Creates a new Query for a given search string. It is assumed that this peer
	 * is the initiator of the query and its address will be bound with the query as
	 * the destination for the result set. This constructor should not be used to route
	 * a query.
	 *
	 * @param guid the guid
	 * @param type     the type of Query.
	 * @param firstQuery  lower bound string
	 * @param secondQuery higher bound string
	 * @param minSpeed the mininum connection speed for responding host.
	 */
	public RangeQuery(GUID guid, Type type, KeyRange key, String firstQuery, String secondQuery, int minSpeed) {
		mGUID = guid;
		mType = type;
		mFirstQueryString = firstQuery;
		mSecondQueryString = secondQuery;
		mBoundsKeys = key;
		mMinSpeed = minSpeed;
		mRequestingHost = PGridP2P.sharedInstance().getLocalHost();
		mIndex = 0;
		mHops = 1;
		mAlgorithm = PGridP2P.sharedInstance().propertyString(Properties.RANGE_QUERY_ALGORITHM);
	}

		/**
	 * Creates a new Query for a given search string. It is assumed that this peer
	 * is the initiator of the query and its address will be bound with the query as
	 * the destination for the result set. This constructor should not be used to route
	 * a query.
	 *
	 * @param host  	Initiator host
	 * @param type     the type of Query.
	 * @param firstQuery  lower bound string
	 * @param secondQuery higher bound string
	 */
	public RangeQuery(PGridHost host, Type type, KeyRange key, String firstQuery, String secondQuery) {
		mGUID = PGridP2PFactory.sharedInstance().generateGUID();
		mType = type;
		mFirstQueryString = firstQuery;
		mSecondQueryString = secondQuery;
		mBoundsKeys = key;
		mMinSpeed = 0;
		mRequestingHost = host;
		mIndex = 0;
		mHops = 1;
		mAlgorithm = PGridP2P.sharedInstance().propertyString(Properties.RANGE_QUERY_ALGORITHM);
	}
	
	/**
	 * Creates a new Query for a given search string. It is assumed that this peer
	 * is the initiator of the query and its address will be bound with the query as
	 * the destination for the result set. This constructor should not be used to route
	 * a query.
	 *
	 * @param host  	Initiator host
	 * @param type     the type of Query.
	 * @param firstQuery  lower bound string
	 * @param secondQuery higher bound string
	 * @param origxMin the original x lowerbound 
	 * @param orig
	 */
	public RangeQuery(PGridHost host, Type type, KeyRange key, String firstQuery, String secondQuery
			,  Long origxMin, Long origxMax, Long origyMin, Long origyMax) {
		mGUID = PGridP2PFactory.sharedInstance().generateGUID();
		mType = type;
		mFirstQueryString = firstQuery;
		mSecondQueryString = secondQuery;
		mBoundsKeys = key;
		mMinSpeed = 0;
		mRequestingHost = host;
		mIndex = 0;
		mHops = 1;
		mAlgorithm = PGridP2P.sharedInstance().propertyString(Properties.RANGE_QUERY_ALGORITHM);
		morigxMin = origxMin;
		morigxMax = origxMax;
		morigyMin = origyMin;
		morigyMax = origyMax;
	
	}


	/**
	 * @see pgrid.QueryInterface#getRepresentation()
	 */
	public String getRepresentation() {
		return mFirstQueryString + " - " + mSecondQueryString;
	}

	/**
	 * Return true if the host is responsable for this query
	 * @param host
	 * @return true if responsible
	 */
	public boolean isHostResponsible(PGridHost host) {
		if (host.getPath().equals("")) return true;
		return getKeyRange().withinRange(new PGridKey(host.getPath()));
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
	public Long setHits() {
		// TODO Auto-generated method stub
		return null;
	}


}
