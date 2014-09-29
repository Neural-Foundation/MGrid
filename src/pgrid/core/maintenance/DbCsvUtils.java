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
package pgrid.core.maintenance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;

import com.google.uzaygezen.core.BitVector;

import mgrid.core.HBaseManager;
import mgrid.core.MGridUtils;
import mgrid.core.Point;
import p2p.index.IDBDataTypeHandler;
import pgrid.Constants;
import pgrid.GUID;
import pgrid.IndexEntry;
import pgrid.PGridHost;
import pgrid.Properties;
import pgrid.PGridKey;
import pgrid.Type;
import pgrid.core.DBManager;
import pgrid.core.index.CSVIndexTable;
import pgrid.core.index.DBIndexTable;
import pgrid.core.index.HostsCacheList;
import pgrid.core.index.IndexManager;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.interfaces.index.PGridIndexFactory;

/**
 * This class is an utility, which helps in DB to CSV synchronization and CSV to
 * DB synchronization as well.
 * 
 * @author yerva
 * 
 */

public class DbCsvUtils {

	private static DbCsvUtils SHARED_INSTANCE = null;
	
	private static PGridP2P mPGridP2P = PGridP2P.sharedInstance();
	
	private static final String TYPE_NAME = mPGridP2P.propertyString(Properties.TYPE_NAME);
	
	private static final int PORT_NUMBER = Integer.parseInt(mPGridP2P.propertyString(Properties.PORT_NUMBER));

	Logger logger = Logger.getLogger(this.getClass().getName());

	public static DbCsvUtils sharedInstance() {
		if (SHARED_INSTANCE == null)
			SHARED_INSTANCE = new DbCsvUtils();
		return SHARED_INSTANCE;
	}

	private DbCsvUtils() {
		mDBManager = DBManager.sharedInstance();
		mHBManager = HBaseManager.sharedInstance();
		mDBIndexTable = IndexManager.getInstance().getIndexTable();

		// Different CSV files

		mCSVIndexTable = IndexManager.getInstance().getCSVIndexTable();
		mDistrCSVIndexTable = IndexManager.getInstance()
				.getDistrCSVIndexTable();
		mCurrDistrCSVIndexTable = new CSVIndexTable("CurrDistr.csv");
		mToDistrCSVIndexTable = IndexManager.getInstance()
				.getToDistrCSVIndexTable();
	}

	/**
	 * The HBaseDataBase manager
	 */
	private HBaseManager mHBManager = null;

	/**
	 * * Reference to the DBManager instance
	 */
	private DBManager mDBManager = null;

	/**
	 * Reference to the CSVIndexTable instance
	 */
	private CSVIndexTable mCSVIndexTable = null;

	/**
	 * Reference to the recvCSVIndexTable instance
	 */
	private CSVIndexTable mDistrCSVIndexTable = null;

	/**
	 * Reference to the recvCSVIndexTable instance
	 */
	private CSVIndexTable mCurrDistrCSVIndexTable = null;

	/**
	 * Reference to the recvCSVIndexTable instance
	 */
	private CSVIndexTable mToDistrCSVIndexTable = null;

	/**
	 * Reference to the DBIndexTable instance
	 */
	private DBIndexTable mDBIndexTable = null;

	private long startTime;

	/**
	 * Represents the INDEX_ENTRY COLUMNS which includes columns from
	 * INDEX_ITEMS_TABLE and HOSTS_TABLE
	 */
	private String IDX_ENTRY_COLUMNS = " ii.GUID as dGUID,ii.ID, ii.KEY, ii.HOST_ID, ii.DATA, ii.DATA_ID, h.GUID as hGUID,h.ADDRESS as ADDR ";

	/**
	 * Represents the INDEX_ENTRY TABLE which is INNER JOIN OF INDEX_ITEMS_TABLE
	 * and HOSTS_TABLE
	 */
	private String IDX_ENTRY_TABLE = " " + DBManager.INDEX_ITEMS_TABLE
			+ " as ii INNER JOIN " + DBManager.HOSTS_TABLE
			+ " as h on h.HOST_ID = ii.HOST_ID ";

	public static final String CSV_DELIMITER = ",";

	public static final String CSV_DELIMITER_MODIFIER = "###";

