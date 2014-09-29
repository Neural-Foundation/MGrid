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

package p2p.index.events;

import p2p.basic.GUID;

import java.util.Collection;

/**
 * Defines the callback interface to notify of search results.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public interface SearchListener {

	/**
	 * Invoked when a new search result is available
	 *
	 * @param guid    the GUID of the original query
	 * @param results a Collection of DataItems matching the original query
	 */
	public void newSearchResult(GUID guid, Collection results);
	
	
	/**
	 * Invoked when a search resulted in no results.
	 *
	 * @param guid    the GUID of the original query
	 */
	public void noResultsFound(GUID guid);

	/**
	 * Invoked when a search failed.
	 *
	 * @param guid    the GUID of the original query
	 */
	public void searchFailed(GUID guid);

	/**
	 * Invoked when a search finished.
	 *
	 * @param guid    the GUID of the original query
	 */
	public void searchFinished(GUID guid);

	/**
	 * Invoked when a search started (reached a responsible peer).
	 *
	 * @param guid    the GUID of the original query
	 * @param message the explanation message.
	 */
	public void searchStarted(GUID guid, String message);
	
	/**
	 * Invoked when a new search result is available
	 *
	 * @param guid    the GUID of the original query
	 * @param resultsize size of Collection of DataItems matching the original query
	 */
	public void newSearchResult(GUID guid, int resultSize);

}


