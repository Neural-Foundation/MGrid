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

import pgrid.Constants;
import pgrid.IndexEntry;
import pgrid.util.Utils;
import pgrid.core.maintenance.DbCsvUtils;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.protocol.IndexModifierCSVMessage;
import pgrid.network.protocol.IndexModifierMessage;
import pgrid.util.logging.LogFormatter;
import pgrid.util.monitoring.MonitoringManager;

import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class distributes inserted, updated, and deleted data items in the network.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */
class Distributor extends pgrid.util.WorkerThread {

	/**
	 * The PGrid.Distributor logger.
	 */
	static final Logger LOGGER = Logger.getLogger("PGrid.Distributor");

	/**
	 * The insert handler.
	 */
	private IndexModifier mIndexModifier = null;

	/**
	 * The insert handler.
	 */
	private IndexModifierCSV mIndexModifierCSV = null;

	/**
	 * The PGridP2P facility.
	 */
	private PGridP2P mPGridP2P = null;

	/**
	 * The remote requests.
	 */
	private final ArrayList mRemoteRequests = new ArrayList();
	/**
	 * The remote requests.
	 */
	private final ArrayList mCSVRemoteRequests = new ArrayList();

	/**
	 * The requests.
	 */
	private final ArrayList mRequests = new ArrayList();

	/**
	 * The requests.
	 */
	private final ArrayList mCSVRequests = new ArrayList();

	/**
	 * The working thread
	 */
	private Thread mThread;

	/**
	 * Lock
	 */
	private final Object mVectorLock = new Object();

	static {
		LogFormatter formatter = new LogFormatter();
		formatter.setDateFormat("HH:mm:ss");
		formatter.setFormatPattern(LogFormatter.DATE + ": " + LogFormatter.MESSAGE + LogFormatter.NEW_LINE + LogFormatter.THROWABLE);
		Constants.initChildLogger(LOGGER, formatter, null);
	}

	/**
	 * Constructs the Distributor.
	 *
	 */
	Distributor() {
		super();
		mPGridP2P = PGridP2P.sharedInstance();
		mIndexModifier = new IndexModifier(this);
		mIndexModifierCSV = new IndexModifierCSV(this);
	}

	/**
	 * Returns the common key for data items of a given level.
	 * @param level the level.
	 * @return the common key.
	 */
	String commonKeyForLevel(int level) {
		String localPath = mPGridP2P.getLocalPath();
		String key;
		if (level == 0)
			key = "";
		else
			key = localPath.substring(0, level);
		if (localPath.charAt(level) == '0')
			key += "1";
		else if (localPath.charAt(level) == '1')
			key += "0";
		return key;
	}

	protected void handleError(Throwable t) {
		if (t instanceof InterruptedException) {
			Constants.LOGGER.finer("Distributor interupted.");
		} else {
			Constants.LOGGER.log(Level.WARNING, "Error in Distributor thread", t);
		}
	}



	protected boolean isCondition() {
		boolean isCondition = true;
		synchronized(mVectorLock) {
			isCondition = mActive && (!mRequests.isEmpty() || !mRemoteRequests.isEmpty() || shouldDistribute() || shouldMerge() || !mCSVRequests.isEmpty() || !mCSVRemoteRequests.isEmpty());
		}
		return isCondition;
	}

	protected void prepareWorker() throws Exception {
		mThread = Thread.currentThread();
		Constants.LOGGER.config("Distributor thread prepared.");
	}

	protected void releaseWorker() throws Exception {
		Constants.LOGGER.config("Distributor thread released.");
	}



	/**
	 * Inserts the given items in the network.
	 *
	 * @param items the items to insert.
	 */
	void insert(Collection items) {
		synchronized(mVectorLock) {
			mRequests.add(new DistributionRequest(IndexModifier.INSERT, items));
		}
		broadcast();
	}

	/**
	 * Inserts the given items in the network.
	 *
	 * @param items the items to insert.
	 */
	void insert(CSVIndexTable csvItems) {
		synchronized(mVectorLock) {
			mCSVRequests.add(new DistributionRequest(IndexModifier.INSERT, csvItems));
		}
		broadcast();
	}

	/**
	 * Inserts the given items in the network.
	 *
	 * @param items the items to insert.
	 */
	void update(Collection items) {
		synchronized(mVectorLock) {
			mRequests.add(new DistributionRequest(IndexModifier.UPDATE, items));
		}
		broadcast();
	}

