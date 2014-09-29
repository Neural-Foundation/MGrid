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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import p2p.index.IDBDataTypeHandler;
import pgrid.Constants;
import pgrid.GUID;
import pgrid.IndexEntry;
import pgrid.PGridHost;
import pgrid.core.DBManager;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.interfaces.index.XMLSimpleIndexEntry;

public class TempDBIndexTable extends DBIndexTable{
	
	
	private Signature mSignature;
	protected void setTableName(String addon){
		mTableName = DBManager.INDEX_ITEMS_TABLE+"_"+mDataTableID;
		mTableName += "_TMP";
		mTableName = "L_"+mTableName;
		return;
	}
	public String getTableName(){
		return mTableName;
	}

	protected void createTableAndIndexes(){
			StringBuilder sql = new StringBuilder();
			sql.append("DROP TABLE IF EXISTS ").append(getTableName()).append(";")
				.append("CREATE TABLE IF NOT EXISTS ").append(getTableName())
				.append("(  	GUID varchar not null,")
	     		.append("	KEY varchar not null")
	     		.append("	);")			
	     		.append("CREATE INDEX IF NOT EXISTS I_INDEX_").append(getTableName()).append("_KEY ON ").append(getTableName()).append("(KEY);");
			int ex = mDBManager.execSQL(sql.toString());
			if(ex != 0){
				Constants.LOGGER.finest("PROBLEM in creating new Temp_DB_Table : "+mTableName);
			}else{
				Constants.LOGGER.finest("SUCCESSFUL in creating new Temp_DB_Table : "+mTableName);
			}
			mSignature = null;
//		}
		return ;
	}
	public String getColumnNames(){
		return " GUID, KEY ";
	}
	
	public TempDBIndexTable() {
		super();
		Constants.LOGGER.finest("Creating new RANDOM_PREDICTION_SUBSET : "+mTableName);
	}
	public TempDBIndexTable(PGridHost host) {
		super();
		Constants.LOGGER.finest("Creating new REMOTE_RANDOM_PREDICTION_SUBSET : "+mTableName);
	}


	@Override
	public void delete() {
		acquireWriteLock();
		try {
			Constants.LOGGER.finest("Deleting RANDOM_PREDICTION_SUBSET : "+mTableName);

			mDBManager.execDeleteSQL("delete from " + DBManager.HOSTS_TABLE + " where HOST_ID = "+mHostID+";");
			mDBManager.execDeleteSQL("delete from " + DBManager.INDEX_TABLES_TABLE + " where INDEX_TABLE_ID = "+mDataTableID+";");
			mDBManager.execSQL("DROP TABLE IF EXISTS " + getTableName() +";");
			mSignature = null;
		} finally {
			releaseWriteLock();
		}
	}
	@Override
	public int count() {
		acquireWriteLock();
		try {
			if(mCountSignature != null && mCountSignature == getSignature()) return mCount;
			mCount = mDBManager.count("*", getTableName());
			mSignature = getSignature();
			mCountSignature = mSignature;
			return mCount;
		} finally {
			releaseWriteLock();
		}	
	}
	public String getIndexEntriesAsXML() {
		long time = System.currentTimeMillis();
		
		String query =  "SELECT * FROM "+getTableName();
		
		
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
		
		
		StringBuffer resultset = new StringBuffer(256);

		try {
			String guid;
			String key;

			PGridHost host = null;
			try {
				host = PGridHost.getHost(InetAddress.getByName("localhost"), 10000); //dummy host.
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			while (rs.next()) {
				guid = rs.getString("GUID");
				key = rs.getString("KEY");

				resultset.append(XMLSimpleIndexEntry.toXMLString("", "\n", guid, "", key, host, ""));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		try {
			rsCon.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return resultset.toString();
	}
	/**
	 * Insert a new index entry in DB. Insertion is not done immediately but only when the
	 * internal buffer is reached. Insert can be forced with <code>flushInsert</code>.
	 *
	 * @param entry index entry to be added.
	 */
	public void sequentialAdd1(IndexEntry entry) {
		acquireWriteLock();
		/*addIndexEntry(entry);*/
		try {
			synchronized(mTempEntries) {
				String dataItem;
				
				StringBuffer sql = new StringBuffer();
				sql.append("INSERT INTO ").append(getTableName()).
						append(" values ('").
						append(entry.getGUID().toString()).append("', '").
						append(entry.getKey().toString()).append("' ").
						append(");");
				mTempEntries.add(sql.toString());

				if (mTempEntries.size() > MAX_TEMP_ENTRIES) {
					flushInsert();
					mSignature = null;
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
		int updateCount = 0;
		String str = "";
		if (except != null)
		for (int i=0; i<except.length; i++) {
			if (str.length()>0) str+=" AND ";
			str += " not KEY LIKE '"+except[i]+"%'";
		}

		if (keys != null)
		for (int i=0; i<keys.length; i++) {
			if (str.length()>0) str+=" OR ";
			str += " KEY LIKE '"+keys[i]+"%'";
		}

		try {
			// duplicate data items from the table
			String sql = " INSERT INTO " +getTableName() +" SELECT "+ it.getColumnNames() + " FROM "+
						it.getTableName() +(str.length()>0?" WHERE "+str:"")+" ";

			long t = System.currentTimeMillis();
			Constants.LOGGER.finest("SELECT START :"+sql);
			Connection con = mDBManager.getConnection();
			PreparedStatement st = con.prepareStatement(sql);
			acquireWriteLock();
			try {
				updateCount=st.executeUpdate();
				st.close();
				
				Constants.LOGGER.finest("SELECT END took:"+sql+" : "+(System.currentTimeMillis()-t)+" msec");
			} finally {
				releaseWriteLock();
				con.close();
			}
			
		} catch (SQLException e){
			e.printStackTrace();
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
	
	public void setSignature(Signature signature) {
		acquireReadLock();
		try {
			mSignature = signature;
		} finally {
			releaseReadLock();
		}
	}
	
	public void sequentialAdd(IndexEntry entry){
		acquireWriteLock();
		/*addIndexEntry(entry);*/
		try {
			
			Connection con = mDBManager.getConnection(); 
			PreparedStatement st = con.prepareStatement("INSERT INTO "+getTableName()+" VALUES(?,?)");
			st.setString(1, entry.getGUID().toString());
			st.setString(2, entry.getKey().toString());

			st.execute();
			st.clearParameters();
			st.clearBatch();
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			releaseWriteLock();
		}
	}
	public void sequentialAdd(String Guid,String Key){
		acquireWriteLock();
		/*addIndexEntry(entry);*/
		try {
			
			Connection con = mDBManager.getConnection(); 
			PreparedStatement st = con.prepareStatement("INSERT INTO "+getTableName()+" VALUES(?,?)");
			st.setString(1, Guid);
			st.setString(2, Key);

			st.execute();
			st.clearParameters();
			st.clearBatch();
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			releaseWriteLock();
		}
	}
	public void flushInsert() {
		
	}


}
