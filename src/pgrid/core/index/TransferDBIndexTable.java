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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import p2p.index.IDBDataTypeHandler;
import pgrid.Constants;
import pgrid.GUID;
import pgrid.IndexEntry;
import pgrid.PGridHost;
import pgrid.core.DBManager;
import pgrid.interfaces.basic.PGridP2P;

public class TransferDBIndexTable extends DBIndexTable {
	private String filterClause="";
	private void setFilterClause(String clause){
		filterClause = clause;
	}
	private String mTableName;
	private Signature mSignature;
	private boolean shouldDelete = true;
	public void setShouldDelete(boolean delete){
		shouldDelete = delete;
	}
	/**
	 * The data item manager.
	 */
	private IndexManager mIndexManager = IndexManager.getInstance();
	private DBIndexTable localIndexTable = mIndexManager.getIndexTable();


	/**
	 * The DataBase manager
	 */
	private DBManager mDBManager = DBManager.sharedInstance();

	private void setTableName(){

		mTableName = DBManager.INDEX_ITEMS_TABLE+"_"+mDataTableID;
		mTableName += "_TRNS";
		return;
	}
	public String getTableName(){
		setTableName();
		return mTableName;
	}
	protected void createTableAndIndexes(){
		if(!mDBManager.tableExists(getTableName())){
			StringBuilder sql = new StringBuilder();
			sql.append("CREATE TEMP TABLE ").append(getTableName())
			.append("(  	INDEX_ITEM_ID varchar not null,")
			.append("	constraint U_").append(getTableName()).append(" unique(INDEX_ITEM_ID));");			
			mDBManager.execSQL(sql.toString());
		}
		return ;
	}
	public String getColumnNames(){
		return " INDEX_ITEM_ID ";
	}

	public TransferDBIndexTable() {
		createTableAndIndexes();
	}
	public TransferDBIndexTable(PGridHost host) {
		super(host);
		createTableAndIndexes();
	}


	@Override
	public void delete() {
		long time = System.currentTimeMillis();
		acquireWriteLock();
		try {
			mDBManager.execDeleteSQL("delete from " + DBManager.HOSTS_TABLE + " where HOST_ID = "+mHostID+";");
			mDBManager.execDeleteSQL("delete from " + DBManager.INDEX_TABLES_TABLE + " where INDEX_TABLE_ID = "+mDataTableID+";");
			if(shouldDelete){
				Constants.LOGGER.warning("Deleting the transferred items:"+getTableName());
				deleteIdxRecordsFaster();
				localIndexTable.setSignature(null);
			}
			mDBManager.execSQL("DROP TABLE " + getTableName() +";");
		} finally {
			Constants.LOGGER.warning("Drop table "+getTableName()+" : took "+(System.currentTimeMillis()-time)+" msecs");
			releaseWriteLock();
		}
	}

	@Override
	public int count() {
		acquireWriteLock();
		try {
			mCount = mDBManager.count("*", getTableName());
			Constants.LOGGER.warning("Count..IdxTable("+getTableName()+") :"+mCount);
			return mCount;
		} finally {
			releaseWriteLock();
		}	
	}

