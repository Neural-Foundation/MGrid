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

import java.util.Collection;

/**
 * Implement this interface, if the application above P-Grid is interested
 * in having its own DATA schema/table. This interface helps in linking the 
 * P-Grid schema and the upper-layer-applications DATA schema/table. 
 *
 * @author @author <a href="mailto:Surender Yerva <surenderreddy.yerva@epfl.ch>">Surender Yerva</a>
 * @version 1.0.0
 */
public interface IDBDataTypeHandler {
	/**
	 * Create the custom DATA_TABLE in this section. The table should contain an ID column(representing the dataItem id), 
	 * which will be the handle to the rest of the P-Grid DB. 
	 * If needed add the INDICES to the custom table for faster DB operations.
	 * Also make sure before creating the tables and indices they do not exist already.
	 */
	void init();
	
	/**
	 * Insert the dataItem object into the DATA-TABLE. Extract the values from dataItem
	 * and insert them as column values and return the corresponding ID. If data item  already exists in the DB then return the dataItemID.
	 * The synchronization aspects will be taken care by the P-Grid DBManager.
	 * 
	 * @return dataItemID
	 * @param dataItem
	 */
	int addDataItem(Object dataItem);

	/**
	 * Update the dataItem object in the DATA-TABLE, with the given dataItemID. Extract the values from dataItem
	 * and update the corresponding column values. The synchronization aspects will be taken care by 
	 * the P-Grid DBManager.
	 * @param dataItemID
	 * @param dataItem
	 */
	void updateDataItem(int dataItemID, Object dataItem);

	/**
	 * Given the dataItemID, delete the corresponding row from the DATA-TABLE.
	 * @param dataItemID
	 * 
	 */
	void removeDataItem(int dataItemID);
	
	/**
	 * Given the dataItemId.. fetch the dataItem.
	 * @param dataItemID
	 * @return
	 */
	Object getDataItem(int dataItemID);
	
	
	/**
	 * This function gives the handle to the upper layer applications to deal with its queries.
	 *
	 * Given the query(usually some filtering on the columns of DATA-TABLE), form the IndexEntries collection from the result set.
	 * 
	 * The way to form indexEntries is.. for each entry in resultset get the data_id and data_string, and using the utility function in DBIndexTable:
	 * getIndexEntry(int,string,type) form the IndexEntry.
	 * 
	 * @param query
	 * @return
	 */
	Collection<IndexEntry> getIndexEntries(Query query);
}
