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

package p2p.index;


import p2p.index.events.SearchListener;
import p2p.basic.Peer;
import p2p.basic.Key;
import p2p.basic.GUID;
import p2p.basic.KeyRange;

/**
 * Used to define types of data items to store.
 * No particular operations are defined; however, implementers are
 * encouraged to provide consistent implementations of
 * java.land.Object methods, including equals and hashCode,
 * so that different types can be distinguished.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public interface TypeHandler {

  /**
   * Create a IndexEntry instance compatible with the Storage implementation.
   *
   * @return a IndexEntry instance
   */
  public IndexEntry createIndexEntry();

	/**
	 * Create a IndexEntry instance compatible with the Storage implementation.
	 *
	 * @param data the encapsulated data
	 * @return a IndexEntry instance
	 */
	public IndexEntry createIndexEntry(Object data);

	/**
	 * Create a IndexEntry instance compatible with the Storage implementation.
	 *
	 * @param guid the guid of the data.
	 * @param key the key generated of the data.
	 * @param host the host.
	 * @param data the encapsulated data.
	 * @return a IndexEntry instance
	 */
	public IndexEntry createIndexEntry(GUID guid, Key key, Peer host, Object data);

	/**
	 * Searches localy for all dataitems matching the query. This method is responsible of calling the
	 * appropriate method in search listener. All methods takes a GUID as parameter and this GUID
	 * should ALWAYS be set to query.getGUID().
	 *
	 * @param query the query.
	 * @param listener the search listener.
	 * @throws IOException 
	 */
	public void handleLocalSearch(Query query, SearchListener listener);

	/**
	 * Construct the string out of the lowerbound that will be use to query
	 * the network. <br/>
	 * For exemple, if you have a lowerbound equals to "Key=Value" you could return
	 * "ValueKey" as the effective search string. The lower bound will be inlcuded in
	 * the query, but the lower bound used for the searching will be the return value
	 * of this method.
	 *  <br/>
	 *  <br/>
	 * If you want to search with the lower bound, either return null or the lower bound
	 *
	 * @param query the query being processed.
	 * @return a string use to perform the search
	 */
	public String submitSearchLowerBoundValue(Query query);

	/**
	 * Construct the string out of the higherbound that will be use to query
	 * the network. <br/>
	 * For exemple, if you have a higherbound equals to "Key=Value" you could return
	 * "ValueKey" as the effective search string. The higher bound will be inlcuded in
	 * the query, but the higher bound used for the searching will be the return value
	 * of this method.
	 *  <br/>
	 *  <br/>
	 * If you want to search with the higher bound, either return null or the lower bound 
	 *
	 * @param query the query being processed.
	 * @return a string use to perform the search
	 */
	public String submitSearchHigherBoundValue(Query query);

	/**
	 * Generate a Key instance compatible with the P2P implementation.
	 * Acceptable source object types depend on implementation.
	 *
	 * When working with the storage layer, only this method should
	 * be called to generate a key, not the one found in the basic interface.
	 *
	 * @param obj the source object from which to generate the key
	 * @return the generated Key implementation
	 */
	public abstract Key generateKey(Object obj);

	/**
	 * Generate a KeyRange instance compatible with the P2P implementation.
	 * Acceptable source object types depend on implementation.
	 *
	 * When working with the storage layer, only this method should
	 * be called to generate a key, not the one found in the basic interface.
	 *
	 * @param lowerBound the source object from which to generate the lower key
	 * @param lowerBound the source object from which to generate the higher key
	 * @return the generated Key implementation
	 */
	public abstract KeyRange generateKeyRange(Object lowerBound, Object higherBound);


}
