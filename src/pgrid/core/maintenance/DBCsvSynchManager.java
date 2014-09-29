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

import java.util.logging.Level;
import java.util.logging.Logger;

import pgrid.interfaces.basic.PGridP2P;
import pgrid.util.WorkerThread;

public class DBCsvSynchManager extends WorkerThread {
	/**
	 * A Constant which indicates the minimum time lapse required since previous useful exchange.
	 */
	private static final long TIME_SINCE_LAST_USEFUL_EXCHANGE = 3*60*1000; //3mins


	private static DBCsvSynchManager SHARED_INSTANCE = null;

	private DBCsvSynchManager(){
		setTimeout(TIME_SINCE_LAST_USEFUL_EXCHANGE/6*2);

	}

	public static DBCsvSynchManager sharedInstance(){
		if(SHARED_INSTANCE == null)
			SHARED_INSTANCE = new DBCsvSynchManager();
		return SHARED_INSTANCE;
	}

	/**
	 * The PGrid.DBCsvSynchManager logger.
	 */
	protected static final Logger LOGGER = Logger.getLogger("PGrid.DBCsvSynchManager");

	@Override
	protected void handleError(Throwable t) {
		if (t instanceof InterruptedException) {
			LOGGER.finer("DBCsvSynchManager interupted.");
		} else {
			LOGGER.log(Level.WARNING, "Error in DBCsvSynchManager thread", t);
		}
	}

	public boolean isSynchronizing(){
		return dbToCsvSynchRequest && (System.currentTimeMillis() - latestTime > TIME_SINCE_LAST_USEFUL_EXCHANGE);
	}
	@Override
	protected boolean isCondition() {
		// TODO Auto-generated method stub
		return dbToCsvSynchRequest && (System.currentTimeMillis() - latestTime > TIME_SINCE_LAST_USEFUL_EXCHANGE);
	}

	@Override
	protected void prepareWorker() throws Exception {
		// TODO Auto-generated method stub
		LOGGER.config("DBCsvSynchManager thread prepared.");

	}

	@Override
	protected void releaseWorker() throws Exception {
		// TODO Auto-generated method stub
		LOGGER.config("DBCsvSynchManager thread released.");

	}

	private boolean dbToCsvSynchRequest = false;
	private long latestTime;

	private Object syncLock = new Object();
	public void setDbToCsvSynchRequest(){
		synchronized (syncLock) {
			dbToCsvSynchRequest =true;
			latestTime = System.currentTimeMillis();
			syncLock.notifyAll();
		}
	}


	long i = 0;
	@Override
	protected void work() throws Exception {
			/*if(isSynchronizing()){
				System.out.println(++i + ": DbCSVSynch Thread Message");
				if(!DbCsvUtils.sharedInstance().isDbUptodateWithCsv()){
					PGridP2P.sharedInstance().getMaintenanceManager().getExchanger().stopExchanger();
					PGridP2P.sharedInstance().getIndexManager().stopDistributor();
					LOGGER.finest("Stopping the Exchanger and Distributor....");
					
					LOGGER.finest("Csv 2 DB Started....");
					long time1 = System.currentTimeMillis();
					DbCsvUtils.sharedInstance().csvToDb();
					long tCSV2DB = timeElapsed(time1);
					LOGGER.finest(tCSV2DB    +"\t : CSV2DB");
					LOGGER.finest("Starting the Exchanger and Distributor....");
					PGridP2P.sharedInstance().getMaintenanceManager().getExchanger().startExchanger();
					PGridP2P.sharedInstance().getIndexManager().startDistributor();
				}

				if(!DbCsvUtils.sharedInstance().isCsvUptodateWithDb()){
					PGridP2P.sharedInstance().getMaintenanceManager().getExchanger().stopExchanger();
					PGridP2P.sharedInstance().getIndexManager().stopDistributor();
					LOGGER.finest("Stopping the Exchanger and Distributor....");
					LOGGER.finest("DB 2 CSV Started....");
					long time1 = System.currentTimeMillis();
					DbCsvUtils.sharedInstance().dbToCsv();
					long tDBtoCSV = timeElapsed(time1);
					LOGGER.finest(tDBtoCSV    +"\t : DBtoCSV");
					LOGGER.finest("Starting the Exchanger and Distributor....");
					PGridP2P.sharedInstance().getMaintenanceManager().getExchanger().startExchanger();
					PGridP2P.sharedInstance().getIndexManager().startDistributor();
				}
				dbToCsvSynchRequest = false;
			}*/
	}

	private long timeElapsed(long t){
		return (System.currentTimeMillis() - t);
	}

	/**
	 * This function should be called whenever some changes are made to the
	 * LocalCSV Entries.
	 */
	public void resetLastCsvUpdateTime(){
		latestTime = System.currentTimeMillis();
	}
	
	public boolean isDbCsvSynchRequested(){
		return dbToCsvSynchRequest;
	}

}
