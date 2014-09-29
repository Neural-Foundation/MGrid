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
package pgrid.interfaces.index;

import p2p.basic.*;
import p2p.index.events.SearchListener;
import p2p.index.*;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.interfaces.basic.PGridP2PFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import com.google.common.primitives.Ints;

import mgrid.core.HBaseManager;

/**
 * This class represents a basic type handler. It can be used as is or extented
 * to provide different search type, type specific hash function or use a
 * data item implementation.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public class DefaultTypeHandler implements TypeHandler {
	/**
	 * The P2P facility.
	 */
	protected P2P mP2P = PGridP2P.sharedInstance();

	/**
	 * The P2P Factory.
	 */
	protected P2PFactory mP2PFactory = null;

	/**
	 * The Storage facility.
	 */
	protected Index mIndex = PGridIndex.sharedInstance();
	
//	protected HBaseManager mHBManager = null ;

	/**
	 * The data type this manager is responsible for.
	 */
	protected Type mType = null;

	/**
	 * Constructs the handler for the responsible type.
	 *
	 * @param type the responsible type.
	 */
	public DefaultTypeHandler(Type type) {
		mType = type;
		mP2PFactory = PGridP2PFactory.sharedInstance();
	}

	/**
	 * Create a IndexEntry instance compatible with the Storage implementation.
	 *
	 * @return a IndexEntry instance
	 */
	public IndexEntry createIndexEntry() {
		return createIndexEntry(null,null,null,null);
	}

	/**
	 * Create a IndexEntry instance compatible with the Storage implementation.
	 *
	 * @param data the encapsulated data
	 * @return a IndexEntry instance
	 */
	public IndexEntry createIndexEntry(Object data) {
		GUID guid = mP2PFactory.generateGUID();
		//Key key = generateKey(data.toString());
		Key key = generateKey(data);
		Peer peer = mP2P.getLocalPeer();

		return createIndexEntry(guid, key, peer, data);
	}

	/**
	 * Create a IndexEntry instance compatible with the Storage implementation.
	 *
	 * Overwrite this method if you want to use a different dataitem class.
	 *
	 * @param guid the guid of the data.
	 * @param key the key generated of the data.
	 * @param host the host.
	 * @param data the encapsulated data.
	 * @return a IndexEntry instance
	 */
	public IndexEntry createIndexEntry(GUID guid, Key key, Peer host, Object data) {
		if (data == null) {
			return new XMLSimpleIndexEntry();
		}
		return new XMLSimpleIndexEntry(guid, mType, key, host, data);
	}

	/**
	 * Searches for given query.
	 *
	 * @param query the query.
	 * @param listener the search listener.
	 */
	public void handleLocalSearch(Query query, SearchListener listener){
		Collection result;
		int resultSize=0;
		String lower = query.getLowerBound().toUpperCase();
		String higher = query.getHigherBound().toUpperCase();
		boolean equal = lower.equals(higher);
		if (equal) {
		
			result = PGridIndex.sharedInstance().getLocalIndexEntries(query.getLowerBound());
			
		} else {
			result = PGridIndex.sharedInstance().getLocalIndexEntries(query.getLowerBound(),query.getHigherBound(),
					query.getOrigxMin(), query.getOrigxMax(), query.getOrigyMin(), query.getOrigyMax());
			for (Iterator it = result.iterator(); it.hasNext();) {
				IndexEntry entry = (IndexEntry) it.next();
				resultSize = Integer.parseInt(entry.getKey().toString());
			}
		}

		if (resultSize > 0) {
			listener.newSearchResult(query.getGUID(), resultSize);
		}	else
			listener.noResultsFound(query.getGUID());
	}

	/**
	 * Construct the string out of the lowerbound that will be use to query
	 * the network. <br/>
	 * For exemple, if you have a lowerbound equals to "Key=Value" you could return
	 * "ValueKey" as the effective search string. The lower bound will be inlcuded in
	 * the query, but the lower bound used for the searching will be the return value
	 * of this method.
	 * <br/>
	 * <br/>
	 * If you want to search with the lower bound, either return null or the lower bound
	 *
	 * @param query
	 * @return a string use to perform the search
	 */
	public String submitSearchLowerBoundValue(Query query) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Construct the string out of the higherbound that will be use to query
	 * the network. <br/>
	 * For exemple, if you have a higherbound equals to "Key=Value" you could return
	 * "ValueKey" as the effective search string. The higher bound will be inlcuded in
	 * the query, but the higher bound used for the searching will be the return value
	 * of this method.
	 * <br/>
	 * <br/>
	 * If you want to search with the higher bound, either return null or the lower bound
	 *
	 * @param query
	 * @return a string use to perform the search
	 */
	public String submitSearchHigherBoundValue(Query query) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Generate a Key instance compatible with the P2P implementation.
	 * Acceptable source object types depend on implementation.
	 * <p/>
	 * When working with the storage layer, only this method should
	 * be called to generate a key, not the one found in the basic interface.
	 *
	 * Overwrite this method if you want to implement a type specific hash function.
	 *
	 * @param obj the source object from which to generate the key
	 * @return the generated Key implementation
	 */
	public Key generateKey(Object obj) {
		return mP2PFactory.generateKey(obj);
	}

	/**
	 * Generate a KeyRange instance compatible with the P2P implementation.
	 * Acceptable source object types depend on implementation.
	 * <p/>
	 * When working with the storage layer, only this method should
	 * be called to generate a key, not the one found in the basic interface.
	 *
	 * @param lowerBound the source object from which to generate the lower key
	 * @param lowerBound the source object from which to generate the higher key
	 * @return the generated Key implementation
	 */
	public KeyRange generateKeyRange(Object lowerBound, Object higherBound) {
		return mP2PFactory.generateKeyRange(lowerBound,higherBound);
	}

	/**
	 * Invoked when new data items are added to the local storage.
	 *
	 * @param items the new items.
	 */
	public void dataItemsAdded(Collection items) {
		// do nothing
	}

		/**
	 * Invoked when data items are updated to the local storage.
	 *
	 * @param items the new items.
	 */
	public void dataItemsUpdated(Collection items) {
		// do nothing
	}

	/**
	 * Invoked when  data items are deleted to the local storage.
	 *
	 * @param items the new items.
	 */
	public void dataItemsRemoved(Collection items){
		// do nothing
	}

	/**
	 * Invoked when the data table is cleared.
	 */
	public void dataTableCleared() {
		// do nothing
	}

	/**
	 * Returns the type this type handler is responsible for.
	 * @return the type
	 */
	public Type getType() {
		return mType;
	}

}