	/**
	 * Inserts the given items in the network.
	 *
	 * @param items the items to insert.
	 */
	void delete(Collection items) {
		synchronized(mVectorLock) {
			mRequests.add(new DistributionRequest(IndexModifier.DELETE, items));
		}
		broadcast();
	}

	/**
	 * Invoked when a new insert request was received from another host.
	 * @param message the insert request.
	 */
	public void remoteDistribution(IndexModifierMessage message) {
		synchronized(mVectorLock) {
			mRemoteRequests.add(new RemoteDistributionRequest(message.getMode(), message));
		}
		broadcast();
	}
	/**
	 * Invoked when a new insert request was received from another host.
	 * @param message the insert request.
	 */
	public void remoteDistribution(IndexModifierCSVMessage message) {
		synchronized(mVectorLock) {
			mCSVRemoteRequests.add(new RemoteDistributionRequest(message.getMode(), message));
		}
		broadcast();
	}

	/**
	 * Sorts the given date items according to their corresponding level.
	 * @param items the items to sort.
	 * @return an array of data item lists.
	 */
	synchronized ArrayList[] sortByLevel(Collection items) {
		String localPath = mPGridP2P.getLocalPath();
		ArrayList[] level = new ArrayList[localPath.length() + 1];
		if(items != null){
//			Collection items = new ArrayList(itms);
			for (Iterator it = items.iterator(); it.hasNext();) {
				IndexEntry item = (IndexEntry)it.next();
				String comPath = Utils.commonPrefix(localPath, item.getKey().toString());
				if ((comPath.length() == localPath.length()) || (comPath.length() == item.getKey().size())) {
					// data item belongs to local host => inform replicas
					if (level[level.length-1] == null)
						level[level.length-1] = new ArrayList();
					level[level.length-1].add(item);
				} else {
					// data item belongs to other host => inform host of responsible level
					if (level[comPath.length()] == null)
						level[comPath.length()] = new ArrayList();
					level[comPath.length()].add(item);
				}
			}
		}
		return level;
	}

	protected void work() throws Exception {
		// Queries
		ArrayList request = null;
		synchronized (mVectorLock) {
			request = new ArrayList(mRequests);
			mRequests.clear();
		}

		MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_DISTR_INDEX_MODIF_PROCESS_GLOBAL);

