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
package pgrid.core.index;

import pgrid.*;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.core.index.IndexManager;
import pgrid.core.index.DBIndexTable;
import pgrid.core.DBManager;

import java.util.Collection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * <p>Title: DBView</p>
 * <p/>
 * <p>Description: A utility class to generate a "View" of the tables, i.e.
 * generate SQL queries and subqueries</p>
 *
 * @author Mark Kornfilt
 * @version 1.0
 */
public class DBView {

	/**
	 * The DataBase manager
	 */
	private DBManager mDBManager = DBManager.sharedInstance();

	/**
	 * The Data Item manager.
	 */
	private IndexManager mIndexManager = null;

	/**
	 * The Data Item table.
	 */
	private DBIndexTable mTable = null;

	/**
	 * The Query defining this View
	 */
	private String mSQLStatement;

	/**
	 * Constructs a DBView based on an SQL statement.
	 *
	 * @param sqlStatement the SQL statement.
	 */
	private DBView(String sqlStatement) {
		mIndexManager = PGridP2P.sharedInstance().getIndexManager();
		mSQLStatement = sqlStatement;
	}

	/**
	 * Constructs a DBView based on an SQL statement.
	 *
	 * @param sqlStatement the SQL statement.
	 */
	private DBView(DBIndexTable table, String sqlStatement) {
		mIndexManager = PGridP2P.sharedInstance().getIndexManager();
		mSQLStatement = sqlStatement;
		mTable = table;
	}

//	/**
//	 * Returns the list of data items.
//	 *
//	 * @return the list of data items.
//	 */
//	public Collection getIndexEntries() {
//		ResultSet rs = mDBManager.execResultSetSQL("select DISTINCT(di.GUID) as GUID, di.TYPE_NAME as TYPE_NAME, di.KEY as KEY, h.GUID as hGUID, h.ADDRESS as ADDR, h.PORT as PORT, di.DATA as DATA " +
//				"from " + DBManager.INDEX_ITEMS_TABLE + " di, " + DBManager.HOSTS_TABLE + " h " +
//				"where di.GUID in (select GUID from " + getIndexEntriesAsSQL() + ") and " +
//				"and h.HOST_ID = di.HOST_ID");
//
//
//		return mIndexManager.getIndexTable().getIndexEntries(rs);
//	}


	/**
	 * Returns the SQL query of this view
	 *
	 * @return The SQL query
	 */
	public String getIndexEntriesAsXML() {
		
		ResultSet rs = null;
		Connection rsCon = null;
		try {
			rsCon = DBManager.sharedInstance().getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (rsCon == null) {
			Constants.LOGGER.warning("No connection available !");
		}
		try {	
			Statement rsSt = rsCon.createStatement();
			rs = rsSt.executeQuery(DBManager.sharedInstance().preprocessSQL(mSQLStatement));
			rsSt.clearBatch();
		} catch (SQLException ex) {
			if (PGridP2P.sharedInstance().isInDebugMode())
				ex.printStackTrace();
			Constants.LOGGER.warning("problem " + ex + "\nexecuting query: " + mSQLStatement +"\nerror code: " + ex.getErrorCode());
		}
		
		
		if (rs == null) {
			Constants.LOGGER.warning("Couldn't get data items. DB connection probably closed.");
			return "";
		}
		String res = mTable.getIndexEntriesAsXML(rs);
		try {
			rsCon.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return res;
	}

	/**
	 * Returns the view
	 *
	 * @return The SQL query
	 */
	public String getIndexEntriesViewAsSQL() {
		return getIndexEntriesAsSQL();
	}
	
	/**
	 * Returns the SQL query of this view
	 *
	 * @return The SQL query
	 */
	public String getIndexEntriesAsSQL() {
		return mSQLStatement;
	}

	/**
	 * Counts the number of data items in this view.
	 *
	 * @return the number of items.
	 */
	public int count() {
		return mDBManager.count("GUID", "("+mSQLStatement+")");
	}

	/**
	 * Returns a selection of a DBView according to the given key criteria.
	 *
	 * @param table the source table.
	 * @param criteria the selection criteria.
	 * @return the new DBView.
	 */
	public static DBView selection(DBIndexTable table, String keyCriteria) {
		return new DBView(table.asView(keyCriteria));
	}

//	/**
//	 * Returns a selection of a DBView according to the given key criteria.
//	 *
//	 * @param table the source table.
//	 * @param criteria the selection criteria.
//	 * @return the new DBView.
//	 */
//	private static DBView selection(DBView table, String criteria) {
//		return new DBView("(select INDEX_TABLE_ID, GUID, TYPE_NAME, KEY, HOST_ID, DATA from (" + table.getIndexEntriesAsSQL() + ") where KEY like '" + criteria + "%')");
//	}


	/**
	 * Returns a new DBView representing the union from table1 and table2.
	 *
	 * @param table1 the first table.
	 * @param table2 the second table.
	 * @return the new DBView.
	 */
	public static DBView union(DBIndexTable table1, DBIndexTable table2) {
		return new DBView("("+table1.asView() + " union " + table2.asView() + ")");
//		return new DBView("(select INDEX_TABLE_ID, GUID, TYPE_NAME, KEY, HOST_ID, DATA from " + table1.asView() + " union " + table2.asView() + ")");
	}

	/**
	 * Returns a new DBView representing the union from table1 and table2.
	 *
	 * @param table1 the first table.
	 * @param table2 the second table.
	 * @return the new DBView.
	 */
	public static DBView union(DBIndexTable table1, DBView table2) {
		return new DBView("("+table1.asView() + " union " + table2.toString() + ")");
//		return new DBView("(select INDEX_TABLE_ID, GUID, TYPE_NAME, KEY, HOST_ID, DATA from " + table1.asView() + " union " + table2.getIndexEntriesAsSQL() + ")");
	}

	/**
	 * Returns a new DBView representing the union from table1 and table2.
	 *
	 * @param table1 the first table.
	 * @param table2 the second table.
	 * @return the new DBView.
	 */
	public static DBView union(DBView table1, DBIndexTable table2) {
		return new DBView("("+table1.toString() + " union " + table2.asView() + ")");
//		return new DBView("(select INDEX_TABLE_ID, GUID, TYPE_NAME, KEY, HOST_ID, DATA from " + table1.getIndexEntriesAsSQL() + " union " + table2.asView() + ")");
	}

	/**
	 * Returns a new DBView representing the union from table1 and table2.
	 *
	 * @param table1 the first table.
	 * @param table2 the second table.
	 * @return the new DBView.
	 */
	public static DBView union(DBView table1, DBView table2) {
		return new DBView("("+table1.toString() + " union " + table2.toString() + ")");
//		return new DBView("(select INDEX_TABLE_ID, GUID, TYPE_NAME, KEY, HOST_ID, DATA from " + table1.getIndexEntriesAsSQL() + " union " + table2.getIndexEntriesAsSQL() + ")");
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return mSQLStatement;
	}

	public static DBView limit(DBIndexTable table, int limit, int offset) {
		String limitStr = "";
		if ((limit+offset)!=0)
			limitStr = " LIMIT "+limit+" OFFSET "+offset;

		return new DBView(table, new StringBuffer().append(table.asView()).append(limitStr).toString());

	}
	

}