	@Override
	protected String getIndexEntriesAsXML(ResultSet res) {
		// TODO Auto-generated method stub
		StringBuffer sb = new StringBuffer();
		List<Integer> indexItemIds = new ArrayList<Integer>();
		if(res == null){
			return null;
		}else{
			try {
				while(res.next()){
					indexItemIds.add(res.getInt("INDEX_ITEM_ID"));
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		acquireReadLock();
		try{
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ").append(IDX_ENTRY_COLUMNS).append(" FROM ").append(IDX_ENTRY_TABLE).append(" WHERE ii.INDEX_ITEM_ID = ");

			Connection rsCon = null;
			try {
				rsCon = DBManager.sharedInstance().getConnection();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			for (Integer indexItemId : indexItemIds) {
				
				
				ResultSet rs = null;
				String query = sql.toString()+""+indexItemId+"";
				
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
				
				
				sb.append(super.getIndexEntriesAsXML(rs));
			}
			try {
				rsCon.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		finally{
			releaseReadLock();
		}

		return sb.toString();
	}

	private void deleteIdxRecordsFaster(){
		long time = System.currentTimeMillis();
		long items = 0;
		int count = count();
		Constants.LOGGER.warning("Deleting "+count+"  records from "+getTableName());
		int BATCH_LIMIT = 1000;
		StringBuilder deleteSQL = new StringBuilder();
		deleteSQL.append("DELETE FROM ").append(mDBManager.INDEX_ITEMS_TABLE).append(" WHERE INDEX_ITEM_ID >= ? AND INDEX_ITEM_ID < * ").append((filterClause.length()>0?" AND "+filterClause:""));
		acquireWriteLock();
		try{
			for (int i = 0; items < count; i++) {
				items += mDBManager.execDeleteSQL(deleteSQL.toString().replace("?", (i*BATCH_LIMIT)+"").replace("*", ((i+1)*BATCH_LIMIT)+""));
				Constants.LOGGER.warning("==> Deleting "+items+" items took "+(System.currentTimeMillis() - time)/1000 + " secs");
				if(i%5 == 0)mDBManager.compactDB();
			}
			mDBManager.compactDB();
		}
		finally{
			releaseWriteLock();

		}
	}

	private void deleteIdxRecords(){
		long time = System.currentTimeMillis();
		long items = 0;
		int count = count();
		Constants.LOGGER.warning("Deleting "+count+"  records from "+getTableName());
		int idx = 0;
		ResultSet rs;

		while(true){
			rs = null;
			String query = "SELECT INDEX_ITEM_ID FROM "+getTableName()+" LIMIT 1000 OFFSET "+idx;
			
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
			
			List<Integer> indexItemIds = new ArrayList<Integer>();
			if(rs == null){
				try {
					rsCon.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				return;
			} else if(idx >= count){
				try {
					rsCon.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				return;
			} else {
				try {
					while(rs.next()){
						indexItemIds.add(rs.getInt("INDEX_ITEM_ID"));
					}
					idx += indexItemIds.size();
					rsCon.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			
				acquireWriteLock();
				try{
					Statement st = null;
					Connection con = mDBManager.getConnection();
					st = con.createStatement();
					StringBuilder sql = new StringBuilder();
					sql.append("DELETE FROM ").append(mDBManager.INDEX_ITEMS_TABLE).append(" WHERE INDEX_ITEM_ID = ");
					ResultSet resTmp;
					for (Integer indexItemId : indexItemIds) {
						++items;
						st.addBatch(sql.toString()+""+indexItemId+"");
					}
					st.executeBatch();
					st.clearBatch();
					st.close();
					con.close();
					if(items%1000 == 0){
						Constants.LOGGER.warning("==> Deleting "+items+" items took "+(System.currentTimeMillis() - time)/1000 + " secs");
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
				finally{
					releaseWriteLock();
				}

			}

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
		Statement st;
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
		Constants.LOGGER.warning("SELECT START: INSERT INTO "+getTableName()+" "+str);int iter = 0;
		long t = System.currentTimeMillis();

		String selectSQL = " SELECT "+getColumnNames()+",KEY FROM "+it.getTableName() + " WHERE INDEX_ITEM_ID >= ? AND INDEX_ITEM_ID < * "+(str.length()>0?" AND "+str:"");

		int maxCount = it.count();
		int BATCH_LIMIT = 2000;
		for(int i=0;BATCH_LIMIT*i<=maxCount;i++){
			acquireWriteLock();
//			acquireReadLock();
			try{
				ResultSet rs = null;
				String query = selectSQL.replace("?", (BATCH_LIMIT*i)+"").replace("*", (BATCH_LIMIT*(i+1)+""));
				
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
				
				
				
				if(!rs.next()){
					continue;
				}else{
					int ii = 0;
					Constants.LOGGER.warning(++iter+" Iteration "+getTableName());
					st = null;
					Connection con = mDBManager.getConnection();
					st = con.createStatement();
					do{
						++ii;
						int indexItemId = rs.getInt("INDEX_ITEM_ID");
						st.addBatch("INSERT INTO "+getTableName()+" VALUES("+indexItemId+")");
					}while(rs.next());
//					acquireWriteLock();
					try {
						if(st != null){
							st.executeBatch();
							Constants.LOGGER.warning("this iteration count "+ii);
							if(i%10 == 0) mDBManager.compactDB();
						}
					} finally {
						st.clearBatch();
						st.close();
						con.close();
//						releaseWriteLock();
					}
				}
				rsCon.close();
			} catch (SQLException e){
				e.printStackTrace();
			}finally{
				releaseWriteLock();
//				releaseReadLock();
			}				
		}
		Constants.LOGGER.warning("SELECT END__ INSERT took:("+getTableName()+") : "+(System.currentTimeMillis()-t)+" msec");
	}
	public int select(DBIndexTable it, String[] keys, String[] except,int maxIdxId, int limit, int maxLimit) {
		Statement st;
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
		setFilterClause(str);
//		Constants.LOGGER.warning("SELECT START: INSERT INTO "+getTableName()+" "+str);int iter = 0;
		long t = System.currentTimeMillis();

		String selectSQL = " SELECT "+getColumnNames()+" FROM "+it.getTableName() + " WHERE INDEX_ITEM_ID > ? and INDEX_ITEM_ID<=* "+(str.length()>0?" AND "+str:"")+" LIMIT "+limit;

//		int maxCount = it.count();
		int BATCH_LIMIT = limit;
		int currentCount = 0;
		while(currentCount != maxLimit){
			acquireWriteLock();
			try{
				ResultSet rs = null;
				String query = selectSQL.replace("?", (maxIdxId)+"").replace("*", ((maxIdxId+BATCH_LIMIT)+""));
//				Constants.LOGGER.warning(sql);
				
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
				
				
				if(!rs.next()){
					continue;
				}else{
					int ii = 0;
//					Constants.LOGGER.warning(++iter+" Iteration "+getTableName());
					st = null;
					Connection con = mDBManager.getConnection();
					st = con.createStatement();
					do{
						++ii;
						currentCount++;
						int indexItemId = rs.getInt("INDEX_ITEM_ID");
						st.addBatch("INSERT INTO "+getTableName()+" VALUES("+indexItemId+")");
						maxIdxId = indexItemId;
						if(currentCount >= maxLimit) break;
					}while(rs.next());
//					acquireWriteLock();
					try {
						if(st != null){
							st.executeBatch();
							Constants.LOGGER.warning("this iteration count "+ii);
							if(currentCount >= maxLimit){
								mDBManager.compactDB();
								Constants.LOGGER.warning("MaxIdxId : "+maxIdxId);
								return maxIdxId;
							}
						}
					} finally {
						st.clearBatch();
						st.close();
						con.close();
//						releaseWriteLock();
					}
				}
				rsCon.close();
			} catch (SQLException e){
				e.printStackTrace();
			}finally{
				releaseWriteLock();
//				releaseReadLock();
			}				

		}

		Constants.LOGGER.warning("ERROR----shud not reach here");
		return 0;
	}

}
