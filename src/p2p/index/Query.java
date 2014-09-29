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

import p2p.basic.GUID;
import p2p.basic.KeyRange;

import java.util.Collection;

/**
 * Defines the operations that queries support. A query includes
 * a query string pair and a type of relevant items. It is used to
 * specify a search request for the storage layer.
 *
 * A simple query, where we are looking for a precise data item, is considered
 * as a special kind of range query where both bounds are equals.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public interface Query {

	/**
	 * Get the query's guid. Used to identify the query across
	 * the network.
	 *
	 * @return the global unique identifier
	 */
	public GUID getGUID();

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
	public Collection<GUID> getQueryReferences();

	/**
	 * Get the key of the query used to route it to the responsible peer.
	 *
	 * @return the query's key.
	 */
	public KeyRange getKeyRange();

	/**
	 * Get the query's status code, as defined by the storage layer.
	 *
	 * @return the status code
	 */
	public int getStatus();

	/**
	 * Get the type of items the query considers, e.g. text/file, etc.
	 *
	 * @return the query's target type
	 */
	public Type getType();

	/**
	 * Get the toRanges lower bound of the range query string that defines the query, e.g. a keyword
	 * such as 'Madonna' or a semantic query such as
	 * '&lt;predicate&gt;Actor&lt;/predicate&gt;&lt;object&gt;Madonna&lt;/object&gt;'.
	 *
	 * @return the query string
	 */
	public String getLowerBound();

	/**
	 * Get the toRanges higher query string that defines the query, e.g. a keyword
	 * such as 'Madonna' or a semantic query such as
	 * '&lt;predicate&gt;Actor&lt;/predicate&gt;&lt;object&gt;Madonna&lt;/object&gt;'.
	 *
	 * @return the query string
	 */
	public String getHigherBound();
	
	public Long getOrigxMin();
	
	public Long getOrigxMax();
	
	public Long getOrigyMin();
	
	public Long getOrigyMax();
	/**
	 * Get the original lower query string that defines the query, e.g. a keyword
	 * such as 'Madonna' or a semantic query such as
	 * '&lt;predicate&gt;Actor&lt;/predicate&gt;&lt;object&gt;Madonna&lt;/object&gt;'.
	 *
	 * @return the query string
	 */
	
	public String getOriginalMin();
	/**
	 * Get the original query string that defines the query, e.g. a keyword
	 * such as 'Madonna' or a semantic query such as
	 * '&lt;predicate&gt;Actor&lt;/predicate&gt;&lt;object&gt;Madonna&lt;/object&gt;'.
	 *
	 * @return the query string
	 */
	public String getOriginalMax();


	public Long getHits();
	
	public Long setHits();
	

}
