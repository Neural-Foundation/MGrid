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

import pgrid.Constants;
import pgrid.Exchange;
import pgrid.Properties;
import pgrid.Statistics;
import pgrid.PGridHost;
import pgrid.util.Utils;
import pgrid.core.*;
import pgrid.core.index.DBIndexTable;
import pgrid.core.index.DBView;
import pgrid.core.index.*;
import pgrid.core.index.Signature;
import pgrid.interfaces.basic.PGridP2P;

import java.util.TreeSet;

/**
 * This class represents the PGrid exchange algorithms.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @author @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */
class ExchangeAlgorithmus extends Exchanger {

	/**
	 * The minimum percentage of known replicas before allowing a new split.
	 */
	private static final double CONFIDENT_NUMBER = 0.6;

	/**
	 * The amount of useless exchanges allowed before stopping initiating new exchanges.
	 */
	protected static final int MAX_USELESS_EXCH_COUNT = 9;

	/**
	 * The data item manager.
	 */
	private IndexManager mIndexManager = null;

	/**
	 * The Exchange counter.
	 */
	private int mExchangeCount = 1;

	/**
	 * The PGridP2P facility.
	 */
	private PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 * Counts the amount of useless exchanges (no split, no new data items, etc.)
	 */
	private short mUselessExchCount = 0;

	/**
	 * The estimation of replicas.
	 */
	protected double mReplicaEstimate = 0;

	/**
	 * Creates a new class to process PGrid exchanges.
	 *
	 */
	ExchangeAlgorithmus(MaintenanceManager manager) {
		super();
		mMaintencanceMgr = manager;
		mIndexManager = mPGridP2P.getIndexManager();
	}