	private static final Logger LOGGER = Logger.getLogger("PGrid.Exchanger");

	/**
	 * Synchronize DB to CSV. Writing the contents of DB to CSV, only if DB and
	 * CSV are out of sync.
	 * 
	 */
	public void dbToCsv() {
		LOGGER.finest("--> BEFORE db2Csv - local size:"
				+ mCSVIndexTable.count());
		mCSVIndexTable.delete();

		// Read from DB either in batches or in a single go and then write to
		// the CSV file;
		String query = "SELECT " + IDX_ENTRY_COLUMNS + " FROM "
				+ IDX_ENTRY_TABLE;

		try {
			ResultSet rs = null;
			Connection rsCon = DBManager.sharedInstance().getConnection();
			if (rsCon == null) {
				Constants.LOGGER.warning("No connection available !");
			}
			try {
				Statement rsSt = rsCon.createStatement();
				rs = rsSt.executeQuery(DBManager.sharedInstance()
						.preprocessSQL(query));
				rsSt.clearBatch();
			} catch (SQLException ex) {
				if (PGridP2P.sharedInstance().isInDebugMode())
					ex.printStackTrace();
				Constants.LOGGER.warning("problem " + ex
						+ "\nexecuting query: " + query + "\nerror code: "
						+ ex.getErrorCode());
			}

			mCSVIndexTable.openFileForWriting();
			while (rs.next()) {
				String KEY = rs.getString("KEY");
				Long ID = rs.getLong("ID");
				String dGUID = rs.getString("dGUID");
				String HOST_IP = rs.getString("ADDR");
				String hGUID = rs.getString("hGUID");
				String dataItem = new StringBuilder()
						.append(KEY)
						.append(CSV_DELIMITER)
						.append(ID)
						.append(CSV_DELIMITER)
						.append(dGUID).append(CSV_DELIMITER).append(TYPE_NAME)
						.append(CSV_DELIMITER).append(HOST_IP)
						.append(CSV_DELIMITER).append(hGUID).toString();

				mCSVIndexTable.addIndexEntry(dataItem.replace("\r", "@###")
						.replace("\n", "@@##"));
			}
			rsCon.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mCSVIndexTable.closeFileForWriting();
			mCSVIndexTable.setSignature(mDBIndexTable.getSignature());

		}
	}

	public boolean isCsvUptodateWithDb() {
		if (mDBIndexTable.getSignature() != null)
			if (mDBIndexTable.getSignature().equals(
					mCSVIndexTable.getSignature())) {
				Constants.LOGGER.finest("Csv is uptodate with DB");
				return true;
			}
		Constants.LOGGER.finest("Csv is NOT uptodate with DB");
		return false;

	}

