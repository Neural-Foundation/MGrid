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

import p2p.basic.events.NoRouteToKeyException;
import p2p.index.events.SearchListener;
import p2p.index.events.NoSuchTypeException;
import p2p.index.events.IndexListener;

import java.util.Collection;

/**
 * Defines the operations that the storage layer supports.
 * It includes standard data (search, insert, delete, update) operations
 * and registration of callbacks associated with them.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public interface Index {

	/**
	 * Register a listener of events related to data items.
	 * Such listeners are notified when operations on items
	 * on the the current node are requested.
	 *
	 * @param listener the listener to register
	 * @param type     the type of data items that the listener is interested in
	 */
	public void addIndexListener(IndexListener listener, Type type);

	/**
	 * Removed a registered listener.
	 *
	 * @param listener the listener to register
	 * @param type     the type of data items that the listener is interested in
	 */
	public void removeIndexListener(IndexListener listener, Type type);

	/**
	 * Remove the index items with given Ds.
	 *
	 * @param items index items to be removed
	 */
	public void delete(Collection items);

	/**
	 * Store the index items into the network
	 *
	 * @param items the IndexItems to insert
	 */
	public void insert(Collection items);

	/**
	 * Get collection of the local index items.
	 *
	 * @return the local index items.
	 */
	public Collection getLocalIndexEntries();

	/**
	 * Search the network for matching items. Implemented as
	 * an asynchronous operation, because search might take
	 * some time. Callback is notified for each new result.
	 *
	 * @param query    the query used to specify the search
	 * @param listener an object to notify when results arrive
	 * @throws p2p.index.events.NoSuchTypeException if the provided Type is unknown.
	 * @throws NoRouteToKeyException if the query cannot be routed to a responsible peer.
	 */
	public void search(Query query, SearchListener listener) throws NoSuchTypeException, NoRouteToKeyException;

	/**
	 * Inserts the indexitems int the network, if the items with such ID's already
	 * exist, they will be rewritten
	 *
	 * @param items the collection of data items
	 */
	public void update(Collection items);

	/**
	 * Shutdowns the storage service.
	 */
	public void shutdown();
	
}