		for (Iterator it = request.iterator();it.hasNext();) {

			MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_DISTR_INDEX_MODIF_PROCESS_LOCAL);
			mIndexModifier.process((DistributionRequest)it.next());
			MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_DISTR_INDEX_MODIF_PROCESS_LOCAL);

		}

		MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_DISTR_INDEX_MODIF_PROCESS_GLOBAL);

		// Queries
		ArrayList csvRequest = null;
		synchronized (mVectorLock) {
			csvRequest = new ArrayList(mCSVRequests);
			mCSVRequests.clear();
		}

		MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_DISTR_INDEX_MODIF_PROCESS_GLOBAL);

		for (Iterator it = csvRequest.iterator();it.hasNext();) {

			MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_DISTR_INDEX_MODIF_PROCESS_LOCAL);
			mIndexModifierCSV.processCSV((DistributionRequest)it.next());
			MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_DISTR_INDEX_MODIF_PROCESS_LOCAL);

		}

		MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_DISTR_INDEX_MODIF_PROCESS_GLOBAL);


		ArrayList remoteRequest = null;
		synchronized (mVectorLock) {
			remoteRequest = new ArrayList(mRemoteRequests);
			mRemoteRequests.clear();
		}

		MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_DISTR_INDEX_MODIF_REMOTE_PROCESS_GLOBAL);

		for (Iterator it = remoteRequest.iterator();it.hasNext();){
			MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_DISTR_INDEX_MODIF_REMOTE_PROCESS_LOCAL);
			mIndexModifier.remoteProcess((RemoteDistributionRequest)it.next());
			MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_DISTR_INDEX_MODIF_REMOTE_PROCESS_LOCAL);
		}

		MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_DISTR_INDEX_MODIF_REMOTE_PROCESS_GLOBAL);

		ArrayList csvRemoteRequest = null;
		synchronized (mVectorLock) {
			csvRemoteRequest = new ArrayList(mCSVRemoteRequests);
			mCSVRemoteRequests.clear();
		}

		MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_DISTR_INDEX_MODIF_REMOTE_PROCESS_GLOBAL);

		for (Iterator it = csvRemoteRequest.iterator();it.hasNext();){
			MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_DISTR_INDEX_MODIF_REMOTE_PROCESS_LOCAL);
			mIndexModifierCSV.remoteProcessCSV((RemoteDistributionRequest)it.next());
			MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_DISTR_INDEX_MODIF_REMOTE_PROCESS_LOCAL);
		}

		MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_DISTR_INDEX_MODIF_REMOTE_PROCESS_GLOBAL);

		if(shouldDistribute()){
			distributeData();
		}
		if(shouldMerge()){
			DbCsvUtils.sharedInstance().mergeRecvDistrCSVFile();
		}

	}

	/**
	 * Shutdown the system
	 */
	public void shutdown() {
		if (mThread != null)
			mThread.interrupt();
	}

	/**
	 * 
	 */
	public void distributeData(){
		CSVIndexTable distrIdxTable = DbCsvUtils.sharedInstance().getCurrDistrCSVIndexTable();
		if(distrIdxTable.exists() && distrIdxTable.count()>0){
			LOGGER.finest("Distributing Records Into Network : "+distrIdxTable.count());
			try {
				insert(distrIdxTable);
			} catch (Exception e) {
				e.printStackTrace();
			} finally{
			}
		}
	}

	/**
	 * 
	 */
	boolean shouldDistribute(){
		CSVIndexTable mToDistrCSVIndexTable = IndexManager.getInstance().getToDistrCSVIndexTable();
		if(mToDistrCSVIndexTable!= null &&mToDistrCSVIndexTable.count() != 0){
			return true;
		}
		return false;
	}
	/**
	 * 
	 */
	boolean shouldMerge(){
		CSVIndexTable mDistrCSVIndexTable = IndexManager.getInstance().getDistrCSVIndexTable();
		if(mDistrCSVIndexTable!= null &&mDistrCSVIndexTable.count() != 0){
			return true;
		}
		return false;
	}

	/**
	 * Sorts the given data items according to their corresponding level.
	 * @param items the items to sort.
	 * @return an array of data item lists.
	 */
	synchronized CSVIndexTable[] sortByLevel(CSVIndexTable csvItems) {
		String localPath = mPGridP2P.getLocalPath();
		CSVIndexTable[] csvLevels = new CSVIndexTable[localPath.length() + 1];
		if(csvItems==null || csvItems.count()==0){
			if(csvItems != null) csvItems.delete();
			return csvLevels;
		}
		Constants.LOGGER.finest("sortByLevel of :"+csvItems.getJustFileName() +" : fileSize : "+csvItems.count());
		if(csvItems != null){
			try{
				String preName = csvItems.getJustFileName().split("\\.")[0];
				Constants.LOGGER.finest("PreName : "+preName);
				
				csvItems.openFileForReading();
				String line = "";
				int i = 0;
				while((line = csvItems.getNextLine()) != null){
					String key = line.split(",")[0];
					String comPath = Utils.commonPrefix(localPath, key);
					if ((comPath.length() == localPath.length()) || (comPath.length() == key.length())) {
						// data item belongs to local host => inform replicas
						if (csvLevels[csvLevels.length-1] == null){
							csvLevels[csvLevels.length-1] = new CSVIndexTable("I_"+preName+"_CSV_LEVEL_"+(csvLevels.length-1)+".csv");
							Constants.LOGGER.finest("Creating file : "+"I_"+preName+"_CSV_LEVEL_"+(csvLevels.length-1)+".csv");
							csvLevels[csvLevels.length-1].openFileForWriting();
						}
						csvLevels[csvLevels.length-1].addIndexEntry(line);
					} else {
						// data item belongs to other host => inform host of responsible level
						if (csvLevels[comPath.length()] == null){
							csvLevels[comPath.length()] = new CSVIndexTable("I_"+preName+"_CSV_LEVEL_"+(comPath.length())+".csv");
							Constants.LOGGER.finest("Creating file : "+"I_"+preName+"_CSV_LEVEL_"+(comPath.length())+".csv");
							csvLevels[comPath.length()].openFileForWriting();
						}
						csvLevels[comPath.length()].addIndexEntry(line);
					}
				}
			}catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}finally{
				csvItems.closeFileOnReading();
				for (int i = 0; i < csvLevels.length; i++) {
					if(csvLevels[i] != null) csvLevels[i].closeFileForWriting();
				}
			}
		}
		csvItems.delete(); //Freeing of resources. The input csvFile is sorted into different LevelCSVFiles
		return csvLevels;
	}
	
	/**
	 * Start exchanger
	 */
	public void startDistributor() {
		mActive = true;
	}

	private boolean mActive = true;
	/**
	 * Stop exchanger
	 */
	public void stopDistributor() {
		mActive = false;
	}

}