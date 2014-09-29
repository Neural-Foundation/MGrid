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

import p2p.basic.Peer;
import p2p.index.IDBDataTypeHandler;
import pgrid.*;
import pgrid.Properties;
import pgrid.core.DBManager;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.interfaces.index.PGridIndexFactory;
import pgrid.interfaces.index.XMLSimpleIndexEntry;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.security.SecureRandom;

import com.google.uzaygezen.core.BitVector;

import mgrid.core.HBaseManager;
import mgrid.core.MGridUtils;
import mgrid.core.Point;

/**
 * <p>Title: DBIndexTable</p>
 * <p/>
 * <p>Description: RoutingTable subclass representing the DataBase RoutingTable</p>
 *
 * @author Mark Kornfilt, Surender Reddy Yerva(surenderreddy.yerva AT epfl.ch)
 * @version 1.0
 */
public class DBIndexTable extends IndexTable {
	protected String mTableName = DBManager.INDEX_ITEMS_TABLE;

	public String getColumnNames(){
//		return " INDEX_ITEM_ID, GUID, TYPE_NAME, KEY, HOST_ID, DATA, DATA_ID ";
		return "  GUID, ID, KEY, HOST_ID, DATA_ID ";
	}
	
	/**
	 * Read values from ini file
	 */
	private static PGridP2P mPGridP2P = PGridP2P.sharedInstance();	
	private static final String TYPE_NAME = mPGridP2P.propertyString(Properties.TYPE_NAME);	
	private static final int PORT_NUMBER = Integer.parseInt(mPGridP2P.propertyString(Properties.PORT_NUMBER));
	/**
	 * The data item manager.
	 */
	private IndexManager mIndexManager = IndexManager.getInstance();

	/**
	 * The DataBase manager
	 */
	protected DBManager mDBManager = DBManager.sharedInstance();

	/**
	 * The HBaseDataBase manager
	 */
	protected HBaseManager mHBManager = HBaseManager.sharedInstance();
	/**
	 * The Signature of the data stored in this data table.
	 */
	private Signature mSignature = null;

	/**
	 * The PGrid facility.
	 */
	private PGridP2P mPGrid = PGridP2P.sharedInstance();


	/**
	 * True iff this table is temporary and its host is a place holder
	 */
	private boolean mTemporaryTable = false;


	private StringBuilder sqlQuery;

	/**
	 * The maximum temp entries before doing an insert
	 */
	protected final int MAX_TEMP_ENTRIES = 10000;

	/**
	 * The Data Table id in the DB.
	 */
	protected int mDataTableID = -1;

	/**
	 * The Host id in the DB.
	 */
	protected int mHostID = -1;

	/**
	 * Temp buffer with entries to be added
	 */
	protected ArrayList<String> mTempEntries = new ArrayList<String>();

	/**
	 * Count cache
	 */
	protected int mCount = 0;

	/**
	 * If true, data is not removed at the end of execution
	 */
	protected boolean mSticky = false;

	/**
	 * data Signature when last mCount was generated
	 */
	protected Signature mCountSignature = null;


	protected SecureRandom mRnd = new SecureRandom();

	/**
	 * Represents the INDEX_ENTRY COLUMNS which includes columns from INDEX_ITEMS_TABLE and HOSTS_TABLE
	 */
	protected String IDX_ENTRY_COLUMNS =	" ii.GUID as dGUID, ii.KEY, ii.ID ii.HOST_ID, ii.DATA_ID, h.GUID as hGUID,h.ADDRESS as ADDR ";

	/**
	 * Represents the INDEX_ENTRY TABLE which is INNER JOIN OF INDEX_ITEMS_TABLE and HOSTS_TABLE
	 */
	protected String IDX_ENTRY_TABLE = " "+ this.getTableName()+" as ii INNER JOIN "+ DBManager.HOSTS_TABLE+" as h on h.HOST_ID = ii.HOST_ID ";


	HashMap mTypeToEntry = new HashMap();

	/**
	 * Construct a data table for an anonymous host.
	 */
	public DBIndexTable() {
		mTemporaryTable = true;
		init();
		setTableName("");
		createTableAndIndexes();
		IDX_ENTRY_TABLE = " "+ this.getTableName()+" as ii INNER JOIN "+ DBManager.HOSTS_TABLE+" as h on h.HOST_ID = ii.HOST_ID ";
	}

	protected void setTableName(String addon){
		mTableName += "_TMP_"+mDataTableID;
	}

