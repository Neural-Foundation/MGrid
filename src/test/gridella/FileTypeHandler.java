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

import p2p.basic.*;
import p2p.index.events.SearchListener;
import p2p.index.Query;
import p2p.index.IndexEntry;
import p2p.index.Type;
import pgrid.PGridHost;
import pgrid.interfaces.index.PGridIndex;
import pgrid.interfaces.index.DefaultTypeHandler;

import java.util.Vector;

/**
 * This class represents the file manager for all shared and downloaded
 * files.
 * This class implements the <code>Singleton</code> pattern as defined by
 * Gamma et.al. As there could only exist one instance of this class, other
 * clients must use the <code>sharedInstance</code> function to use this class.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class FileTypeHandler extends DefaultTypeHandler {

	/**
	 * Constructs the handler for the responsible type.
	 *
	 * @param type the responsible type.
	 */
	public FileTypeHandler(Type type) {
		super(type);
	}

	/**
	 * Create a IndexEntry instance compatible with the Storage implementation.
	 *
	 * @return a IndexEntry instance
	 */
	public IndexEntry createIndexItem(PGridHost host, int qoS, String path, String name, int size, String infos, String desc) {
		GUID guid = mP2PFactory.generateGUID();
		Key key = generateKey(name);

		return new XMLFileIndexEntry(guid, getType(), key, host, qoS, path, name, size, infos, desc);
	}

	/**
	 * Create a IndexEntry instance compatible with the Storage implementation.
	 *
	 * @param guid the guid of the data.
	 * @param key the key generated of the data.
	 * @param host the host.
	 * @param data the encapsulated data.
	 * @return a IndexEntry instance
	 */
	public IndexEntry createIndexEntry(GUID guid, Key key, Peer host, Object data) {
		if (data == null) return new XMLFileIndexEntry();
		return new XMLFileIndexEntry(guid, getType(), key, host, data);
	}

	/**
	 * Searches for given query.
	 *
	 * @param query the query.
	 * @param listener the search listener.
	 */
	public void handleLocalSearch(Query query, SearchListener listener) {
		Vector result;
		String lower = query.getLowerBound().toUpperCase();
		String higher = query.getHigherBound().toUpperCase();
		boolean equal = lower.equals(higher);

		if (equal) {
			result = (Vector) PGridIndex.sharedInstance().getLocalIndexEntries(query.getLowerBound());
		} else {
			result = (Vector) PGridIndex.sharedInstance().getLocalIndexEntries(query.getLowerBound(),query.getHigherBound());
		}

		if (result.size() > 0)
			listener.newSearchResult(query.getGUID(), result);
		else
			listener.noResultsFound(query.getGUID());
		//listener.searchFinished(query.getGUID());
	}

}