	/**
	 * Executes the exchange.
	 *
	 * @param exchange   the exchange to process.
	 * @param invited    indicates the initiator of this exchange.
	 * @param recursion  the recursion.
	 * @param currentLen the current len position.
	 * @param remoteRevision remote revision
	 */
	void process(PGridHost host, Exchange exchange, boolean invited, int recursion, int currentLen, int minStorage, long remoteRevision) {

		if (mPGridP2P.isInTestMode()) {
			int localITSize =  mIndexManager.getPredictionSubset().count();
			int remoteITSize = ((TempDBIndexTable)exchange.getIndexTable()).count();

			LOGGER.fine("start " + mExchangeCount + ". Exchange " + exchange.getGUID() + " (Invited=" + String.valueOf(invited) + ", Recursion=" + exchange.getRecursion() + ") with Host " + exchange.getHost().toHostString());
			LOGGER.fine("Local Host (Path: '" + mPGridP2P.getLocalPath() + "', rev: "+(mPGridP2P.getLocalHost().getRevision())+", Data Items: " + localITSize + ") - Remote Host (Path: '" + exchange.getHost().getPath() + "', rev: "+(remoteRevision-1)+", Data Items: " + (exchange.getIndexTable() != null ? remoteITSize : 0) + ")");
		}

		mPGridP2P.getStatistics().Exchanges++;

		// save some values to compare output of exchange
		String initPath = mPGridP2P.getLocalPath();
		String lInitPath = initPath,lFinalPath = initPath;
		Signature initDataSign = mIndexManager.getPredictionSubset().getSignature();
		if (initDataSign == null) return; //signature should never be null unless P-Grid is shuting down

		XMLRoutingTable routingTable = exchange.getRoutingTable();
		String path = exchange.getHost().getPath();
		String rInitPath = path,rFinalPath=path;
		routingTable.setLocalHost(host);
		DBIndexTable dataTable = (TempDBIndexTable)exchange.getIndexTable();
		if (dataTable == null){
			dataTable = new TempDBIndexTable();
			System.err.println("Exch Rndsubset is NULL");
		}

		
		// construct common path and its length
		String commonPath = Utils.commonPrefix(mPGridP2P.getLocalPath(), path);
		int len = commonPath.length();

		// update statistics
		mPGridP2P.getMaintenanceManager().getBalancer().updateStatistics(len, currentLen, mPGridP2P.getLocalPath().length(), path.length());

		// compute path lengths, union table, and table selections
		int lLen = mPGridP2P.getLocalPath().length() - len;
		int rLen = path.length() - len;

		// P-Grid maintainance activities: Refreshes the routing tables and fidget lists at each peer
		RoutingTable rt=null;
		try {
			rt = (RoutingTable) mPGridP2P.getRoutingTable().clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}

		// localhost
		mPGridP2P.getRoutingTable().refresh(routingTable, len, mPGridP2P.getLocalPath().length(), path.length(),
				mPGridP2P.propertyInteger(Properties.MAX_FIDGETS), mPGridP2P.propertyInteger(Properties.MAX_REFERENCES),
				invited, exchange.getRandomNumber());
		// remote host
		routingTable.refresh(rt, len, path.length(), mPGridP2P.getLocalPath().length(),
				mPGridP2P.propertyInteger(Properties.MAX_FIDGETS), mPGridP2P.propertyInteger(Properties.MAX_REFERENCES),
				!invited, exchange.getRandomNumber());
		if ((lLen > 0) && (rLen > 0)) {
			// Peer's paths are incomplete
			LOGGER.finer("case 1: Peer's paths are incomplete");
			if (PGridP2P.sharedInstance().isInTestMode())
				mPGridP2P.getStatistics().ExchangeCases[Statistics.EXCHANGE_CASE_1]++;
			mPGridP2P.getRoutingTable().addLevel(len, host);

			if (!invited) {
				// try to initiate a recursive exchange if local peer initiated this exchange
				if (recursion < mPGridP2P.propertyInteger(Properties.MAX_RECURSIONS)) {
					LOGGER.finer("initialize a random exchange with one peer of the remote routing table");
					mMaintencanceMgr.randomExchange(routingTable.getLevelVector(len), recursion + 1, len + 1);
				}
			}
		} else if ((lLen == 0) && (rLen == 0)) {
			// Peers have the same paths, path extension possible
			LOGGER.finer("case 2: Peers have the same paths, path extension possible");
			if (PGridP2P.sharedInstance().isInTestMode())
				mPGridP2P.getStatistics().ExchangeCases[Statistics.EXCHANGE_CASE_2]++;

			DBView lData = DBView.union(DBView.selection(mIndexManager.getPredictionSubset(), commonPath + ExchangeAlgUtils.pathExtension(!invited, exchange.getRandomNumber())),DBView.selection(dataTable, commonPath + ExchangeAlgUtils.pathExtension(!invited, exchange.getRandomNumber())));
			float lDataCount = lData.count();
			DBView rData = DBView.union(DBView.selection(mIndexManager.getPredictionSubset(), commonPath + ExchangeAlgUtils.pathExtension(invited, exchange.getRandomNumber())),DBView.selection(dataTable, commonPath + ExchangeAlgUtils.pathExtension(invited, exchange.getRandomNumber())));
			float rDataCount = rData.count();

			
			float lr1 = DBView.selection(mIndexManager.getPredictionSubset(), mPGridP2P.getLocalPath()).count();
			float lr2 = DBView.selection(dataTable, path).count();
			float lrt = DBView.union(DBView.selection(mIndexManager.getPredictionSubset(), mPGridP2P.getLocalPath()),DBView.selection(dataTable, mPGridP2P.getLocalPath())).count();
			float est = ExchangeAlgUtils.estimateN(lr1 + lr2 - lrt, lr1, lr2) / lrt * Constants.REPLICATION_FACTOR;

			// sum up all replicas. Count localhost and remote host as duplicas.
			TreeSet replicas = new TreeSet(mPGridP2P.getRoutingTable().getReplicaVector());
			replicas.addAll(routingTable.getReplicaVector());
			float nrep = replicas.size() +
					(replicas.contains(host)?0:1) +
					(replicas.contains(mPGridP2P.getLocalHost())?0:1);

			mReplicaEstimate = est;

			if (mPGridP2P.isInTestMode()) {
				LOGGER.finer("check: lr1: " + lr1 + ", lr2: " + lr2 + ", lrt: " + lrt + ", est: " + est + " rdtt: "+dataTable.count()+" rep: "+nrep);
				LOGGER.finer("case 2.1 or case 2.2: ld1: " + lDataCount + ", ld2: " + rDataCount + ", est: " + est + " randomNr: " + exchange.getRandomNumber() +
						", M1: " + ExchangeAlgUtils.computeM1(lDataCount, rDataCount, (nrep>mReplicaEstimate?nrep:mReplicaEstimate))+
						", rept: "+(Constants.REPLICATION_FACTOR * CONFIDENT_NUMBER * 2.0)+", minStorage: "+minStorage);
			}

			if (
					(	nrep==0 || nrep > (Constants.REPLICATION_FACTOR + 1.0)	) ||
					(   (  ( (path.length() == 0 && mPGridP2P.getLocalPath().length() == 0) && (lrt >= 2 * minStorage) ) ||
					       ( (path.length() != 0 || mPGridP2P.getLocalPath().length() != 0) && (lrt >= 2 * minStorage) )
					 	)		&& 
					 	( exchange.getRandomNumber() < ExchangeAlgUtils.computeM1(lDataCount, rDataCount, (nrep>mReplicaEstimate?nrep:mReplicaEstimate)) 
					 	)		&& 
					 	( nrep > (Constants.REPLICATION_FACTOR * CONFIDENT_NUMBER * 2.0) 
					 	) 
					 )
				) {
				// case 2.1: Data is exchanged, new level of routing table is added and statistics is reset.
				LOGGER.finer("case 2.1: Data is exchanged, new level of routing table is added and statistics is reset.");
				if (PGridP2P.sharedInstance().isInTestMode())
					mPGridP2P.getStatistics().ExchangeCases[Statistics.EXCHANGE_CASE_2_1]++;

				mPGridP2P.getRoutingTable().acquireWriteLock();
				try {
					mPGridP2P.setLocalPath(commonPath + ExchangeAlgUtils.pathExtension(!invited, exchange.getRandomNumber()));
					path = commonPath + ExchangeAlgUtils.pathExtension(invited, exchange.getRandomNumber());
					mPGridP2P.getRoutingTable().addLevel(len, host);
					mPGridP2P.getRoutingTable().clearReplicas();
					lFinalPath = mPGridP2P.getLocalPath();
					rFinalPath = path;
				} finally{
					mPGridP2P.getRoutingTable().releaseWriteLock();
				}
				mPGridP2P.getMaintenanceManager().getBalancer().resetStatistics();
				routingTable.getLocalHost().setPath(path, remoteRevision);
				routingTable.setLevels(path.length() - 1);
			} else {
				// case 2.2: Replicate data.
				LOGGER.finer("case 2.2: Replicate data if not too many data items. (nReplicas = "+nrep+")");

				if (PGridP2P.sharedInstance().isInTestMode())
					mPGridP2P.getStatistics().ExchangeCases[Statistics.EXCHANGE_CASE_2_2]++;
				mPGridP2P.getRoutingTable().addReplica(host);
				mPGridP2P.getRoutingTable().addReplicas(routingTable.getReplicaVector());
				lFinalPath = mPGridP2P.getLocalPath();
				rFinalPath = lFinalPath;
			}
		} else if ((lLen == 0) && (rLen > 0)) {
			// case 3a: Paths are in prefix relationship, exchange or retraction is possible (remote is longer)");
			LOGGER.finer("case 3a: Paths are in prefix relationship, exchange or retraction is possible (remote is longer)");
			if (mPGridP2P.isInTestMode())
				mPGridP2P.getStatistics().ExchangeCases[Statistics.EXCHANGE_CASE_3a]++;
			String lPath = mPGridP2P.getLocalPath().concat((path.charAt(len) == '0' ? "1" : "0"));
			String lPath2 = mPGridP2P.getLocalPath().concat((path.charAt(len) == '0' ? "0" : "1"));

			//DBView uData = DBView.union(mIndexManager.getPredictionSubset(), dataTable);			
			DBView lData = DBView.selection(mIndexManager.getPredictionSubset(), lPath);
			float lDataCount = lData.count();
			DBView lData2 = DBView.selection(mIndexManager.getPredictionSubset(), lPath2);
			float lDataCount2 = lData2.count();
			
			if (mPGridP2P.isInTestMode())
				LOGGER.finer("case 3a.1 or case 3a.2? - ld1: " + lDataCount + ", ld2: " + lDataCount2 + ", est: " + exchange.getReplicaEstimate() +
					", randomNr: " + exchange.getRandomNumber() + ", M2: " + ExchangeAlgUtils.computeM2(lDataCount, lDataCount2,
					exchange.getReplicaEstimate() * Math.pow(2, rLen - 1)));
			if ((lDataCount > lDataCount2) ||
					((lDataCount <= lDataCount2) &&
							(exchange.getRandomNumber() <= ExchangeAlgUtils.computeM2(lDataCount, lDataCount2, exchange.getReplicaEstimate() * Math.pow(2, rLen - 1))))) {
				// case 3a.1: case where longer path in overpopulated region, then adopt opposite path only with reduced ...
				LOGGER.finer("case 3a.1: Longer path in overpopulated region");
				if (mPGridP2P.isInTestMode())
					mPGridP2P.getStatistics().ExchangeCases[Statistics.EXCHANGE_CASE_3a_1]++;

				mPGridP2P.getRoutingTable().acquireWriteLock();
				try {
					mPGridP2P.setLocalPath(lPath);
					mPGridP2P.getRoutingTable().addLevel(len, host);

					for (int i = len + 1; i < routingTable.getLevelCount(); i++) {
						mPGridP2P.getRoutingTable().addLevel(len, routingTable.getLevelVector(i));
					}
					//mIndexManager.setIndexTable(DBView.selection(uData, lPath));
					mPGridP2P.getMaintenanceManager().getBalancer().resetStatistics();
					mPGridP2P.getRoutingTable().clearReplicas();
					
					lFinalPath = mPGridP2P.getLocalPath();
					rFinalPath = path;
					
				} finally{
					mPGridP2P.getRoutingTable().releaseWriteLock();
				}
				mReplicaEstimate = exchange.getReplicaEstimate() * Math.pow(2, rLen - 1);
			} else {
				// case 3a.2: adopt longer remote path
				LOGGER.finer("case 3a.2: adopt longer remote path.");
				if (mPGridP2P.isInTestMode())
					mPGridP2P.getStatistics().ExchangeCases[Statistics.EXCHANGE_CASE_3a_2]++;

				mPGridP2P.getRoutingTable().acquireWriteLock();
				try {
					mPGridP2P.setLocalPath(lPath2);
					mPGridP2P.getRoutingTable().setLevel(len, routingTable.getLevel(len));
				} finally{
					mPGridP2P.getRoutingTable().releaseWriteLock();
				}
				if (rLen == 1  && Constants.REPLICATION_FACTOR!= 0) {
					mPGridP2P.getRoutingTable().addReplica(host);
					mPGridP2P.getRoutingTable().addReplicas(routingTable.getReplicaVector());
				}
				mReplicaEstimate = exchange.getReplicaEstimate() * Math.pow(2, rLen - 1);
				
				lFinalPath = mPGridP2P.getLocalPath();
				rFinalPath = path;
			}
		} else if ((rLen == 0) && (lLen > 0)) {
			// case 3b: case where longer path in overpopulated region, then adopt opposite path only with reduced ...
			LOGGER.finer("case 3b: Paths are in prefix relationship, exchange or retraction is possible (local is longer)");
			if (mPGridP2P.isInTestMode())
				mPGridP2P.getStatistics().ExchangeCases[Statistics.EXCHANGE_CASE_3b]++;
			String rPath = path.concat((mPGridP2P.getLocalPath().charAt(len) == '0' ? "1" : "0"));
			String rPath2 = path.concat((mPGridP2P.getLocalPath().charAt(len) == '0' ? "0" : "1"));

			DBView uData = DBView.union(mIndexManager.getPredictionSubset(), dataTable);
			DBView rData = DBView.selection(dataTable, rPath);
			DBView rData2 = DBView.selection(dataTable, rPath2);

			float rDataCount = rData.count();
			float rDataCount2 = rData2.count();

			if (mPGridP2P.isInTestMode())
				LOGGER.finer("case 3b.1 or case 3b.2? - ld1: " + rDataCount + ", ld2: " + rDataCount2 + ", est: " + mReplicaEstimate +
					", randomNr: " + exchange.getRandomNumber() + ", M2: " + ExchangeAlgUtils.computeM2(rDataCount, rDataCount2,
					mReplicaEstimate * Math.pow(2, lLen - 1)));

			if ((rDataCount > rDataCount2) ||
					((rDataCount <= rDataCount2) &&
							(exchange.getRandomNumber() <= ExchangeAlgUtils.computeM2(rDataCount, rDataCount2, mReplicaEstimate * Math.pow(2, lLen - 1))))) {
				// case 3b.1: Path extension to complimentary bit at level len+1 if too much data");
				LOGGER.finer("case 3b.1: Path extension to complimentary bit at level len+1 if too much data.");
				if (mPGridP2P.isInTestMode())
					mPGridP2P.getStatistics().ExchangeCases[Statistics.EXCHANGE_CASE_3b_1]++;
				path = rPath;
				mPGridP2P.getRoutingTable().addLevel(len, host);

				mPGridP2P.getMaintenanceManager().getBalancer().resetStatistics();
				
				lFinalPath = mPGridP2P.getLocalPath();
				rFinalPath = path;
				
			} else {
				// case 3b.2: adopt longer remote path
				LOGGER.finer("case 3b.2: adopt longer remote path.");
				if (mPGridP2P.isInTestMode())
					mPGridP2P.getStatistics().ExchangeCases[Statistics.EXCHANGE_CASE_3b_2]++;
				path = rPath2;

				if (lLen == 1 && Constants.REPLICATION_FACTOR!= 0) {
					mPGridP2P.getRoutingTable().addReplica(host);
				}

				lFinalPath = mPGridP2P.getLocalPath();
				rFinalPath = path;
			}
			routingTable.getLocalHost().setPath(path, remoteRevision);
			
		}
		
		LOGGER.finest("*************************************************************");
		LOGGER.finest("L("+mPGridP2P.getLocalHost().toHostString()+") : ("+lInitPath+")\t--->\t ("+lFinalPath+")");
		LOGGER.finest("R("+host.toHostString()+") : ("+rInitPath+")\t--->\t ("+rFinalPath+")");
		LOGGER.finest("*************************************************************");
		
		mExchangeCount++;
		Signature signature = mIndexManager.getPredictionSubset().getSignature();
		LOGGER.fine("Local Host (Path: '" + mPGridP2P.getLocalPath() + "', rev: "+mPGridP2P.getLocalHost().getRevision()+", Data Items: " + mIndexManager.getPredictionSubset().count() + "("+mIndexManager.getPredictionSubset().getTableName()+","+mIndexManager.getPredictionSubset().getClass().getSimpleName()+")) - " +
				"Remote Host (Path: '" + path + "', rev: "+host.getRevision()+", Data Items: " + dataTable.count() +  "("+dataTable.getTableName()+","+dataTable.getClass().getSimpleName()+")"  + ") - old signature: "+initDataSign+" new signature: "+signature);

		Constants.LOGGER.finest("Here___________________AAAA");
		// stop initiating exchanges if no usefull exchanges were performed for a while
		if (signature == null) return ;
		Constants.LOGGER.finest("Here___________________BBBB");

		if (Constants.REGULATE_EXCHANGE) {
			if (mPGridP2P.getLocalPath().equals(initPath)) {

				// peer is inviting and it was not incompatible with remote peer
				if (!invited) {
					Constants.LOGGER.finest("Incrementing USELESS Exchange Count :"+mUselessExchCount);
					if (++mUselessExchCount >= MAX_USELESS_EXCH_COUNT) {
						if (mPGridP2P.propertyBoolean(Properties.INIT_EXCHANGES) == true) {
							LOGGER.fine("Stop initiating exchanges.");
							// TODO fix me
							mPGridP2P.setInitExchanges(false);
						}
					}
				}
			} else {
				mUselessExchCount = 0;
				if (mPGridP2P.propertyBoolean(Properties.INIT_EXCHANGES) == false) {
					LOGGER.fine("Restart initiating exchanges.");
					// TODO fix me
					mPGridP2P.setInitExchanges(true);
				}
			}
		}

		// run the Replication Balancer if activated
		if (mPGridP2P.propertyBoolean(Properties.REPLICATION_BALANCE))
			mPGridP2P.getMaintenanceManager().replicationBalance();

		// save the routing table and the data table
		mPGridP2P.getRoutingTable().save();
		if (PGridP2P.sharedInstance().isInTestMode()) {
			mPGridP2P.getStatistics().PathLength = mPGridP2P.getRoutingTable().getLevelCount();
			mPGridP2P.getStatistics().Replicas = mPGridP2P.getRoutingTable().getReplicaVector().size();
			mPGridP2P.getStatistics().DataItemsManaged = mPGridP2P.getIndexManager().getPredictionSubset().count();
			mPGridP2P.getStatistics().DataItemsPath = DBView.selection(mIndexManager.getPredictionSubset(),mPGridP2P.getLocalHost().getPath()).count();
			
		}
		return;

	}

	/**
	 * Returns the replicas estimate
	 * @return the replicas estimate
	 */
	public double getReplicaEstimate() {
		return mReplicaEstimate;
	}

}