	public void csvToDb() {
		
			try {
				mCSVIndexTable.openFileForReading();
				String line = "";
			while ((line = mCSVIndexTable.getNextLine()) != null) {
				try {
					mHBManager.addIndexEntry((IndexEntry) stringToIndexEntry(line));
				} catch (Exception e) {
					System.err.println("Error in this line:" + line);
					e.printStackTrace();
				}  
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				mHBManager.flushTable();
				mCSVIndexTable.closeFileOnReading();
			}
		mDBIndexTable.setSignature(null);

	}

	public boolean isDbUptodateWithCsv() {
		if (mDBIndexTable == null)
			return false;
		if (mDBIndexTable.getSignature() != null)
			if (mDBIndexTable.getSignature().equals(
					mCSVIndexTable.getSignature())) {
				Constants.LOGGER.finest("DB is uptodate with CSV");
				return true;
			}
		Constants.LOGGER.finest("DB is NOT uptodate with CSV");
		return false;

	}

	public void filterCSV(String filterRemoteKey, String localKey,
			boolean split, String toRemoteFileName) {
		LOGGER.finest("--> BEFORE FILTERING - local size:"
				+ mCSVIndexTable.count());
		CSVIndexTable mRemoteFileteredCsvFile, mLocalFilteredCsvFile;
		Constants.LOGGER.finest("Filtering with key : " + filterRemoteKey
				+ " ; shudSPLIT :" + (split ? "TRUE" : "FALSE"));

		mRemoteFileteredCsvFile = new CSVIndexTable(toRemoteFileName, false);
		mLocalFilteredCsvFile = new CSVIndexTable("localFiltered.csv", false);

		try {
			mCSVIndexTable.openFileForReading();
			mRemoteFileteredCsvFile.openFileForWriting();
			mToDistrCSVIndexTable.openFileForWriting();

			if (split) {
				mLocalFilteredCsvFile.openFileForWriting();
			}

			String line = "";
			int toDis = 0;
			int tot = 0, loc = 0, rem = 0;
			while ((line = mCSVIndexTable.getNextLine()) != null) {
				tot++;
				if (line.startsWith(filterRemoteKey)) {
					rem++;
					mRemoteFileteredCsvFile.addIndexEntry(line);
				}
				if (line.startsWith(localKey)) {
					loc++;
					if (split)
						mLocalFilteredCsvFile.addIndexEntry(line);
				}
				if (!line.startsWith(localKey)
						&& !line.startsWith(filterRemoteKey)) {
					toDis++;
					mToDistrCSVIndexTable.addIndexEntry(line);
				}
			}
			if (split) {
				if (mCSVIndexTable.count() != mLocalFilteredCsvFile.count())
					mCSVIndexTable.setSignature(null);
				mCSVIndexTable.closeFileOnReading();
				mLocalFilteredCsvFile.closeFileForWriting();
				mCSVIndexTable.changeTo(mLocalFilteredCsvFile);
			}
			LOGGER.finest("--> AFTER  FILTERING - local size:("
					+ mCSVIndexTable.count() + ","
					+ mRemoteFileteredCsvFile.count() + "," + toDis + ")");
			LOGGER.finest("--> **** " + tot + " ==>(" + loc + "," + rem + ","
					+ toDis + ")");
			if (tot != loc + rem + toDis) {
				LOGGER.finest("--> **************** possible MISMATCH *********************");

			}
		} catch (Exception e) {
			System.out.println("inside filtercsv");
			e.printStackTrace();
		} finally {
			mCSVIndexTable.closeFileOnReading();
			mRemoteFileteredCsvFile.closeFileForWriting();
			if (split)
				mLocalFilteredCsvFile.closeFileForWriting();
			mToDistrCSVIndexTable.closeFileForWriting();
		}

	}

	/**
	 * Merges the LOCAL and the RECEIVED FILE( that is received from the other
	 * peer)
	 * 
	 */
	public void mergeLocalAndRemoteCSVFiles(String recvFileName) {
		CSVIndexTable mReceivedCSVFile = new CSVIndexTable(recvFileName);

		LOGGER.finest("--> BEFORE Merging - local size:("
				+ mCSVIndexTable.count() + "," + mReceivedCSVFile.count() + ")");

		if (mReceivedCSVFile == null || mReceivedCSVFile.count() == 0) {
			LOGGER.finest("--> AFTER  Merging - local size:("
					+ mCSVIndexTable.count() + ")");
			if (mReceivedCSVFile != null) {
				mReceivedCSVFile.delete();
			}
			return;
		}
		CSVIndexTable mMergedCSVIndexTable = null;
		try {
			DBCsvSynchManager.sharedInstance().resetLastCsvUpdateTime();
			mCSVIndexTable.openFileForWriting();
			mMergedCSVIndexTable = CsvUtils.mergeCSVIndexTables(mCSVIndexTable,
					mReceivedCSVFile);
			mCSVIndexTable.setSignature(null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mCSVIndexTable.closeFileForWriting();
			if (mMergedCSVIndexTable != null) {
				mCSVIndexTable.changeTo(mMergedCSVIndexTable);
			}
			LOGGER.finest("--> AFTER  Merging - local size:("
					+ mCSVIndexTable.count() + ")");
			mReceivedCSVFile.closeFileOnReading();

			mReceivedCSVFile.delete();
		}
	}

	/*
	 * public void mergeRecvDistrCSVFile(){ if (mDistrCSVIndexTable == null) {
	 * return; } if(mDistrCSVIndexTable.count() > 0){
	 * DBCsvSynchManager.sharedInstance().resetLastCsvUpdateTime(); boolean
	 * inSync =
	 * mDBIndexTable.getSignature().equals(mCSVIndexTable.getSignature());
	 * 
	 * LOGGER.finest("(BEGIN)Received Distri Items into local store. num = "+
	 * mDistrCSVIndexTable.count()); String line = "";
	 * 
	 * boolean isException = false;
	 * 
	 * try { mDistrCSVIndexTable.openFileForReading();
	 * mCSVIndexTable.openFileForWriting(); while((line =
	 * mDistrCSVIndexTable.getNextLine()) != null){
	 * mCSVIndexTable.addIndexEntry(line);
	 * mDBIndexTable.addIndexEntry((IndexEntry)stringToIndexEntry(line)); } }
	 * catch (Exception e) { isException = true; Constants.LOGGER.finest(
	 * "Warning or error in dealing with entries received during distribution.. Will be attempted next time;"
	 * +e.getMessage()); } finally{ mDistrCSVIndexTable.closeFileOnReading();
	 * mCSVIndexTable.closeFileForWriting(); if(!isException)
	 * mDistrCSVIndexTable.empty(); }
	 * LOGGER.finest("(END)Received Distri Items into local store. num = "
	 * +mDistrCSVIndexTable.count());
	 * 
	 * if(inSync){ mCSVIndexTable.setSignature(mDBIndexTable.getSignature()); }
	 * } }
	 */
	public void mergeRecvDistrCSVFile() {
		if (mDistrCSVIndexTable == null) {
			return;
		}
		if (mDistrCSVIndexTable.count() > 0) {
			DBCsvSynchManager.sharedInstance().resetLastCsvUpdateTime();
			boolean inSync = mDBIndexTable.getSignature().equals(
					mCSVIndexTable.getSignature());

			LOGGER.finest("(BEGIN)Received Distri Items into local store. num = "
					+ mDistrCSVIndexTable.count());
			String line = "";

			boolean isException = false;

			try {
				mDistrCSVIndexTable.openFileForReading();
			//	mCSVIndexTable.openFileForWriting();
				while ((line = mDistrCSVIndexTable.getNextLine()) != null) {
				//	mCSVIndexTable.addIndexEntry(line);
					mHBManager
							.addIndexEntry((IndexEntry) stringToIndexEntry(line));
				}
			} catch (Exception e) {
				isException = true;
				e.printStackTrace();
				Constants.LOGGER
						.finest("Warning or error in dealing with entries received during distribution.. Will be attempted next time;"
								+ e.getMessage());
			} finally {
				mDistrCSVIndexTable.closeFileOnReading();
			//	mCSVIndexTable.closeFileForWriting();
				if (!isException)
					mDistrCSVIndexTable.empty();
			}
			LOGGER.finest("(END)Received Distri Items into local store. num = "
					+ mDistrCSVIndexTable.count());

			if (inSync) {
				mCSVIndexTable.setSignature(mDBIndexTable.getSignature());
			}
		}
	}

	/**
	 * Adds a Host to the DB.
	 * 
	 * @param host
	 *            the host to add.
	 * @return The host ID if the host has been inserted, -1 otherwise.
	 */
	private int addHost(String hGuid, String IP, int port) {
		try {
			// check if host already exists
			int id = HostsCacheList.containsKey(hGuid) ? HostsCacheList
					.get(hGuid) : -1;
			if (id >= 0)
				return id;
			Constants.LOGGER.finest("Did not find host :" + hGuid);
			// add new host
			String hostGUID = hGuid;
			String hostAddress = IP;
			int hostPort = port;
			int hostQOS = 0;
			String hostPath = "";
			int hostID = mDBManager.mergeSQL(DBManager.HOSTS_TABLE, "GUID",
					"null,'" + hostGUID + "','" + hostAddress + "'," + hostPort
							+ ",'" + hostQOS + "','" + hostPath + "'," + 0);
			HostsCacheList.put(hGuid, hostID);
			return hostID;
		} finally {
		}
	}

	private long timeTaken(long startTime) {
		return (System.currentTimeMillis() - startTime);
	}

	private p2p.index.IndexEntry stringToIndexEntry(String s){
		
		String[] tkns = s.split(",");
		if(tkns.length != 5){
			System.err.println("stringToIndexEntry(): Null -> " + s);
			new Throwable().printStackTrace();
			return null;
		}
		GUID dGuid = GUID.getGUID(tkns[2]);
		//Type type = (Type) PGridIndexFactory.sharedInstance().getTypeByString(tkns[3]);
		Type type = (Type) PGridIndexFactory.sharedInstance().getTypeByString(TYPE_NAME);
		PGridKey key = new PGridKey(tkns[0]);
		PGridHost host = PGridHost.getHost(tkns[4], tkns[3], String.valueOf(PORT_NUMBER));
		BitVector[] xy = MGridUtils.HilbertInverseConvertor(Long.parseLong(key.toString(), 2));
		Long x= xy[0].toExactLong();
		Long y = xy[0].toExactLong();
		Long id = Long.parseLong(tkns[1]);
		Point point = new Point(x,y,id);
		return IndexManager.getInstance().createIndexEntry(dGuid, type, key, host, point);
	}

	public void addToCSVandDB(Collection items, boolean shouldFilter) {
		try {
			// mCSVIndexTable.openFileForWriting();
			mToDistrCSVIndexTable.openFileForWriting();
			boolean inSync = mDBIndexTable.getSignature().equals(
					mCSVIndexTable.getSignature());

			for (Object obj : items) {
				if (PGridP2P.sharedInstance().isLocalPeerResponsible(
						((IndexEntry) obj).getKey())) {
					mHBManager.addIndexEntry((IndexEntry) obj);
				} else {
					mToDistrCSVIndexTable.addIndexEntry((IndexEntry) obj);
				}
			}

			if (inSync) {
				mCSVIndexTable.setSignature(mDBIndexTable.getSignature());
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// mCSVIndexTable.closeFileForWriting();
			mToDistrCSVIndexTable.closeFileForWriting();
		}
	}

	public void addToCSV(CSVIndexTable csvIndexTable, Collection items) {
		try {
			csvIndexTable.openFileForWriting();
			boolean inSync = mDBIndexTable.getSignature().equals(
					mCSVIndexTable.getSignature());
			for (Object obj : items) {
				csvIndexTable.addIndexEntry((IndexEntry) obj);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			csvIndexTable.closeFileForWriting();
		}
	}

	public void addToCSV(CSVIndexTable csvIndexTable, CSVIndexTable src) {
		try {
			csvIndexTable.openFileForWriting();
			src.openFileForReading();
			boolean inSync = mDBIndexTable.getSignature().equals(
					mCSVIndexTable.getSignature());
			String dataItem = null;
			while ((dataItem = src.getNextLine()) != null) {
				csvIndexTable.addIndexEntry(dataItem);
			}
		} catch (Exception e) {
			System.out.print("inside add to csv");
			e.printStackTrace();
		} finally {
			csvIndexTable.closeFileForWriting();
			src.closeFileOnReading();
		}
	}

	public CSVIndexTable getCurrDistrCSVIndexTable() {
		if (mToDistrCSVIndexTable != null && mToDistrCSVIndexTable.count() != 0) {
			boolean isException = false;
			try {
				mCurrDistrCSVIndexTable = new CSVIndexTable("Curr_"
						+ randomString() + ".csv", false);
				mToDistrCSVIndexTable.openFileForReading();
				mCurrDistrCSVIndexTable.openFileForWriting();
				String line = "";
				while ((line = mToDistrCSVIndexTable.getNextLine()) != null) {
					mCurrDistrCSVIndexTable.addIndexEntry(line);
				}
			} catch (Exception e) {
				isException = true;
				Constants.LOGGER
						.finest("Warning or error in dealing with entries to be distributed.. Will be attempted next time;"
								+ e.getMessage());
			} finally {
				mToDistrCSVIndexTable.closeFileOnReading();
				mCurrDistrCSVIndexTable.closeFileForWriting();
				if (!isException)
					mToDistrCSVIndexTable.empty();
			}

		}
		return mCurrDistrCSVIndexTable;
	}

	String randomString() {
		String s = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		int len = 6;
		char[] c = new char[len];
		Random r = new Random();
		for (int i = 0; i < len; i++) {
			c[i] = s.charAt(r.nextInt(1000) % s.length());
		}
		return new String(c);
	}
}
