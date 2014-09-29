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

import p2p.basic.P2P;
import p2p.basic.GUID;
import p2p.basic.events.NoRouteToKeyException;
import p2p.index.Query;
import p2p.index.Index;
import p2p.index.Type;
import p2p.index.events.NoSuchTypeException;
import p2p.index.events.SearchListener;
import p2p.index.events.IndexListener;
import pgrid.core.index.IndexManager;
import pgrid.core.search.SearchManager;
import pgrid.interfaces.basic.PGridP2P;

import java.io.IOException;
import java.util.*;

/**
 * Defines the operations that the storage layer supports.
 * It includes standard data (search, insert, delete, update) operations
 * and registration of callbacks associated with them.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class PGridIndex implements Index {

	/**
	 * The reference to the only instance of this class (Singleton
	 * pattern). This differs from the C++ standard implementation by Gamma
	 * et.al. since Java ensures the order of static initialization at runtime.
	 *
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	private static final PGridIndex SHARED_INSTANCE = new PGridIndex();

	/**
	 * The search manager.
	 */
	private SearchManager mSearchManager = null;

	/**
	 * The data item manager.
	 */
	private IndexManager mIndexManager = null;

	/**
	 * The P2P facility.
	 */
	private PGridP2P mPGridP2P = null;

	/**
	 * The Storage Factory.
	 */
	private PGridIndexFactory mStorageFactory = null;

	/**
	 * The constructor must be protected to ensure that only subclasses can
	 * call it and that only one instance can ever get created. A client that
	 * tries to instantiate PGridIndexManager directly will get an error at compile-time.
	 */
	protected PGridIndex() {
		mStorageFactory = PGridIndexFactory.sharedInstance();
	}

	/**
	 * This creates the only instance of this class. This differs from the C++ standard implementation by Gamma et.al.
	 * since Java ensures the order of static initialization at runtime.
	 *
	 * @return the shared instance of this class.
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	public static PGridIndex sharedInstance() {
		return SHARED_INSTANCE;
	}

	/**
	 * Initializes the P-Grid facility.
	 *
	 * @param p2p the P2P facility.
	 */
	synchronized public void init(P2P p2p) {
		mPGridP2P = (PGridP2P)p2p;
		mIndexManager = mPGridP2P.getIndexManager();
		mSearchManager = mPGridP2P.getSearchManager();

		// initiat the storage manager
		mIndexManager.init(mPGridP2P.propertyString(pgrid.Properties.INDEX_TABLE), mPGridP2P.getLocalHost());
	}

	/**
	 * Register a listener of events related to data items.
	 * Such listeners are notified when operations on items
	 * on the the current node are requested.
	 *
	 * @param listener the listener to register
	 * @param type     the type of data items that the listener is interested in
	 */
	public void addIndexListener(IndexListener listener, Type type) {
		mIndexManager.addIndexListener(listener, type);
	}

	/**
	 * Removed a registered listener.
	 *
	 * @param listener the listener to register
	 * @param type	 the type of data items that the listener is interested in
	 */
	public void removeIndexListener(IndexListener listener, Type type) {
		mIndexManager.removeIndexListener(listener,(pgrid.Type) type);
	}

	/**
	 * Remove the data items with given Ds.
	 *
	 * @param items the data items to be removed
	 */
	public void delete(Collection items) {
		if (items == null)
			throw new NullPointerException();

		mIndexManager.deleteIndexEntries(items, mPGridP2P.hasJoined());
	}

	/**
	 * Store the data items into the network
	 *
	 * @param items the DataItems to insert
	 */
	public void insert(Collection items) {
		if (items == null)
			throw new NullPointerException();

		mIndexManager.insertIndexEntries(items, mPGridP2P.hasJoined());
	}

	/**
	 * Get collection of the local data items.
	 *
	 * @return the local data items.
	 */
	public Collection getLocalIndexEntries() {
		return mIndexManager.getIndexTable().getIndexEntries();
	}

	/**
	 * Get collection of the local data items prefixed by the
	 * given prefix.
	 *
	 * @param prefix prefixing the data item data field
	 * @return the local data items.
	 */
	public Collection getLocalIndexEntries(String prefix) {
		return mIndexManager.getIndexTable().getIndexEntriesPrefixed(prefix);
	}

	/**
	 * Get collection of the local data items prefixed by the
	 * given prefix.
	 *
	 * @param lowerPrefix prefixing the data item data field
	 * @param higherPrefix prefixing the data item data field
	 * @return the local data items.
	 */
	public Collection getLocalIndexEntries(String lowerPrefix, String higherPrefix) {
		return mIndexManager.getIndexTable().getIndexEntriesPrefixed(lowerPrefix, higherPrefix);
	}

	/**
	 * Get collection of the local data items prefixed by the
	 * given prefix.
	 *
	 * @param lowerPrefix prefixing the data item data field
	 * @param higherPrefix prefixing the data item data field
	 * @param origxMin the original query x minimum
	 * @param origxMax the original query x maximum
	 * @param origyMin the original query y minimum
	 * @param origyMax the original query y maximum
	 * @return the local data items.
	 */
	public Collection getLocalIndexEntries(String lowerPrefix, String higherPrefix,  Long origxMin, Long origxMax, Long origyMin, Long origyMax) {
		return mIndexManager.getIndexTable().getIndexEntriesPrefixed(lowerPrefix, higherPrefix, origxMin,  origxMax, origyMin, origyMax);
	}

	/**
	 * Get collection of the local data items.
	 *
	 * @return the local data items.
	 */
	public Collection getOwnedIndexEntries() {
		return mIndexManager.getIndexTable().getOwnedIndexEntries();
	}

	/**
	 * Search the network for matching items. Implemented as
	 * an asynchronous operation, because search might take
	 * some time. Callback is notified for each new result.
	 *
	 * @param query    the query used to specify the search
	 * @param listener an object to notify when results arrive
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 * @throws NoRouteToKeyException if the query cannot be routed to a responsible peer.
	 */
	public void search(Query query, SearchListener listener) throws NoSuchTypeException, NoRouteToKeyException {
		if ((query == null) || (listener == null))
			throw new NullPointerException();
		// forward the request to the search manager
		mSearchManager.search(query, listener);
	}

	/**
	 * Search the network for matching items. Implemented as
	 * an asynchronous operation, because search might take
	 * some time. Callback is notified for each new result. <br>
	 *
	 * This method register the given listener for the give GUID. Use this method if you are only interested in binding
	 * a listener to a query reference GUID and not the query GUID itself.
	 *
	 * @param query    the query used to specify the search
	 * @param listener an object to notify when results arrive
	 * @param guid	the guid used for registerating the listener. If null, no listener is registered
	 * @throws p2p.index.events.NoSuchTypeException
	 *          if the provided Type is unknown.
	 * @throws p2p.basic.events.NoRouteToKeyException
	 *          if the query cannot be routed to a responsible peer.
	 */
	public void search(p2p.index.Query query, SearchListener listener, GUID guid) throws NoSuchTypeException, NoRouteToKeyException {
		// forward the request to the search manager
		mSearchManager.search(query, listener, guid);
	}

	/**
	 * Updates the dataitems into the network, if the items with such ID's already
	 * exist, they will be rewritten
	 *
	 * @param items the collection of data items
	 */
	public void update(Collection items) {
		if (items == null)
			throw new NullPointerException();

		mIndexManager.updateIndexEntries(items);
	}

	/**
	 * Shutdowns the storage service.
	 */
	public void shutdown() {
		mIndexManager.shutdown();
	}

}