	protected void createTableAndIndexes(){
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE IF NOT EXISTS "+getTableName()+"(").append("")
		.append("KEY varchar not null,")
	//	.append("X bigint not null,")
	//	.append("Y bigint not null,")
		.append("ID bigint not null,")
		.append("GUID varchar not null,")
	//	.append("TYPE_NAME varchar not null,")
		.append("HOST_ID integer not null,")
	//	.append("DATA varchar_ignorecase,")
		.append("DATA_ID integer not null);");
		
		mDBManager.execSQL(sql.toString());
		
	}

	/**
	 * Construct a data table for the given host.
	 *
	 * @param host the host.
	 */
;	public DBIndexTable(PGridHost host) {
		mTemporaryTable = false;
		init(host);
		if (host.isLocalHost()) mSticky = true;
		IDX_ENTRY_TABLE = " "+ this.getTableName()+" as ii INNER JOIN "+ DBManager.HOSTS_TABLE+" as h on h.HOST_ID = ii.HOST_ID ";
	}

	/**
	 * Lock
	 */
	protected ReadWriteLock mRTLock = new ReentrantReadWriteLock();

	public void acquireReadLock() {
		mRTLock.readLock().lock();
	}

	public void acquireWriteLock() {
		mRTLock.writeLock().lock();
	}

	/**
	 * Adds all delivered Data Items.
	 *
	 * @param collection the Collection.
	 */
	public void addAll(Collection collection) {
		for (Iterator it = collection.iterator(); it.hasNext();) {
			sequentialAdd((IndexEntry) it.next());
		}
		collection.clear();
		flushInsert();
	}


	/**
	 * Adds all delivered Data Items.
	 *
	 * @param dataTable the index table to merge.
	 */
	public void addAll(DBIndexTable dataTable) {
		int updateCount =0;
		acquireWriteLock();
		try {
			String sql = "MERGE into " + this.getTableName() +" KEY (GUID) SELECT "+getColumnNames()+" FROM " + dataTable.getTableName()+";";
			Statement st;
			try {
				Connection con = mDBManager.getConnection();
				st = con.createStatement();
				updateCount=st.executeUpdate(sql);
				st.clearBatch();
				st.close();
				
				// release connection to pool
				con.close();
				
			} catch (SQLException ex) {
				ex.printStackTrace();
			}

			if (updateCount>0) mSignature=null;
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Add a data table entry.
	 *
	 * @param hostID the Host ID in the DB.
	 * @return the data table id if the host exists or has been inserted, -1 if insertion failed.
	 */
	protected int addDataTable(int hostID) {
		acquireWriteLock();

		try {
			// check if host already exists
			int id = getIndexTableID(hostID);
			if (id >= 0)
				return id;

			// create data table

			return mDBManager.insertSQL(DBManager.INDEX_TABLES_TABLE, "null, " + hostID + ", ''");
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Adds a Host to the DB.
	 *
	 * @param host the host to add.
	 * @return The host ID if the host has been inserted, -1 otherwise.
	 */

	protected int addHost(PGridHost host) {
		
		int hostID =	mHBManager.addHost(host); 
		
		return hostID;
		
		/*acquireWriteLock();
		try {
			// check if host already exists

			int id = HostsCacheList.containsKey(host.getGUID().toString())?HostsCacheList.get(host.getGUID().toString()):-1;
			if (id >= 0)
				return id;
			// add new host
			String hostGUID = host.getGUID().toString();
			String hostAddress = host.getAddressString();
			int hostPort = host.getPort();
			int hostQOS = host.getSpeed();
			String hostPath = host.getPath();
		//	int hostID = mDBManager.mergeSQL(DBManager.HOSTS_TABLE, "GUID", "null,'" + hostGUID + "','" + hostAddress + "',"+ hostQOS + "','" + hostPath + "'," + 0);
	
			
		
			
			HostsCacheList.put(hostGUID, hostID);
		
		} finally {
			releaseWriteLock();
		}*/
	}


	/**
	 * Inserts a IndexEntry in the DB.
	 *
	 * @param item the data item.
	 */
	public void addIndexEntry(IndexEntry item) {
		sequentialAdd(item);
		flushInsert();
	}

	/**
	 * Returns the SQL statement used to select all data items of this data table.
	 *
	 * @return the SQL statement.
	 */
	String asView() {
		return asView("");
	}

	/**
	 * Returns the SQL statement used to select all data items of this data table.
	 *
	 * @return the SQL statement.
	 */
	String asView(String keyPrefix) {
		return " SELECT "+getColumnNames()+" FROM "+getTableName()+	((keyPrefix.length()!=0)?" WHERE KEY LIKE '"+keyPrefix+"%'":"")+" ";
	}

	/**
	 * Removes all data items from the DB belonging to this data table.
	 */
	public void clear() {
		acquireWriteLock();

		try {
			mDBManager.execDeleteSQL("delete from " + DBManager.HOSTS_TABLE + " where HOST_ID in (select HOST_ID from " +
					DBManager.INDEX_TABLES_TABLE + " where INDEX_TABLE_ID = " + mDataTableID + ")");
			mDBManager.execDeleteSQL("DELETE FROM "+getTableName());
			refreshAllTypeDataTables();
		} finally {
			releaseWriteLock();
		}
	}

	public Object clone() throws CloneNotSupportedException {
		DBIndexTable dt = (DBIndexTable) super.clone();

		// we need to duplicate dataitem->datatable relationship
		if (mDataTableID != -1) {
			// ...
		}
		dt.mRTLock = new ReentrantReadWriteLock();

		return dt;
	}

	/**
	 * Returns the number of locally managed DataItems.
	 *
	 * @return the number of DataItems.
	 */
	public int count() {
		acquireWriteLock();

		try {
			if (mCountSignature != null && mCountSignature.equals(getSignature())) return mCount;

			mCountSignature = getSignature();
			mCount = mDBManager.count("*", getTableName());

			return mCount;
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Removes the data table from the DB.
	 */
	public void delete() {
		acquireWriteLock();
		
		try {
			if (mTemporaryTable){
				mDBManager.execDeleteSQL("delete from " + DBManager.HOSTS_TABLE + " where HOST_ID = "+mHostID+";");
				mDBManager.execSQL("DROP TABLE IF EXISTS " + getTableName() +";");
			}
			else{
				mDBManager.execDeleteSQL("delete from " + DBManager.INDEX_TABLES_TABLE + " where INDEX_TABLE_ID = "+mDataTableID+";");
				mDBManager.execSQL("DROP TABLE IF EXISTS " + getTableName() +";");
			}
			Constants.LOGGER.finest("Deleting DBIdxTable : "+getTableName());
			mSignature = null;
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Duplicates all data items of this data table for to the given one.
	 * @param dataTable the data table to extend.
	 */
	public void duplicate(DBIndexTable dataTable) {
		// duplicate data items from the table
		duplicate(dataTable, 0,0);
	}

	/**
	 * Duplicates all data items of this data table for to the given one.
	 * @param dataTable the data table to extend.
	 */
	public void duplicate(DBIndexTable dataTable, int limit, int offset) {
		acquireWriteLock();
		mDBManager.execSQL("TRUNCATE TABLE "+dataTable.getTableName());

		try {
			String str = "";

			if ((limit+offset) != 0) {
				str = " order by GUID LIMIT "+limit + " OFFSET "+offset;
			}
			// duplicate data items from the table
			String sql = "INSERT into " + dataTable.getTableName() +
			" SELECT " + dataTable.getColumnNames() + " FROM " +
			this.getTableName() + str +";";

			Constants.LOGGER.finest(sql.toString());

			mDBManager.execSQL(sql);
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Insert all index entries currently in temp buffer
	 */
	public void flushInsert1() {
		boolean modified = true;

		Statement st;
		acquireWriteLock();
		try {
			synchronized(mTempEntries) {
				if (mTempEntries.isEmpty()) return;
				try {
					Connection con = mDBManager.getConnection();
					st = con.createStatement();
					for (Iterator it = mTempEntries.iterator(); it.hasNext(); ) {
						st.addBatch((String) it.next());
					}
					st.executeBatch();
					st.clearBatch();
					st.close();
					// release connection to pool
					con.close();
					mDBManager.compactDB();
				} catch (BatchUpdateException ex) {
					// if there is a unique constraint violation, check if we should reset signature
					int[] results = ex.getUpdateCounts();
					modified = false;
					for (int i=0; i<results.length; i++) {
						if (results[i] != PreparedStatement.EXECUTE_FAILED) {
							modified = true;
							break;
						}
					}
				} catch (SQLException e) {
//					e.printStackTrace();
				}


				mTempEntries.clear();

				if (modified) mSignature = null;
			}

		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * An utility function to get the dataItemID given the indexEntry;
	 * @param entry
	 * @return
	 */
	private int getDataItemID(IndexEntry entry){
		ResultSet rs = null;
		int dataItemID = 0;
		try{
			String query = " SELECT " + this.getColumnNames() +
			" FROM " + this.getTableName() +
			" WHERE GUID = '"+entry.getGUID().toString()+ "' AND TYPE_NAME = '"+entry.getTypeString()+"'";
			
			Connection rsCon = DBManager.sharedInstance().getConnection();
			if (rsCon == null) {
				Constants.LOGGER.warning("No connection available !");
			}
			try {	
				Statement rsSt = rsCon.createStatement();
				rs = rsSt.executeQuery(DBManager.sharedInstance().preprocessSQL(query));
				rsSt.clearBatch();
			} catch (SQLException ex) {
				if (PGridP2P.sharedInstance().isInDebugMode())
					ex.printStackTrace();
				Constants.LOGGER.warning("problem " + ex + "\nexecuting query: " + query +"\nerror code: " + ex.getErrorCode());
			}
			
			
			if(rs == null || !rs.next()) return -1;
			dataItemID = rs.getInt("DATA_ID");
			rsCon.close();
		}
		catch(SQLException e){
			e.printStackTrace();
		}
		return dataItemID;

	}

	protected PGridHost getHost(int id) {
		acquireReadLock();
		//System.out.println("DBIndexTable.getHost(): id=" + id);
		try {
			String query = "SELECT * from HOSTS where HOST_ID = "+id+";";

			ResultSet rs = null;
			Connection rsCon = DBManager.sharedInstance().getConnection();
			if (rsCon == null) {
				Constants.LOGGER.warning("No connection available !");
			}
			try {	
				Statement rsSt = rsCon.createStatement();
				rs = rsSt.executeQuery(DBManager.sharedInstance().preprocessSQL(query));
				rsSt.clearBatch();
			} catch (SQLException ex) {
				if (PGridP2P.sharedInstance().isInDebugMode())
					ex.printStackTrace();
				Constants.LOGGER.warning("problem " + ex + "\nexecuting query: " + query +"\nerror code: " + ex.getErrorCode());
			}
			if (rs != null) {
				try {
					rs.next();
					PGridHost host = PGridHost.getHost(rs.getString("GUID"), rs.getString("ADDRESS"), rs.getString("PORT"));
					rsCon.close();
					return host;
				} catch (SQLException e) {
				}
			}
			rsCon.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			releaseReadLock();
		}
		return null;
	}

	/**
	 * Returns the host id in the DB.
	 *
	 * @param host the host.
	 * @return the host ID if the host exists, -1 otherwise.
	 */
	protected int getHostID(PGridHost host) {
		acquireReadLock();
		int returnValue;
		try {
			returnValue = -1;
			if (host.getGUID() == null) return -1;

			try {
				Connection con = mDBManager.getConnection();
				Statement st = con.createStatement();

				ResultSet rs = st.executeQuery("select HOST_ID from " + DBManager.HOSTS_TABLE + " where GUID = '"+host.getGUID().toString()+"'");

				if (rs.next())
					returnValue = rs.getInt("HOST_ID");
				st.clearBatch();
				st.close();
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			}
		} finally {
			releaseReadLock();
		}
		return returnValue;
	}

	/**
	 * Returns the host id in the DB.
	 *
	 * @param host the host.
	 * @return the host ID if the host exists, -1 otherwise.
	 */
	private boolean indexItemExists(IndexEntry ie) {
		acquireReadLock();
		try {
			if (ie.getGUID() == null) return false;

			try {
				Connection con = mDBManager.getConnection();
				Statement st = con.createStatement();

				ResultSet rs =st.executeQuery("select GUID from " + DBManager.INDEX_ITEMS_TABLE + " where GUID = '"+ie.getGUID().toString()+"'");

				if (rs.next()){
					st.close();
					con.close();
					return true;
				}
				st.close();
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			}
		} finally {
			releaseReadLock();
		}
		return false;
	}

	/**
	 * Returns the list of all Data Items.
	 *
	 * @return the list of all data items.
	 */
	public Collection getIndexEntries() {
		return getIndexEntries("");
	}


	/**
	 * Returns the list of all Data Items.
	 *
	 * @return the list of all data items.
	 */
	public Collection getIndexEntries(long limit, long offset) {
		return getIndexEntries("", limit, offset);
	}

	/**
	 * Returns the list of data items for the given result set.
	 *
	 * @param rs the result set.
	 * @return the list of data items.
	 */
	protected Collection getIndexEntries(ResultSet rs) {
		acquireReadLock();
		try {
			Vector dataitems = new Vector();
			p2p.index.IndexEntry entry;
			String guid;
			try {
				while (rs.next()) {
					guid = rs.getString("dGUID");
					GUID dGuid = GUID.getGUID(guid);
					entry = null;

					if (entry == null) {
						Type type = (Type) PGridIndexFactory.sharedInstance().getTypeByString(TYPE_NAME);
						PGridKey key = new PGridKey(rs.getString("KEY"));
						PGridHost host = PGridHost.getHost(rs.getString("hGUID"), rs.getString("ADDR"), String.valueOf(PORT_NUMBER));
						Object data = rs.getString("KEY");
						IDBDataTypeHandler dbDataTypeHandler = mDBManager.getDBTypeHandler(type.toString());
						if(dbDataTypeHandler != null){
							data = dbDataTypeHandler.getDataItem(rs.getInt("DATA_ID"));
		
						}
						entry = mIndexManager.createIndexEntry(dGuid, type, key, host, data);
					}
					dataitems.add(entry);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return dataitems;
		} finally {
			releaseReadLock();
		}
	}

	/**
	 * Returns the list of data items with the given prefix.
	 *
	 * @param prefix the common prefix of the selected data items.
	 * @return the list of data items.
	 */
	public Collection getIndexEntries(String keyPrefix) {
		return getIndexEntries(keyPrefix, 0,0);
	}

	/**
	 * Returns the list of data items with the given prefix.
	 *
	 * @param prefix the common prefix of the selected data items.
	 * @param limit SQL limit to use
	 * @param offset SQL offset to use
	 * @return the list of data items.
	 */
	public Collection getIndexEntries(String keyPrefix, long limit, long offset) {
		acquireReadLock();
		try {

			String query = getIndexEntriesAsSQL(keyPrefix, limit, offset);
			ResultSet rs = null;
			Connection rsCon = DBManager.sharedInstance().getConnection();
			if (rsCon == null) {
				Constants.LOGGER.warning("No connection available !");
			}
			try {	
				Statement rsSt = rsCon.createStatement();
				rs = rsSt.executeQuery(DBManager.sharedInstance().preprocessSQL(query));
				rsSt.clearBatch();
			} catch (SQLException ex) {
				if (PGridP2P.sharedInstance().isInDebugMode())
					ex.printStackTrace();
				Constants.LOGGER.warning("problem " + ex + "\nexecuting query: " + query +"\nerror code: " + ex.getErrorCode());
			}
			
			
			if (rs == null) {
				Constants.LOGGER.warning("Couldn't get data items prefixed by \""+keyPrefix+"\". DB connection probably closed.");
				rsCon.close();
				return new Vector();
			}
			Collection col = getIndexEntries(rs);
			rsCon.close();
			return col;
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			releaseReadLock();
		}
		return new Vector();
	}


	/**
	 * Returns the SQL statement used to select all data items of this data table.
	 *
	 * @return the SQL statement.
	 * @param limit
	 * @param offset
	 */
	private String getIndexEntriesAsSQL(long limit, long offset) {
		return getIndexEntriesAsSQL("", limit, offset);
	}

	/**
	 * Returns the SQL statement used to select all data items with the given prefix of this data table.
	 * if both limit and offset are set to 0, all entries are returned
	 *
	 * @return the SQL statement.
	 */
	private String getIndexEntriesAsSQL(String keyPrefix, long limit, long offset) {
		String limitStr = "";
		String condition = "";
		if ((limit+offset)!=0)
			limitStr = " ORDER BY dGUID LIMIT "+limit+" OFFSET "+offset;
		if (keyPrefix.length() != 0)
			condition = "WHERE ii.KEY LIKE '"+keyPrefix+"%' ";

		sqlQuery = new StringBuilder("");
		sqlQuery.append("SELECT ").append(IDX_ENTRY_COLUMNS).append("FROM ").append(IDX_ENTRY_TABLE)
		.append(condition).append(limitStr);

		return sqlQuery.toString();
	}

	/**
	 * Returns the list of all Data Items.
	 *
	 * @return the list of all data items.
	 */
	public String getIndexEntriesAsXML() {
		return getIndexEntriesAsXML("");
	}

	/**
	 * Returns the list of all Data Items.
	 *
	 * @return the list of all data items.
	 */
	public String getIndexEntriesAsXML(long limit, long offset) {
		return getIndexEntriesAsXML("", limit, offset);
	}

	/**
	 * Returns the list of data items for the given result set.
	 *
	 * @param rs the result set.
	 * @return the list of data items.
	 */
	protected String getIndexEntriesAsXML(ResultSet rs) {
		long time = System.currentTimeMillis();
		int count =0;
		StringBuffer resultset = new StringBuffer(256);
		System.out.println("inside the problematic dbindextable");
		try {
			String guid;
			String type;
			String key;
			PGridHost host;
			String data;
			Long x;
			Long y;
			Long id;

			while (rs.next()) {
				guid = rs.getString("dGUID");
				type = TYPE_NAME;
				key = rs.getString("KEY");
				host = PGridHost.getHost(rs.getString("hGUID"), rs.getString("ADDR"),String.valueOf(PORT_NUMBER));
				data = rs.getString("KEY");
				BitVector[]  xy = MGridUtils.HilbertInverseConvertor(Long.parseLong(key, 2));
				
				x = xy[0].toExactLong();
				y = xy[0].toExactLong();
				id = Long.parseLong(rs.getString("ID"));
				Point point = new Point(x,y,id);
				IDBDataTypeHandler dbDataTypeHandler = mDBManager.getDBTypeHandler(type.toString());
				if(dbDataTypeHandler != null){
					data = dbDataTypeHandler.getDataItem(rs.getInt("DATA_ID")).toString();
				}

				resultset.append(XMLSimpleIndexEntry.toXMLString("", "\n", guid, type, key, host, data,point));
				count++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return resultset.toString();

	}

	/**
	 * Returns the list of data items with the given prefix.
	 *
	 * @param prefix the common prefix of the selected data items.
	 * @return the list of data items.
	 */
	public String getIndexEntriesAsXML(String keyPrefix) {
		return getIndexEntriesAsXML(keyPrefix, 0,0);
	}

	/**
	 * Returns the list of data items with the given prefix.
	 *
	 * @param prefix the common prefix of the selected data items.
	 * @param limit SQL limit to use
	 * @param offset SQL offset to use
	 * @return the list of data items.
	 */
	public String getIndexEntriesAsXML(String keyPrefix, long limit, long offset) {
		ResultSet rs = null;

		acquireReadLock();
		try {
			String query = getIndexEntriesAsSQL(keyPrefix, limit, offset);
			Connection rsCon = DBManager.sharedInstance().getConnection();
			if (rsCon == null) {
				Constants.LOGGER.warning("No connection available !");
			}
			try {	
				Statement rsSt = rsCon.createStatement();
				rs = rsSt.executeQuery(DBManager.sharedInstance().preprocessSQL(query));
				rsSt.clearBatch();
			} catch (SQLException ex) {
				if (PGridP2P.sharedInstance().isInDebugMode())
					ex.printStackTrace();
				Constants.LOGGER.warning("problem " + ex + "\nexecuting query: " + query +"\nerror code: " + ex.getErrorCode());
			}
			
			if (rs == null) {
				Constants.LOGGER.warning("Couldn't get data items prefixed by \""+keyPrefix+"\". DB connection probably closed.");
				rsCon.close();
				return "";
			}
			String res = getIndexEntriesAsXML(rs);
			rsCon.close();
			return res;

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			releaseReadLock();
		}
		return "";

	}

	/**
	 * Returns the list of data items with the given prefix.
	 *
	 * @param prefix the common prefix of the selected data items.
	 * @return the list of data items.
	 */
	public Collection getIndexEntriesPrefixed(String dataPrefix) {
		return getIndexEntriesPrefixed(dataPrefix, 0,0);
	}

	/**
	 * Returns the list of data items with the given prefix.
	 *
	 * @param prefix the common prefix of the selected data items.
	 * @param limit SQL limit to use
	 * @param offset SQL offset to use
	 * @return the list of data items.
	 */
	
public Collection getIndexEntriesPrefixed(String dataPrefix, long limit, long offset) {
	
	// call HBase Index
			Collection res = mHBManager.getSingleRecord(dataPrefix);
			return res;
	}

	/**
	 * Returns the list of data items with the given prefix.
	 *
	 * @param lprefix the common prefix of the selected data items.
	 * @param hprefix the common prefix of the selected data items.
	 * @return the list of data items.
	 * @throws IOException 
	 */
	public Collection getIndexEntriesPrefixed(String ldataPrefix, String hdataPrefix){
		return getIndexEntriesPrefixed(ldataPrefix, hdataPrefix, 0,0);
	}
	
	/**
	 * Returns the list of data items with the given prefix.
	 *
	 * @param lprefix the common prefix of the selected data items.
	 * @param hprefix the common prefix of the selected data items.
	 * @param origxMin the original query x minimum
	 * @param origxMax the original query x maximum
	 * @param origyMin the original query y minimum
	 * @param origyMax the original query y maximum
	 * @return the list of data items.
	 * @throws IOException 
	 */
	public Collection getIndexEntriesPrefixed(String ldataPrefix, String hdataPrefix, Long origxMin, Long origxMax, Long origyMin, Long origyMax){
		return getIndexEntriesPrefixed(ldataPrefix, hdataPrefix,origxMin,  origxMax, origyMin, origyMax, 0,0);
	}

	/**
	 * Returns the list of data items with the given prefix.
	 *
	 * @param lprefix the common prefix of the selected data items.
	 * @param hprefix the common prefix of the selected data items.
	 * @return the list of data items.
	 * @throws IOException 
	 */
	 public Collection getIndexEntriesPrefixed(String ldataPrefix, String hdataPrefix, long limit, long offset) {
		return null;
	}

	/**
	 * Returns the list of data items with the given prefix.
	 *
	 * @param lprefix the common prefix of the selected data items.
	 * @param hprefix the common prefix of the selected data items.
	 * @param origxMin the original query x minimum
	 * @param origxMax the original query x maximum
	 * @param origyMin the original query y minimum
	 * @param origyMax the original query y maximum
	 * @return the list of data items.
	 * @throws IOException 
	 */	
	public Collection getIndexEntriesPrefixed(String ldataPrefix, String hdataPrefix, Long origxMin, Long origxMax, Long origyMin, Long origyMax,long limit, long offset) {
		// call HBaseIndex	
		Collection res = mHBManager.getRangeRecords(ldataPrefix, hdataPrefix, origxMin,  origxMax, origyMin, origyMax);
		return res;
	}

	/**
	 * Returns the list of data items with the given prefix.
	 *
	 * @param prefix the common prefix of the selected data items.
	 * @return the list of data items.
	 */
	public String getIndexEntriesPrefixedAsXML(String dataPrefix) {
		return getIndexEntriesPrefixedAsXML(dataPrefix, 0,0);
	}

	/**
	 * Returns the list of data items with the given prefix.
	 *
	 * @param prefix the common prefix of the selected data items.
	 * @param limit SQL limit to use
	 * @param offset SQL offset to use
	 * @return the list of data items.
	 */
	public String getIndexEntriesPrefixedAsXML(String dataPrefix, long limit, long offset) {
		acquireReadLock();
		ResultSet rs = null;
		Statement st = null;
		String res = null;
		try {
			
			try {
				Connection con = mDBManager.getConnection();
				st = con.createStatement();
				String limitStr = "";
				if ((limit+offset)!=0)
					limitStr = " order by di.KEY LIMIT "+limit+" OFFSET "+offset;

				sqlQuery = new StringBuilder("");
				sqlQuery.append("SELECT ").append(IDX_ENTRY_COLUMNS).append("FROM ").append(IDX_ENTRY_TABLE)
				.append("WHERE ii.KEY like '").append(DBManager.escapeSQL(dataPrefix)).append("%'  ").append(limitStr);
				rs = st.executeQuery(sqlQuery.toString());
				res = getIndexEntriesAsXML(rs);
				st.clearBatch();
				st.close();
				con.close();

			} catch (SQLException e) {
				e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			}

			return res;
		} finally {
			try {
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			releaseReadLock();
			
		}
	}

	/**
	 * Returns the list of data items with the given prefix.
	 *
	 * @param lprefix the common prefix of the selected data items.
	 * @param hprefix the common prefix of the selected data items.
	 * @return the list of data items.
	 */
	public String getIndexEntriesPrefixedAsXML(String ldataPrefix, String hdataPrefix) {
		return getIndexEntriesPrefixedAsXML(ldataPrefix, hdataPrefix, 0,0);
	}

	/**
	 * Returns the list of data items with the given prefix.
	 *
	 * @param lprefix the common prefix of the selected data items.
	 * @param hprefix the common prefix of the selected data items.
	 * @return the list of data items.
	 */
	public String getIndexEntriesPrefixedAsXML(String ldataPrefix, String hdataPrefix, long limit, long offset) {
		acquireReadLock();
		ResultSet rs = null;
		Statement st = null;
		String res = null;
		try {
			try {

				String limitStr = "";
				if ((limit+offset)!=0)
					limitStr = " order by di.KEY LIMIT "+limit+" OFFSET "+offset;
				Connection con = mDBManager.getConnection();
				st = con.createStatement();

				sqlQuery = new StringBuilder("");
				sqlQuery.append("SELECT ").append(IDX_ENTRY_COLUMNS).append("FROM ").append(IDX_ENTRY_TABLE)
				.append("WHERE (ii.DATA > '").append(DBManager.escapeSQL(ldataPrefix)).append("' OR ii.DATA LIKE '").append(DBManager.escapeSQL(ldataPrefix)).append("%') AND ")
				.append(" (ii.DATA < '").append(DBManager.escapeSQL(hdataPrefix)).append("' OR ii.DATA LIKE '").append(DBManager.escapeSQL(hdataPrefix)).append("%') ")
				.append(limitStr);
				rs = st.executeQuery(sqlQuery.toString());
				
				res = getIndexEntriesAsXML(rs);
				st.clearBatch();
				st.close();
				con.close();

			} catch (SQLException e) {
				e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			}
			return res;
		} finally {
			try {
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			releaseReadLock();
		}
	}

	/**
	 * Given the dataItemID, dataItem and the type this function returns the IndexEntry.
	 * If for given dataItemID and type ,if the indexEntry does not belong to the local index table then this function returns null.
	 * 
	 * @param dataItemID
	 * @param dataItem
	 * @param type
	 * @return
	 */
	public p2p.index.IndexEntry getIndexEntry(int dataItemID, String dataItem, Type type){
		ResultSet rs = null;
		p2p.index.IndexEntry entry = null;
		acquireReadLock();
		try{
			try {
				sqlQuery = new StringBuilder("");
				sqlQuery.append("SELECT ").append(IDX_ENTRY_COLUMNS).append("FROM ").append(IDX_ENTRY_TABLE)
				.append("WHERE ii.DATA_ID = ").append(dataItemID).append(" AND ii.TYPE_NAME = '").append(type.toString()).append("';");
				
				Connection rsCon = DBManager.sharedInstance().getConnection();
				if (rsCon == null) {
					Constants.LOGGER.warning("No connection available !");
				}
				try {	
					Statement rsSt = rsCon.createStatement();
					rs = rsSt.executeQuery(DBManager.sharedInstance().preprocessSQL(sqlQuery.toString()));
					rsSt.clearBatch();
				} catch (SQLException ex) {
					if (PGridP2P.sharedInstance().isInDebugMode())
						ex.printStackTrace();
					Constants.LOGGER.warning("problem " + ex + "\nexecuting query: " + sqlQuery.toString() +"\nerror code: " + ex.getErrorCode());
				}
				
				if (rs == null || !rs.next()) {
					rsCon.close();
					return null;
				}

				GUID dGuid = GUID.getGUID(rs.getString("dGUID"));
				Type dtype = (Type) PGridIndexFactory.sharedInstance().getTypeByString(rs.getString("TYPE_NAME"));
				PGridKey key = new PGridKey(rs.getString("KEY"));
				PGridHost host = PGridHost.getHost(rs.getString("hGUID"), rs.getString("ADDR"), rs.getString("PORT"));
				entry = mIndexManager.createIndexEntry(dGuid, dtype, key, host, dataItem);
				rsCon.close();
			} catch (SQLException e) {
				e.printStackTrace();
			} 
			return entry;
		}
		finally {
			releaseReadLock();
		}
	}

	/**
	 * Given the dataItemID, dataItem and the type this function returns the IndexEntry As XML string.
	 * If for given dataItemID and type ,if the indexEntry does not belong to the local index table then this function returns null.
	 * 
	 * @param dataItemID
	 * @param dataItem
	 * @param type
	 * @return
	 */
	public String getIndexEntryAsXML(int dataItemID, String dataItem, Type type){
		ResultSet rs = null;
		StringBuffer resultset = new StringBuffer();

		acquireReadLock();
		try{
			try {
				sqlQuery = new StringBuilder("");
				sqlQuery.append("SELECT ").append(IDX_ENTRY_COLUMNS).append("FROM ").append(IDX_ENTRY_TABLE)
				.append("WHERE  ii.DATA_ID = ").append(dataItemID).append(" AND ii.TYPE_NAME = '").append(type.toString()).append("';");
				
				Connection rsCon = DBManager.sharedInstance().getConnection();
				if (rsCon == null) {
					Constants.LOGGER.warning("No connection available !");
				}
				try {	
					Statement rsSt = rsCon.createStatement();
					rs = rsSt.executeQuery(DBManager.sharedInstance().preprocessSQL(sqlQuery.toString()));
					rsSt.clearBatch();
				} catch (SQLException ex) {
					if (PGridP2P.sharedInstance().isInDebugMode())
						ex.printStackTrace();
					Constants.LOGGER.warning("problem " + ex + "\nexecuting query: " + sqlQuery.toString() +"\nerror code: " + ex.getErrorCode());
				}
				
				
				
				if (rs == null) {
					rsCon.close();
					return null;
				}

				GUID dGuid = GUID.getGUID(rs.getString("dGUID"));
				Type dtype = (Type) PGridIndexFactory.sharedInstance().getTypeByString(rs.getString("TYPE_NAME"));
				PGridKey key = new PGridKey(rs.getString("KEY"));
				PGridHost host = PGridHost.getHost(rs.getString("hGUID"), rs.getString("ADDR"), rs.getString("PORT"));
				Point point = new Point(Long.parseLong(rs.getString("X")),Long.parseLong(rs.getString("Y")),Long.parseLong(rs.getString("ID")));
				resultset.append(XMLSimpleIndexEntry.toXMLString("", "\n", dGuid.toString(), dtype.toString(), key.toString(), host, dataItem,point));
				rsCon.close();
			} catch (SQLException e) {
				e.printStackTrace();
			} 
			return resultset.toString();
		}
		finally {
			releaseReadLock();
		}
	}

	/**
	 * Returns the data table ID if a data table exists.
	 *
	 * @return the data table ID if it exists, -1 otherwise.
	 */
	protected int getIndexTableID() {
		return mDataTableID;
	}

	/**
	 * Returns the data table ID if a data table exists for the given host ID.
	 *
	 * @param hostID the host ID.
	 * @return the data table ID if it exists, -1 otherwise.
	 */
	protected int getIndexTableID(int hostID) {
		acquireReadLock();
		try {
			String query = "select INDEX_TABLE_ID from " + DBManager.INDEX_TABLES_TABLE + " where HOST_ID=" + hostID;
			

			ResultSet rs = null;
			Connection rsCon = null;
			try {
				rsCon = DBManager.sharedInstance().getConnection();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			if (rsCon == null) {
				Constants.LOGGER.warning("No connection available !");
			}
			try {	
				Statement rsSt = rsCon.createStatement();
				rs = rsSt.executeQuery(DBManager.sharedInstance().preprocessSQL(query));
				rsSt.clearBatch();
			} catch (SQLException ex) {
				if (PGridP2P.sharedInstance().isInDebugMode())
					ex.printStackTrace();
				Constants.LOGGER.warning("problem " + ex + "\nexecuting query: " + query +"\nerror code: " + ex.getErrorCode());
			}
			
			
			try {
				if (rs != null && rs.next()){ 
						int res = rs.getInt("INDEX_TABLE_ID");
						rsCon.close();
						return res;
				}		
				rsCon.close();
				return -1;
			} catch (SQLException e) {
				e.printStackTrace();
				return -1;
			}
		} finally {
			releaseReadLock();
		}
	}

	/**
	 * Returns the list of data items from the host.
	 *
	 * @return the list of data items.
	 */
	public Collection getOwnedIndexEntries() {
		acquireReadLock();
		try {
			sqlQuery = new StringBuilder("");
			sqlQuery.append("SELECT ").append(IDX_ENTRY_COLUMNS).append("FROM ").append(IDX_ENTRY_TABLE)
			.append("WHERE h.host_id = ").append(mHostID).append(")");

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
				rs = rsSt.executeQuery(DBManager.sharedInstance().preprocessSQL(sqlQuery.toString()));
				rsSt.clearBatch();
			} catch (SQLException ex) {
				if (PGridP2P.sharedInstance().isInDebugMode())
					ex.printStackTrace();
				Constants.LOGGER.warning("problem " + ex + "\nexecuting query: " + sqlQuery.toString() +"\nerror code: " + ex.getErrorCode());
			}
			
			if (rs == null) {
				Constants.LOGGER.warning("Couldn't retrieved owned data items. DB connection probably closed.");
				try {
					rsCon.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				return new Vector();
			}
			Collection col = getIndexEntries(rs);
			try {
				rsCon.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return col;
		} finally {
			releaseReadLock();
		}
	}

	/**
	 * Returns the signature for the data items.
	 *
	 * @return the signature.
	 */
	public Signature getSignature() {
		acquireWriteLock();
		try {
			// return signature if known
			if (mSignature != null)
				return mSignature;

			mSignature = new Signature();
			return mSignature;

		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Retreive signature cache from DB
	 * @return
	 */
	public Hashtable getSignatureCache() {
		acquireReadLock();
		Hashtable ht;
		try {
			
			String query = "select * from "+DBManager.SIGNATURE_CACHE;
			
			ResultSet rs = null;
			Connection rsCon = null;
			try {
				rsCon = DBManager.sharedInstance().getConnection();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			if (rsCon == null) {
				Constants.LOGGER.warning("No connection available !");
			}
			try {	
				Statement rsSt = rsCon.createStatement();
				rs = rsSt.executeQuery(DBManager.sharedInstance().preprocessSQL(query));
				rsSt.clearBatch();
			} catch (SQLException ex) {
				if (PGridP2P.sharedInstance().isInDebugMode())
					ex.printStackTrace();
				Constants.LOGGER.warning("problem " + ex + "\nexecuting query: " + query +"\nerror code: " + ex.getErrorCode());
			}
			
			ht = new Hashtable();

			if (rs != null) {
				try {
					while (rs.next()) {
						int id = rs.getInt("HOST_ID");
						Signature sign = new Signature(rs.getString("SIGNATURE"));
						PGridHost host = getHost(id);
						ht.put(host, sign);
					}
				} catch (SQLException e) {

				}
			}
			try {
				rsCon.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} finally {
			releaseReadLock();
		}
		return ht;
	}

	public String getTableName(){
		return mTableName;
	}

	/**
	 * Initialize a datatable for an anonymous peer
	 */
	public void init() {
		acquireWriteLock();

		try {
			mHostID = mDBManager.insertSQL(DBManager.HOSTS_TABLE, "null,'" + GUID.getGUID() + "','NA',0,'NA','NA',1");			
		} finally{
			releaseWriteLock();
		}
		// check if a data table already exists and create it
		mDataTableID = addDataTable(mHostID);
	}
	
	/**
	 * Initialize a Host datatable
	 *
	 *  @param host the host.
	 */
	public void init(PGridHost host) {
		// add the corresponding host
		mHostID = addHost(host);

	//	System.out.println("DBIndexTable.init(): mHostID=" + mHostID);
		// check if a data table already exists and create it
		mDataTableID = addDataTable(mHostID);

//		// read the signature from the DB
		if(host.isLocalHost()) restoreConfig();
	}

	/**
	 * Create a random subset of this data base
	 * @param limit nomber of entries in the subset.
	 * @return return a subset
	 */
	public DBIndexTable randomSubSet(int limit) {
		TempDBIndexTable table = new TempDBIndexTable();
		int count = count();
		if (limit >= count) {
			duplicate(table);
			return table;
		}

		duplicate(table, limit, mRnd.nextInt(count-limit));
		return table;
	}

	/**
	 * Removes all the DATA entries in all custom type data tables if there is no reference to them from the INDEX_ITEMS_TABLE.
	 * 
	 */
	private void refreshAllTypeDataTables(){
		for(Object typeString:mDBManager.getDBDataTypeHandlers().keySet()){
			refreshTypeDataTable((String)typeString);
		}
	}

	/**
	 * Removes all the DATA entries in the given type data table if there is no reference to them from the INDEX_ITEMS_TABLE.
	 * @param typeString
	 */
	private void refreshTypeDataTable(String typeString){
		IDBDataTypeHandler dbDataTypeHandler = mDBManager.getDBTypeHandler(typeString);
		if(dbDataTypeHandler != null){
			acquireWriteLock();
			try {
				StringBuffer sql = new StringBuffer();
				sql.append("( SELECT DATA_ID FROM ").append(this.getTableName()).append("TYPE_NAME = '"+typeString+"') ");
				ResultSet rs = null;
				try {
					
					Connection rsCon = DBManager.sharedInstance().getConnection();
					if (rsCon == null) {
						Constants.LOGGER.warning("No connection available !");
					}
					try {	
						Statement rsSt = rsCon.createStatement();
						rs = rsSt.executeQuery(DBManager.sharedInstance().preprocessSQL(sql.toString()));
						rsSt.clearBatch();
					} catch (SQLException ex) {
						if (PGridP2P.sharedInstance().isInDebugMode())
							ex.printStackTrace();
						Constants.LOGGER.warning("problem " + ex + "\nexecuting query: " + sql.toString() +"\nerror code: " + ex.getErrorCode());
					}
					
					dbDataTypeHandler.removeDataItem(rs.getInt("DATA_ID"));
					rsCon.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mDBManager.execDeleteSQL(sql.toString());
			} finally {
				releaseWriteLock();
			}
		}
	}

	public void releaseReadLock() {
		mRTLock.readLock().unlock();
	}

	public void releaseWriteLock() {
		mRTLock.writeLock().unlock();
	}

	/**
	 * Removes all given data items.
	 *
	 * @param items the items to remove.
	 */
	public void removeAll(Collection items) {
		acquireWriteLock();
		try {
			if (items == null)
				throw new NullPointerException();
			if(items.isEmpty()) return;

			for (Iterator it = items.iterator(); it.hasNext();) {
				IndexEntry dataitem = (IndexEntry)it.next();
				removeIndexEntry(dataitem);
			}
			items.clear();
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Removes all given data items owned by peer.
	 *
	 * @param items the items to remove.
	 */
	// TODO: parse items to remove only items owned by peer
	public void removeAll(Collection items, Peer peer) {
		removeAll(items);
	}

	/**
	 * Removes all given data items.
	 */
	public void removeAllOwnedIndexEntries() {
		acquireWriteLock();

		try {
			StringBuffer sql = new StringBuffer();
			sql.append("delete from ").append(DBManager.INDEX_ITEMS_TABLE).append(" where HOST_ID = "+mHostID+";");
			mDBManager.execDeleteSQL(sql.toString());
			refreshAllTypeDataTables();
		} finally {
			releaseWriteLock();
		}

	}


	/**
	 * Removes the given Data Item.
	 *
	 * @param dataItem the item to remove.
	 */
	public void removeIndexEntry(IndexEntry dataItem) {
		StringBuffer sql = new StringBuffer();
		acquireWriteLock();

		try {
			if (dataItem == null)
				throw new NullPointerException();

			
			IDBDataTypeHandler dbDataTypeHandler = mDBManager.getDBTypeHandler(dataItem.getTypeString());
			if(dbDataTypeHandler != null){
				int dataItemID = getDataItemID(dataItem);
				String query = " SELECT DATA_ID " +
				" FROM " + getTableName() +
				" WHERE GUID = '"+dataItem.getGUID().toString()+ "' AND TYPE_NAME = '"+dataItem.getTypeString()+"'";
				int count = mDBManager.count("DATA_ID", "("+query+")");
				if(count == 1){
					dbDataTypeHandler.removeDataItem(dataItemID);
				}
			}

			mDBManager.execDeleteSQL(sql.append("delete from ").append(getTableName()).append(" where GUID = '").append(dataItem.getGUID().toString()).append("'").toString());
			mSignature = null;

		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * An useful function to restore all the saved config information
	 *
	 */
	private void restoreConfig(){
		acquireReadLock();
		try{
			mSignature = new Signature(mDBManager.getConfig("signature"));
			if(mSignature != null){
				String tmpCount = mDBManager.getConfig("count");
				mCount = tmpCount==null?0:Integer.parseInt(tmpCount);
			}
		}
		finally {
			releaseReadLock();
		}	
	}
	
	/**
	 * This method saves the signature and deletes duplicate GUID type data items
	 */
	public void save() {
		acquireWriteLock();

		try {
			mDBManager.setConfig("signature", getSignature().toString()); //saves the datatable signature
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Save signature cache to DB
	 * @param cache a hash table contining host as key and Signature as values
	 */
	public void saveSignatureCache(Hashtable cache) {
		acquireWriteLock();
		try {
			Enumeration it = cache.keys();
			String path = mPGrid.getLocalPath();

			// clear cache table
			mDBManager.execSQL("Delete from "+DBManager.SIGNATURE_CACHE+";");

			while (it.hasMoreElements()) {
				PGridHost host = (PGridHost) it.nextElement();

				if (host.getPath().equals(path)) {
					int id = addHost(host);
					mDBManager.execSQL("INSERT INTO "+DBManager.SIGNATURE_CACHE+" values ("+id+",'"+cache.get(host).toString()+"');");
				}
			}
		} finally {
			releaseWriteLock();
		}
	}


	/**
	 * Copy part of the current index table into <code>it</code>.<code>it</code> will contain entries selected with keys and except.
	 * if delete is true, this method will split the current index table based on <code>keys</code> and <code>except</code>.
	 * If delete is false, this method will only copy entries from this index table to <code>it</code>.
	 *
	 * @param it an empty index table (no check is done for performance reasons)
	 * @param keys	keys to copy
	 * @param except keys to avoid
	 * @param delete true if copied entries should be removed from this index table.
	 */
	public void select(DBIndexTable it, String[] keys, String[] except, boolean delete) {
	}

	/**
	 * Insert a new index entry in DB. Insertion is not done immediately but only when the
	 * internal buffer is reached. Insert can be forced with <code>flushInsert</code>.
	 *
	 * @param entry index entry to be added.
	 */
	public void sequentialAdd1(IndexEntry entry) {
		/*acquireWriteLock();
		addIndexEntry(entry);
		try {
			synchronized(mTempEntries) {
				IDBDataTypeHandler dbTypeHandler = mDBManager.getDBTypeHandler(entry.getTypeString());
				int dataItemID;
				String dataItem;
				if(dbTypeHandler != null){
					dataItemID = dbTypeHandler.addDataItem(entry.getData());
					dataItem = "";
				}else{
					dataItemID = 0;
					dataItem = entry.getData().toString();
				}

				StringBuffer sql = new StringBuffer();
				sql.append("merge into ").append(getTableName()).append(" key(GUID)").
				append(" values (").
				append("'").
				append(entry.getKey().toString()).append("', '").
				append(entry.getGUID().toString()).append("', '").
				append(entry.getType().toString()).append("', ").
				append(addHost((PGridHost) entry.getPeer())).
			//	append(", '").append(DBManager.escapeSQL(dataItem)).append("', ").
				append(dataItemID).
				append(");");
				mTempEntries.add(sql.toString());

				if (mTempEntries.size() > MAX_TEMP_ENTRIES) {
					flushInsert();
				}
			}
		} finally {
			releaseWriteLock();
		}*/
	}

	/**
	 * Sets the data table to contain only elements with the given path.
	 *
	 * @param path the path of the local index table.
	 */
	public void setIndexTable(String path) {
		boolean modified = false;
		int deleted=0;

		try {
			String sql = "delete from " + DBManager.INDEX_ITEMS_TABLE + " where not KEY LIKE '"+path+"%');";
			Connection con = mDBManager.getConnection();
			PreparedStatement st = con.prepareStatement(sql);

			acquireWriteLock();
			try {
				if (!st.execute())
					deleted = st.getUpdateCount();

				if (deleted==0) modified = true;
				refreshAllTypeDataTables();

			} finally {
				releaseWriteLock();
			}
			
			st.clearParameters();
			st.clearBatch();
			st.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (modified) mSignature = null;

	}

	/**
	 * Sets the signature of the data table.
	 * @param signature the signature.
	 */
	public void setSignature(Signature signature) {
		acquireReadLock();
		try {
			mSignature = signature;
		} finally {
			releaseReadLock();
		}
	}

	/**
	 * Shuts down the DataBase and closes the connection
	 */
	public void shutdown() {
		acquireWriteLock();
		try {
			mDBManager.setConfig("signature", getSignature().toString()); // saves the datatable signature
			mDBManager.setConfig("count", mCount+""); //saves the datatable signature
			
			// if DB is not persistant, remove it.
			if (!mSticky) {
				mDBManager.execSQL("delete from "+DBManager.INDEX_ITEMS_TABLE);
			}
			
			mHBManager.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Update all given data items.
	 *
	 * @param items the items to Update.
	 */
	public void updateAll(Collection items) {
		acquireWriteLock();
		try {
			if (items == null)
				throw new NullPointerException();

			for (Iterator it = items.iterator(); it.hasNext();) {
				IndexEntry item = (IndexEntry)it.next();
				updateIndexEntry(item);
			}
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Update all given data items owned by peer.
	 *
	 * @param items the items to remove.
	 */
	// TODO: parse items to update only items owned by peer
	public void updateAll(Collection items, Peer peer) {
		updateAll(items);
	}

	/**
	 * Update a IndexEntry in the DB.
	 *
	 * @param item the data item.
	 */
	public void updateIndexEntry(IndexEntry item) {
		acquireWriteLock();
		try {
			String data = item.getData().toString();
			IDBDataTypeHandler dbDataTypeHandler = mDBManager.getDBTypeHandler(item.getTypeString());
			if(dbDataTypeHandler != null){
				data = "";
				dbDataTypeHandler.updateDataItem(getDataItemID(item), item.getData());
			}

			String sql = "update " + getTableName() + " SET DATA = '"+item.getKey().toString()+"', KEY = '"+item.getKey().toString()+"' WHERE GUID = '"+item.getGUID().toString()+"'"; 

			System.out.println(sql);
			Constants.LOGGER.finest(sql);
			mDBManager.execSQL(sql);

			mSignature = null;

		} finally {
			releaseWriteLock();
		}
	}


	private Connection insCon = null;
	private PreparedStatement insSt = null;
	
	

	/**
	 * Inserts all given data items owned by peer.
	 *
	 * @param items the items to insert.
	 */
	
	public void sequentialAdd(IndexEntry entry){
	/*	acquireWriteLock();
		try {
			if(insCon == null || insCon.isClosed()){
				insCon = mDBManager.getConnection();
				insSt =  insCon.prepareStatement("MERGE INTO "+getTableName()+" KEY(GUID) VALUES(?,?,?,?,?,?,?,?,?)");
			}

			IDBDataTypeHandler dbTypeHandler = mDBManager.getDBTypeHandler(entry.getTypeString());
			int dataItemID;
			String dataItem;
			if(dbTypeHandler != null){
				dataItemID = dbTypeHandler.addDataItem(entry.getData());
				dataItem = "";
			}else{
				dataItemID = 0;
				dataItem = entry.getData().toString();
			}
			insSt.setString(1, entry.getKey().toString());
		//	insSt.setLong(2, entry.getPoint().x);
		//	insSt.setLong(3, entry.getPoint().y);
			insSt.setLong(2, entry.getPoint().id);
			insSt.setString(3, entry.getGUID().toString());
		//	insSt.setString(4, entry.getType().toString());
			insSt.setInt(4, addHost((PGridHost) entry.getPeer()));
		//	insSt.setString(8, DBManager.escapeSQL(dataItem));
			insSt.setInt(5, dataItemID);
			insSt.execute();
			insSt.clearParameters();
			insSt.clearBatch();
			entry = null;
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Misformed Index Entry :"+entry);
		}finally {
			releaseWriteLock();
			mSignature = null;
		} */
	}

	public void sequentialAdd(String entry){
		/*acquireWriteLock();
		addIndexEntry(entry);
		try {
			if(insCon == null || insCon.isClosed() || insSt == null || insSt.getConnection().isClosed()){
				insCon = mDBManager.getConnection();
				insSt =  insCon.prepareStatement("MERGE INTO "+getTableName()+" KEY(GUID) VALUES(?,?,?,?,?,?,?,?,?)");
			}
			String[] tkns = entry.split(",");
			if(tkns.length != 5){
				System.err.println("Warning: Error in entry -> " + entry);
				return;
			}
			String Key = tkns[0];
		//	String Data = tkns[1];
			String dGUID = tkns[2];
			String typeString = TYPE_NAME;
			String hGuid = tkns[4];
			String hostIP = tkns[3];
			String hostPort = String.valueOf(PORT_NUMBER);

			IDBDataTypeHandler dbTypeHandler = mDBManager.getDBTypeHandler(tkns[3]);
			int dataItemID;
			String dataItem;
			if(dbTypeHandler != null){
				dataItemID = dbTypeHandler.addDataItem(tkns[1]);
				dataItem = "";
			}else{
				dataItemID = 0;
				dataItem = tkns[1];
			}

			insSt.setString(1, Key);
			insSt.setString(2, dGUID);
			insSt.setString(3, typeString);
			insSt.setInt(4, addHost(PGridHost.getHost(hGuid, hostIP, hostPort)));
			insSt.setString(5, DBManager.escapeSQL(dataItem));
			insSt.setInt(6, dataItemID);

			insSt.execute();
			insSt.clearParameters();
			insSt.clearBatch();
			entry = null;
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Misformed Index Entry :"+entry);
		}finally {
			releaseWriteLock();
			mSignature = null;
		}*/
	}
	public void flushInsert() {
		
	}
}

