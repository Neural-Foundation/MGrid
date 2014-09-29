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
import pgrid.PGridHost;
import pgrid.core.index.CSVIndexTable;
import pgrid.core.index.DBView;
import pgrid.core.index.IndexManager;
import pgrid.core.index.Signature;
import pgrid.core.index.DBIndexTable;
import pgrid.core.index.TempDBIndexTable;
import pgrid.core.index.TransferDBIndexTable;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.MessageManager;
import pgrid.network.router.MessageWaiter;
import pgrid.network.protocol.*;
import pgrid.util.logging.FlushedStreamHandler;
import pgrid.util.logging.LogFormatter;
import pgrid.util.monitoring.MonitoringManager;
import pgrid.util.Compression;
import pgrid.util.Utils;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import p2p.basic.GUID;
import p2p.index.IndexEntry;

/**
 * This class processes the PGridP2P Exchange between two hosts.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @author <a href="mailto:SurenderReddy Yerva <surenderreddy.yerva@epfl.ch>">Surender Reddy Yerva</a>
 * @version 2.0.0
 */
class Exchanger extends pgrid.util.WorkerThread implements MessageWaiter {

	/**
	 * A Constant which indicates the minimum time lapse required since previous useful exchange.
	 */
	private static final long TIME_SINCE_LAST_USEFUL_EXCHANGE = 3*60*1000; //5mins
	
	private static final long EI_TIMEOUT = 10*60*1000; //10mins
	
	private static boolean IS_SYNCH_PENDING = false;

	private long lastUsefulExchangeTime = 0;

	/**
	 * The time to wait for no further exchange.
	 */
	private static final int ATTEMPT_TIMEOUT = 3*30000; // 30 sec.

	/**
	 * The time to wait for no further exchange.
	 */
	private static final int READ_ATTEMPT_TIMEOUT = 60*60*1000; // 60 min.

	/**
	 * Maximum number of index entries per message.
	 */
	private static final int MAX_ENTRIES_PER_MSG = 100000;

	/**
	 * The PGrid.Exchanger logger.
	 */
	protected static final Logger LOGGER = Logger.getLogger("PGrid.Exchanger");

	/**
	 * Exchange Logger for debug purposes
	 *
	 */
	private void initExchLog(){
		String logFile = "Exchange.log";
		String LOG_DIR = Constants.LOG_DIR;
		if (LOG_DIR.length() > 0)
			new File(LOG_DIR).mkdirs();
		try {
			FileHandler fHandler = new FileHandler(LOG_DIR + logFile);
			LogFormatter formatter = new LogFormatter();
			formatter.setDateFormat("HH:mm:ss");
			formatter.setFormatPattern(PGridP2P.sharedInstance().getLocalHost().getPort()+": "+LogFormatter.DEBUG_PATTERN);

			fHandler.setFormatter(formatter);
			LOGGER.addHandler(fHandler);
			StreamHandler eHandler = new FlushedStreamHandler(System.err, formatter);
			eHandler.setLevel(Level.WARNING);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Could not use logging file '" + LOG_DIR + logFile + "'!", e);
		} catch (SecurityException e) {
			LOGGER.log(Level.WARNING, "Could not use logging file '" + LOG_DIR + logFile + "'!", e);
		}
	}

	/**
	 * Min storage before spliting
	 */
	protected int mMinStorageEstimate = 0;

	/**
	 * The data item manager.
	 */
	private IndexManager mIndexManager = null;


	/**
	 * If true, the exchanger will process exchange messages
	 */
	private boolean mActive = true;

	/**
	 * The exchange algorithmus processing the exchanges.
	 */
	private ExchangeAlgorithmus mExchangeAlg = null;

	/**
	 * The exchange requests.
	 */
	private final List<ExchangeRequest> mExchangeRequests = new ArrayList<ExchangeRequest>();

	/**
	 * The exchange index entries message.
	 */
	private final ArrayList mEntriesMsg = new ArrayList();

	/**
	 * The exchange csv index entries message.
	 */
	private final ArrayList<ExchangeCSVIndexEntriesMessage> mCsvIeEntriesMsg = new ArrayList<ExchangeCSVIndexEntriesMessage>();

//	/**
//	 * The exchange invitation requests.
//	 */
//	private final ArrayList mExchangeInvitations = new ArrayList();
	
	/**
	 * The exchange invitation requests map(host,EI pair).
	 */
	private final Map<GUID, ExchangeInvitationRequest> mEImap = new HashMap<GUID, ExchangeInvitationRequest>();

	/**
	 * The exchange replies.
	 */
	private final ArrayList mExchangeReplies = new ArrayList();

	/**
	 * If the exchanger thread is idle or not.
	 */
	private boolean mIdleFlag = true;

	/**
	 * The Maintencance Manager.
	 */
	protected MaintenanceManager mMaintencanceMgr = null;

	/**
	 * The Message Manager.
	 */
	private MessageManager mMsgMgr = MessageManager.sharedInstance();

	/**
	 * The PGridP2P facility.
	 */
	private PGridP2P mPGridP2P = null;

	/**
	 * Shutdown variable
	 */
	private boolean mShutdown = false;


	/**
	 * Lock
	 */
	private final Object mExchangeLock = new Object();

	/**
	 * Reference to the worker thread
	 */
	private Thread mThread = null;

	/**
	 * Lock
	 */
	private final Object mRequestLock = new Object();

	/**
	 * The secure random number generator.
	 */
	private SecureRandom mRandomizer = new SecureRandom();

	/**
	 * Total index entries messages expected for the current exchanger
	 */
	private int mTotal = Integer.MAX_VALUE;

	/**
	 * Holds all unseen ACKs.
	 */
	final private ArrayList<GUID> mACKMsg = new ArrayList<GUID>();

	/**
	 * Holds all unseen NACKs. /**Equivalent to No Acknowledgements
	 */
	final private ArrayList<GUID> mNACKMsg = new ArrayList<GUID>();


	static {
		LogFormatter formatter = new LogFormatter();
		formatter.setDateFormat("HH:mm:ss");
		formatter.setFormatPattern(
				LogFormatter.DATE + ": " + LogFormatter.MESSAGE + LogFormatter.NEW_LINE + LogFormatter.THROWABLE);
		Constants.initChildLogger(LOGGER, formatter, null);
	}

	/**
	 * Creates a new router.
	 */
	protected Exchanger() {

	}

	/**
	 * Creates a new router.
	 *
	 * @param p2p the P2P facility.
	 * @param maintenanceMgr the Maintenance Manager.
	 */
	Exchanger(PGridP2P p2p, MaintenanceManager maintenanceMgr) {
		super();
		mPGridP2P = p2p;
		mIndexManager = mPGridP2P.getIndexManager();
		mMaintencanceMgr = maintenanceMgr;
		mExchangeAlg = new ExchangeAlgorithmus(maintenanceMgr);		
		initExchLog();
		
		mDBCsvSynchManager = DBCsvSynchManager.sharedInstance();
		synchThread = new Thread(mDBCsvSynchManager,"SynchManager");
		synchThread.start();
		
		
	}
	
	private DBCsvSynchManager mDBCsvSynchManager = null;
	private Thread synchThread = null;

	protected void handleError(Throwable t) {
		if (t instanceof InterruptedException) {
			LOGGER.finer("Exchanger interupted.");
		} else {
			LOGGER.log(Level.WARNING, "Error in Exchanger thread", t);
		}
	}

	private boolean handleExchangeInvitation(ExchangeInvitationRequest request) {
		// check if the invitation is recent enough
		if ((request.getStartTime() + EI_TIMEOUT/2) < System.currentTimeMillis()) {
			Constants.LOGGER.finer("Exchange invitation  ["+request.getExchangeInvitation().getGUID()+"] from " + request.getExchangeInvitation().getHeader().getHost().toHostString() + " ignored (invite timeout)!");
			mPGridP2P.getStatistics().ExchangesIgnored++;
			return false;
		}
		// check if the invitation is allowed
		if (!request.getExchangeInvitation().getHeader().getHost().isExchangeTime()) {
			Constants.LOGGER.finer("Exchange invitation ["+request.getExchangeInvitation().getGUID()+"] from " + request.getExchangeInvitation().getHeader().getHost().toHostString() + " ignored (too early)!");
			mPGridP2P.getStatistics().ExchangesIgnored++;
			return false;

		}
		LOGGER.finest("EXCHANGE Begins ... handleExchInv..."+mPGridP2P.getLocalHost().toHostString()+"<-->"+request.getExchangeInvitation().getHeader().getHost().toHostString());
		isCSVExchangeProcessActive = true;
		tBegin = System.currentTimeMillis();

		double rnd = mRandomizer.nextDouble();
		long time = System.currentTimeMillis();

		// send exchange message
		sendExchangeMessage(request, rnd);
		tSendExch = timeElapsed(time);
		time = System.currentTimeMillis();


		// block until the return message was received or timeout is raised
		ExchangeReplyMessage exchReply = waitForReply(request);
		tWaitReply = timeElapsed(time);

		boolean found = (exchReply != null);

		if (found) {
			sendAck(exchReply.getGUID(),exchReply.getHeader().getHost());

			// duplicate local data table if the signatures are equal
			Signature sign = mIndexManager.getPeerIndexTableSignature(exchReply.getExchange().getHost());

			if (sign != null && sign.equals(mIndexManager.getIndexTable().getSignature()) &&
					mPGridP2P.getLocalPath().equals(exchReply.getExchange().getHost().getPath()) &&
					exchReply.getExchange().getIndexTable().count() == 0) {


				MonitoringManager.sharedInstance().startTimer(
						pgrid.util.monitoring.Constants.AT_EXCH_INVIT_DUPLICATE_PREDICTION_SUBSET);

				mIndexManager.getPredictionSubset().duplicate(exchReply.getExchange().getIndexTable());

				MonitoringManager.sharedInstance().stopTimer(
						pgrid.util.monitoring.Constants.AT_EXCH_INVIT_DUPLICATE_PREDICTION_SUBSET);

			}
			// set the random number
			exchReply.getExchange().setRandomNumber(rnd);
			// execute the exchange algorithmus

			// recompute our min storage
			mMinStorageEstimate = (mMinStorageEstimate+exchReply.getExchange().getMinStorage())/2;
			mPGridP2P.setProperty(Properties.EXCHANGE_MIN_STORAGE, ""+mMinStorageEstimate);
			String curPath = mPGridP2P.getLocalPath();
			String oldRPath = exchReply.getExchange().getRoutingTable().getLocalHost().getPath();
			MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_EXCH_INVIT_EXCH_PROCESS);
			time = System.currentTimeMillis();
			mExchangeAlg.process(exchReply.getExchange().getHost(), exchReply.getExchange(), true,
					exchReply.getExchange().getRecursion(), exchReply.getExchange().getLenCurrent(), mMinStorageEstimate,
					exchReply.getExchange().getHost().getRevision()+1);

			tExchAlg = timeElapsed(time);
			MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_EXCH_INVIT_EXCH_PROCESS);
			String rCurPath = exchReply.getExchange().getHost().getPath();
			MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_EXCH_INVIT_DELETE_INDEX_TABLE);
			exchReply.getExchange().getIndexTable().delete();
			MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_EXCH_INVIT_DELETE_INDEX_TABLE);
			if(PGridP2P.sharedInstance().isInTestMode()){
				mPGridP2P.getStatistics().MinStorage = mMinStorageEstimate;
			}

			LOGGER.finest("oldLPath,LPath :"+curPath+"-->"+mPGridP2P.getLocalPath());
			LOGGER.finest("oldRPath,rPath :"+oldRPath+"-->"+rCurPath);
			if ((sign == null || !mIndexManager.getIndexTable().getSignature().equals(sign) || !mPGridP2P.getLocalPath().equals(curPath)  || !oldRPath.equals(rCurPath))
					&& 	!(mPGridP2P.getLocalPath().equals(curPath) && oldRPath.equals(rCurPath) && !curPath.equals(rCurPath)) ){
				
				//Useful Exchange
				DBCsvSynchManager.sharedInstance().resetLastCsvUpdateTime();

				if(mPGridP2P.getLocalPath().equals(curPath)){
					if(sign == null){
						LOGGER.finest("Remote Peer signature is NULL");
					}
					if(!mIndexManager.getIndexTable().getSignature().equals(sign)){
						LOGGER.finest("Remote Peer signature is DIFFERENT from the LocalPeer Signature");
					}
				}

				MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_EXCH_INVIT_UPLOAD_DATA);

				uploadData(exchReply.getExchange().getGUID(),
						exchReply.getExchange().getHost(),
						mIndexManager.getIndexTable(),
						exchReply.getExchange().getRoutingTable().getLocalHost().getPath(), rCurPath,
						curPath,
						mPGridP2P.getLocalPath());

				MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_EXCH_INVIT_UPLOAD_DATA);
				long upload = (System.currentTimeMillis()-time);
				waitForCSVExchangeProcessToComplete(upload);
				// invitation accepted => set the next possible exchange time
				request.getExchangeInvitation().getHeader().getHost().invited();
				exchInvTimeHandle.put(request.getExchangeInvitation().getHeader().getHost().toHostString(), System.currentTimeMillis());
				//Delete only on useful exchange
				mIndexManager.getPredictionSubset().delete();
				mIndexManager.getPredictionSubsetCSV().delete();
				//Useful Exchange
				DBCsvSynchManager.sharedInstance().resetLastCsvUpdateTime();
			} else {
				LOGGER.finest("EXCHANGE Ends(Inv) ... WITHOUT UPLOAD..."+mPGridP2P.getLocalHost().toHostString()+"<-->"+request.getExchangeInvitation().getHeader().getHost().toHostString());
				isCSVExchangeProcessActive = false;
			}
			// save new remote signature
			mIndexManager.setPeerIndexTableSignature(exchReply.getExchange().getHost(), mIndexManager.getIndexTable().getSignature());

			time = System.currentTimeMillis();
			return true;
		}

		LOGGER.finest("EXCHANGE Ends(Inv) ... WITHOUT UPLOAD(II)..."+mPGridP2P.getLocalHost().toHostString()+"<-->"+request.getExchangeInvitation().getHeader().getHost().toHostString());
		LOGGER.fine("Exchange ["+request.getExchangeInvitation().getGUID()+"] with host " + request.getExchangeInvitation().getHeader().getHost().toHostString() + " failed (timeout).");
		if (PGridP2P.sharedInstance().isInTestMode())
			mPGridP2P.getStatistics().ExchangesFailed++;
		return false;
	}

	private void sendExchangeMessage(ExchangeInvitationRequest request, double rnd) {

		boolean splitted = true;
		PGridHost host = request.getExchangeInvitation().getHeader().getHost();

		// create the data table according to the path of the remote host
		DBIndexTable dataItems = mIndexManager.getPredictionSubset();
		CSVIndexTable csvDataItems = mIndexManager.getPredictionSubsetCSV();
		// create the message sent to the remote host
		ExchangeMessage msg = new ExchangeMessage(request.getExchangeInvitation().getGUID(), mPGridP2P.getLocalHost(),
				request.getExchangeInvitation().getRecursion(),	request.getExchangeInvitation().getCurrentLen(), mMinStorageEstimate,
				mExchangeAlg.getReplicaEstimate(),	mPGridP2P.getRoutingTable(), /*dataItems*/csvDataItems, mPGridP2P.getIndexManager().getIndexTableSignature(), splitted);
		msg.getExchange().setRandomNumber(rnd);
		mMsgMgr.sendMessage(host, msg, this, request.getExchangeInvitation().getGUID());
		LOGGER.finest("Sending ExchangeRequest Message to "+host.toHostString());
	}

	private ExchangeReplyMessage waitForReply(ExchangeInvitationRequest request) {
		GUID guid = request.getExchangeInvitation().getGUID();
		int THREAD_SLEEP_TIME = 2000; //msec
		LOGGER.finest("Waiting for ExchangeReply ");

		ExchangeReplyMessage exchReply = null;
		boolean found = false;

		long timeout = System.currentTimeMillis() + ATTEMPT_TIMEOUT;

		// get exchange reply message
		while (true) {
			long sleepTime = timeout - System.currentTimeMillis();
			if (sleepTime <= 0){
				exchReply = null;
				LOGGER.finest("ExchangeReply Not Received from ("+request.getExchangeInvitation().getHeader().getHost().toHostString()+").TIME_WASTED :"+ATTEMPT_TIMEOUT);
				break;
			}
			/**
			 * If elapsed time is greater than 10secs, we send a NACK --> thus avoiding the wait on the remote peer
			 */
			if(sleepTime >= 10000){
				if(requests != null){
					if(requests.size() > 0){
						ExchangeRequest req = requests.remove(0);
						sendNAck(req.getExchange().getGUID(),req.getExchange().getHost());
					}
				}
			}
			Object mExchangeReply = null;
			synchronized (mRequestLock) {
				for (Iterator it = mExchangeReplies.iterator(); it.hasNext();) {
					mExchangeReply=it.next();
					exchReply = (ExchangeReplyMessage) mExchangeReply;
					if (exchReply.getGUID().equals(guid)) {
						found = true;
						LOGGER.finest("ExchangeReply Received");
						break;
					}
				}
			}
			synchronized (mACKMsg) {
				if (mNACKMsg.contains(guid)) {
					mNACKMsg.remove(guid);
					LOGGER.finest("NACK Received (time not waste)");
					return null;
				}
			}
			if (!found) {
				try {
					Thread.currentThread().sleep(THREAD_SLEEP_TIME);
				} catch (InterruptedException e) {
					return null;
				}
			} else {
				synchronized (mRequestLock) {
					mExchangeReplies.remove(exchReply);
				}
				break;
			}
		}
		return exchReply;
	}

	private void handleExchangeRequest(ExchangeRequest request) {
		LOGGER.finest("EXCHANGE Begins ... handleExchReq..."+mPGridP2P.getLocalHost().toHostString()+"<-->"+request.getExchange().getHost().toHostString());


		// create a working copy of the data table
		// todo: clone local data item to avoid modification from insert, update or delete messages during exchange

		// check if the exchange is recent enough
		if ((request.getStartTime() + ATTEMPT_TIMEOUT/2) < System.currentTimeMillis()) {
			Constants.LOGGER.finer("Exchange  ["+request.getExchange().getGUID()+"] from " + request.getExchange().getHost().toHostString() + " ignored (exchange timeout)!");
			mPGridP2P.getStatistics().ExchangesIgnored++;
			sendNAck(request.getExchange().getGUID(),request.getExchange().getHost());
			return;
		}
		
		//Sending a NACK if the current node is busy synchronizing
		if(mDBCsvSynchManager.isSynchronizing()){
			sendNAck(request.getExchange().getGUID(),request.getExchange().getHost());
			return;
		}
		
		isCSVExchangeProcessActive = true;
		tBegin = System.currentTimeMillis();

		long time = System.currentTimeMillis();

		MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_EXCH_REQ_SEND_EXCH_REPLY);
		// send exchange reply message
		sendExchangeReplyMessage(request);
		tSendExchReply = timeElapsed(time);
		time = System.currentTimeMillis();
		MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_EXCH_REQ_SEND_EXCH_REPLY);


		MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_EXCH_REQ_WAIT_ACK);
		// wait for the acknowledgement
		if (!waitForAck(request)) {
			LOGGER.fine("Exchange ["+request.getExchange().getGUID()+"] with host " + request.getExchange().getHost().toHostString() + " failed (timeout on acknowledgement).");
			if (PGridP2P.sharedInstance().isInDebugMode())
				LOGGER.finer("Current local path: " + mPGridP2P.getLocalPath() + ".");
			return;
		} else {
			LOGGER.finer("Exchange reply  ["+request.getExchange().getGUID()+"] received at host " + request.getExchange().getHost().toHostString() + ".");
		}
		MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_EXCH_REQ_WAIT_ACK);
		tWaitAck = timeElapsed(time);
		time = System.currentTimeMillis();


		MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_EXCH_REQ_INDEX_SIGNATURE);
		// duplicate local data table if the signatures are equal
		Signature sign = mIndexManager.getPeerIndexTableSignature(request.getExchange().getHost());
		MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_EXCH_REQ_INDEX_SIGNATURE);


		MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_EXCH_REQ_DUPLICATE_DATA_TABLE);

		if (sign != null && sign.equals(mIndexManager.getIndexTable().getSignature()) &&
				mPGridP2P.getLocalPath().equals(request.getExchange().getHost().getPath()) && request.getExchange().getIndexTable().count() == 0) {
			mIndexManager.getPredictionSubset().duplicate(request.getExchange().getIndexTable());
		}
		MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_EXCH_REQ_DUPLICATE_DATA_TABLE);


		// execute the exchange algorithmus
		// recompute our min storage
		mMinStorageEstimate = (mMinStorageEstimate+request.getExchange().getMinStorage())/2;
		String curPath = mPGridP2P.getLocalPath();
		String oldRPath = request.getExchange().getRoutingTable().getLocalHost().getPath();

		mPGridP2P.setProperty(Properties.EXCHANGE_MIN_STORAGE, ""+mMinStorageEstimate);

		MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_EXCH_REQ_EXCH_PROCESS);

		time = System.currentTimeMillis();
		mExchangeAlg.process(request.getExchange().getHost(), request.getExchange(), false, request.getExchange().getRecursion(),
				request.getExchange().getLenCurrent(), mMinStorageEstimate,
				request.getExchange().getHost().getRevision()+1);
		tExchAlg = timeElapsed(time);

		MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_EXCH_REQ_EXCH_PROCESS);
		String rCurPath = request.getExchange().getHost().getPath();

		request.getExchange().getIndexTable().delete();
		//mIndexManager.compactDB();

		if(PGridP2P.sharedInstance().isInTestMode()){
			mPGridP2P.getStatistics().MinStorage = mMinStorageEstimate;
		}

		LOGGER.finest("oldLPath,LPath :"+curPath+"-->"+mPGridP2P.getLocalPath());
		LOGGER.finest("oldRPath,rPath :"+oldRPath+"-->"+rCurPath);

		if ((sign == null || !mIndexManager.getIndexTable().getSignature().equals(sign) || !mPGridP2P.getLocalPath().equals(curPath) || !oldRPath.equals(rCurPath))
				&& 	!(mPGridP2P.getLocalPath().equals(curPath) && oldRPath.equals(rCurPath) && !curPath.equals(rCurPath))) {
			
			//Useful Exchange
			DBCsvSynchManager.sharedInstance().resetLastCsvUpdateTime();
			
			if(mPGridP2P.getLocalPath().equals(curPath)){
				if(sign == null){
					LOGGER.finest("Remote Peer signature is NULL");
				}
				if(!mIndexManager.getIndexTable().getSignature().equals(sign)){
					LOGGER.finest("Remote Peer signature is DIFFERENT from the LocalPeer Signature");
				}
			}

			MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_EXCH_REQ_UPLOAD_DATA);

			uploadData(request.getExchange().getGUID(),
					request.getExchange().getHost(),
					mIndexManager.getIndexTable(),
					request.getExchange().getRoutingTable().getLocalHost().getPath(), rCurPath,
					curPath, mPGridP2P.getLocalPath());

			MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_EXCH_REQ_UPLOAD_DATA);

			long upload = (System.currentTimeMillis()-time);


			MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_EXCH_REQ_READ_INDEX);

			// read index entries
//			readIndexEntries(request.getExchange(), upload);
			waitForCSVExchangeProcessToComplete(upload);

			MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_EXCH_REQ_READ_INDEX);

			//}

			//Delete only on useful exchange
			mIndexManager.getPredictionSubset().delete();
			mIndexManager.getPredictionSubsetCSV().delete();

			//Useful Exchange
			DBCsvSynchManager.sharedInstance().resetLastCsvUpdateTime();

		} else {
//			mMsgMgr.sendMessage(request.getExchange().getHost(), new ExchangeIndexEntriesMessage((pgrid.GUID) request.getExchange().getGUID()), null);
			LOGGER.finest("EXCHANGE Ends ... WITHOUT UPLOAD..."+mPGridP2P.getLocalHost().toHostString()+"<-->"+request.getExchange().getHost().toHostString());
			isCSVExchangeProcessActive = false;

		}
		// save remote signature
		mIndexManager.setPeerIndexTableSignature(request.getExchange().getHost(), mIndexManager.getIndexTable().getSignature());

		request.getExchange().getHost().exchanged();
//		LOGGER.finer("Exchange took: "+(System.currentTimeMillis()-time)+"ms");

	}

	private void sendExchangeReplyMessage(ExchangeRequest request) {
		boolean splitted = true;
		PGridHost host = request.getExchange().getHost();

//		// create the data table according to the path of the remote host
//		DBIndexTable dataItems = compileDataTable(mIndexManager.getPredictionSubset(), request.getExchange().getHost().getPath(),
//		request.getExchange().getIndexTable().getSignature(),mIndexManager.getPeerIndexTableSignature(request.getExchange().getHost()));

		DBIndexTable dataItems = mIndexManager.getPredictionSubset();
		CSVIndexTable csvDataItems = mIndexManager.getPredictionSubsetCSV();

		// create the message sent to the remote host
		ExchangeReplyMessage msg = new ExchangeReplyMessage(request.getExchange().getGUID(), mPGridP2P.getLocalHost(),
				request.getExchange().getRecursion(), request.getExchange().getLenCurrent(), mMinStorageEstimate, mExchangeAlg.getReplicaEstimate(),
				mPGridP2P.getRoutingTable(), csvDataItems/*dataItems*/, mPGridP2P.getIndexManager().getIndexTableSignature(), splitted);

		mMsgMgr.sendMessage(host, msg, this, request.getExchange().getGUID());
		LOGGER.finest("Sending ExchangeReply to "+host.toHostString());


//		dataItems.delete();
	}

	private TempDBIndexTable compileDataTable(DBIndexTable table, String path, Signature sign, Signature oldSign) {
		TempDBIndexTable col = new TempDBIndexTable();

		// is the other host a replica?
		if (path.equals(mPGridP2P.getLocalPath())) {
			if (PGridP2P.sharedInstance().isInTestMode())
				mPGridP2P.getStatistics().ExchangesReplicas++;
			// are the signatures not equal => send also data items
			if (oldSign==null || !oldSign.equals(sign)) {
				table.duplicate(col);
			} else {
				if (PGridP2P.sharedInstance().isInTestMode())
					mPGridP2P.getStatistics().ExchangesRealReplicas++;
			}
		} else {
			// construct common path and its length
			String commonPath = Utils.commonPrefix(mPGridP2P.getLocalPath(), path);
			int len = commonPath.length();

			// compute path lengths, union table, and table selections
			int lLen = mPGridP2P.getLocalPath().length() - len;
			int rLen = path.length() - len;

			// if we are in case 3a or 3b, we should send the complementary path too
			if ((lLen == 0) && (rLen > 0) || (rLen == 0) && (lLen > 0)) {
				col.select(table, new String[]{commonPath}, null, false);
			} else {
				col.select(table, new String[]{path}, null, false);
			}
		}
		if (PGridP2P.sharedInstance().isInTestMode())
			mPGridP2P.getStatistics().DataItemsSent += col.count();
		return col;
	}

	/**
	 * Read all index entries messages and add each IE in the ex object
	 * @param ex the exchange message
	 * @param sendingTime time in ms taken to send all items
	 * @return true if all messages were received
	 */
	private boolean readIndexEntries(Exchange ex, long sendingTime) {
		long timeout = System.currentTimeMillis() + READ_ATTEMPT_TIMEOUT - sendingTime;
		boolean found = false;
		int count = 0;
		ExchangeIndexEntriesMessage ie;

		synchronized(mEntriesMsg) {
			LOGGER.finest("BEFORE......"+mIndexManager.getIndexTable().count());
			while (true) {
				long sleepTime = timeout - System.currentTimeMillis();


				for (Iterator it = mEntriesMsg.iterator(); it.hasNext();) {
					ie = ((ExchangeIndexEntriesMessage)it.next());
					//mIndexManager.getIndexTable().addAll(ie.getIndexEntries());

					if (ie.getHeader().getReferences().contains(ex.getGUID())) {
						count = count+1;
						if (count >= mTotal) {
							found = true;
							break;
						}
					}
				}
				if (!mEntriesMsg.isEmpty()) {
					mEntriesMsg.clear();
					timeout = System.currentTimeMillis() + READ_ATTEMPT_TIMEOUT;
				}

				if (!found && sleepTime > 0) {
					try {
						mEntriesMsg.wait(sleepTime);
					} catch (InterruptedException e) {
						return false;
					}
				} else {
					break;
				}
			}

			LOGGER.finest("AFTER......"+mIndexManager.getIndexTable().count());
		}
		if (found) LOGGER.finest("All index entries messages received for exchange: ["+ex.getGUID()+"].");
		else LOGGER.finest(""+(mTotal-count)+" out of "+mTotal+" index entries messages are missing for exchange: ["+ex.getGUID()+"].");


		return found;
	}

	boolean isCSVExchangeProcessActive = false;
	boolean isCSVExchangeUploadActive = false;
	Object mCSVEntriesMsg = new Object();
	Object mCSVEntriesUpload = new Object();

	/**
	 * Wait till the ExchangeFile is downloaded from the other participating peer
	 * @param sendingTime
	 */
	private void waitForCSVExchangeProcessToComplete(long sendingTime){
		LOGGER.finest("Waiting for CSV_IE_Message");
		long timeout = System.currentTimeMillis() + READ_ATTEMPT_TIMEOUT - sendingTime;
		synchronized(mCSVEntriesMsg) {
			while (true) {
				long sleepTime = timeout - System.currentTimeMillis();
//				if(!isCSVExchangeProcessActive) break;
				if(mCsvIeEntriesMsg.size() > 0){
					processNewCsvIeMessage();
					LOGGER.finest("CSV_IE_Message received");
					break;
				}
				if(sleepTime<0){
					LOGGER.finest("CSV_IE_Message not received. TIME_WASTED :"+READ_ATTEMPT_TIMEOUT);
					isCSVExchangeProcessActive = false;
					break;
				}
				try {
					mCSVEntriesMsg.wait(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * Wait till the ExchangeFile is uploaded to the other participating peer
	 * @param sendingTime
	 */
	private void waitForCSVUploadToComplete(long sendingTime){
		LOGGER.finest("waiting for CSV_IE_Message sending");
		long timeout = System.currentTimeMillis() + READ_ATTEMPT_TIMEOUT - sendingTime;
		synchronized(mCSVEntriesUpload) {
			while (true) {
				long sleepTime = timeout - System.currentTimeMillis();
				if(!isCSVExchangeUploadActive){
					LOGGER.finest("CSV_IE_Message sending complete");
					break;
				}
				if(sleepTime<0){
					isCSVExchangeUploadActive = false;
					LOGGER.finest("CSV_IE_Message sending aborted. TIME_WASTED :"+READ_ATTEMPT_TIMEOUT);
					break;
				}
				try {
					mCSVEntriesUpload.wait(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	private void uploadData(GUID guid, PGridHost host, DBIndexTable dbIndexTable, String remotePath, String oldRemotePath,String oldLocalPath, String localPath) {
		tInvStage = timeElapsed(tBegin);
		int lenOld = Utils.commonPrefix(oldLocalPath, oldRemotePath).length();
		int lLenOld = oldLocalPath.length() - lenOld;
		int rLenOld = oldRemotePath.length() - lenOld;
		int len = Utils.commonPrefix(localPath, remotePath).length();

//		// send data to remote host
//		DBIndexTable db = new TransferDBIndexTable();
//		if (remotePath.length() > len && path.length() > len)
//		db.select(dbIndexTable, new String[]{remotePath}, null, true);
//		else {
//		db.select(dbIndexTable, new String[]{remotePath}, null, false);
//		}
//		sendSplittedData(host, db, guid);
//		sendSplittedData(host, dbIndexTable, guid, path, remotePath);
		LOGGER.finest("Upload Begins");
		isCSVExchangeUploadActive = true;
//		sendDataInCSV(host,guid,path,remotePath);
		sendDataInCSV(host,guid,localPath,oldLocalPath,remotePath);
		isCSVExchangeUploadActive = false;
		LOGGER.finest("Upload Ends");

		time2 = System.currentTimeMillis();


		// if needed, insert data to the network
//		if (((lLenOld == 0) && (rLenOld > 0)) && !((remotePath.length() == path.length()) && path.length() > len)) {
//		if(isItTimeForSynch()){
//		distributeData();
//		}
//		}

		tDistribute = timeElapsed(time2);

//		LOGGER.finest("Distribution took: "+(System.currentTimeMillis()-time2)+"ms.");

//		db.delete();


		// if needed, insert data to the network
//		if (((lLenOld == 0) && (rLenOld > 0)) && !((remotePath.length() == path.length()) && path.length() > len)) {
//		TransferDBIndexTable dbDiff = new TransferDBIndexTable();
//		dbDiff.select(mIndexManager.getIndexTable(), null, new String[]{path, remotePath}, true);

//		long max = 1000;
//		Collection entries;

//		for (int i=0;;i++) {
//		entries =  dbDiff.getIndexEntries(max, i*max);
//		if (entries.isEmpty()) break;

//		mIndexManager.insertIndexEntries(entries, true);
//		}
//		dbDiff.delete();
//		}

//		LOGGER.finer("Remaining part of upload took: "+(System.currentTimeMillis()-time2)+"ms.");

		//		db.delete();


	}

	private void distributeData(){
		CSVIndexTable distrIdxTable = DbCsvUtils.sharedInstance().getCurrDistrCSVIndexTable();
		if(distrIdxTable.exists() && distrIdxTable.count()>0){
			time1 = System.currentTimeMillis();

			LOGGER.finest("Distributing Records Into Network : "+distrIdxTable.count());
			boolean isException=false;
			try {

				distrIdxTable.openFileForReading();
				long MAX = 1000;
				IndexEntry ie;
				int idx = 0;
				Collection<IndexEntry> entries = new ArrayList<IndexEntry>(); 
				while((ie = distrIdxTable.getNextIndexEntry()) != null){
					idx++;
					entries.add(ie);
					if(idx%MAX == 0){
						LOGGER.finest("Distributing Records Size: "+entries.size());
						mIndexManager.distributeLocalIndexEntries(entries);
						entries = new ArrayList<IndexEntry>();
					}
				}
				if(entries != null && entries.size() > 0){
					mIndexManager.distributeLocalIndexEntries(entries);
//					entries.clear();
				}

			} catch (Exception e) {
				// TODO: handle exception
				isException=true;
				Constants.LOGGER.finest("Warning or error during the distribution of entries.. Will be attempted next time;"+e.getMessage());
			} finally{
				distrIdxTable.closeFileOnReading();
				if(!isException) distrIdxTable.empty();
			}

			tDistribute = timeElapsed(time1);
			LOGGER.finest(tDistribute    +"\t : Distribution");
		}
	}

	private void sendDataInCSV(PGridHost host,GUID guid,String localPath,String oldLocalPath, String remotePath){
		boolean shouldSplit = false;

//		int len = Utils.commonPrefix(path, remotePath).length();
//		if (remotePath.length() > len && path.length() > len)
		if (localPath.length() >= oldLocalPath.length())
			shouldSplit = true;
		else {
			shouldSplit = false;
		}

		time1 = System.currentTimeMillis();

		if(true){
			LOGGER.finest("Db to CSV");
			if(isItTimeForDb2CsvSynch()){
				LOGGER.finest("HERE.. DB to CSV");
			//	DbCsvUtils.sharedInstance().dbToCsv();
			}
//			LOGGER.finer("DB to CSV took: "+(System.currentTimeMillis()-time1)+"ms.");
			tDBtoCSV = timeElapsed(time1);
			time1 = System.currentTimeMillis();

		}
		LOGGER.finest("CSV Filtering__remotePath:"+remotePath+" __localPath:"+localPath+" __shouldSplit:"+(shouldSplit?"TRUE":"FALSE"));

		String fileName = "Remote"+new Random().nextInt(1000000)+".csv";
//		String fileName = "REM_"+mPGridP2P.getLocalHost().toHostString()+"_"+host.toHostString()+".csv";

		DbCsvUtils.sharedInstance().filterCSV(remotePath, localPath, shouldSplit,fileName);
//		LOGGER.finer("Exchange filtering took: "+(System.currentTimeMillis()-time1)+"ms.");
		tCSVFilter = timeElapsed(time1);
		time1 = System.currentTimeMillis();


//		Compression.compressFile(Constants.CSV_DIR+"REMOTE_FILTERED.csv", Constants.CSV_DIR+"REMOTE_FILTERED.csv.zip");
		ExchangeCSVIndexEntriesMessage csvMsg = new ExchangeCSVIndexEntriesMessage(fileName);

		LOGGER.finest("Sending CSV File (# of Entries :"+new CSVIndexTable(fileName).count()+")");
		mMsgMgr.sendMessage(host, csvMsg, null);
//		LOGGER.finer("Exchange sending took: "+(System.currentTimeMillis()-time1)+"ms.");
		time1 = System.currentTimeMillis();

	}

	private void sendSplittedData(PGridHost host, DBIndexTable dbIndexTable, GUID guid,String path,String remotePath) {
		long t = System.currentTimeMillis();
		ExchangeIndexEntriesMessage ieMsg;
		long count = DBView.selection(dbIndexTable, remotePath).count();
		if (count == 0) {
			ieMsg = new ExchangeIndexEntriesMessage((pgrid.GUID) guid);
			mMsgMgr.sendMessage(host, ieMsg, null);
			return;
		}

		int len = Utils.commonPrefix(path, remotePath).length();
		// send data to remote host
		TransferDBIndexTable dataItems = new TransferDBIndexTable();
		if (remotePath.length() > len && path.length() > len)
			dataItems.setShouldDelete(true);
//		db.select(dbIndexTable, new String[]{remotePath}, null, true);
		else {
			dataItems.setShouldDelete(false);
//			db.select(dbIndexTable, new String[]{remotePath}, null, false);
		}

		long data = 0;
		long time = 0;
		long send = 0;

		short total = (short) Math.ceil((double)count/(double)MAX_ENTRIES_PER_MSG);

		LOGGER.finer("Sending "+count+" items to "+host.toHostString()+" in "+total+" messages.");

		// send data
		DBView db;
		int indexItemId = -1;
		for (short i=0;i<total;i++) {
			long limit = (i==total-1)?(count-i*MAX_ENTRIES_PER_MSG):MAX_ENTRIES_PER_MSG;
			indexItemId = dataItems.select(dbIndexTable, new String[]{remotePath}, null, indexItemId, MAX_ENTRIES_PER_MSG,(int)limit );
			db = DBView.limit(dataItems, MAX_ENTRIES_PER_MSG, i*MAX_ENTRIES_PER_MSG);
			ieMsg = new ExchangeIndexEntriesMessage((pgrid.GUID) guid, db,i, total);
			Constants.LOGGER.warning("===> Sending "+i+" of "+total+" messages ");
			mMsgMgr.sendMessage(host, ieMsg, null);
		}
		Constants.LOGGER.finest("Sending "+count+" items took "+ (System.currentTimeMillis()-t)/1000+" secs");
		dataItems.delete();
	}


	/**
	 * Wait for an acknowledgement message
	 *
	 * @return true if the ACK message has been reseved
	 */
	protected boolean waitForAck(ExchangeRequest request) {
		GUID guid =  request.getExchange().getGUID();
		int THREAD_SLEEP_TIME = 2000;//msec
		LOGGER.finest("Waiting For ACK");
		// block until the return message was received or timeout is raised
		long timeout = System.currentTimeMillis() + ATTEMPT_TIMEOUT;

		while (true) {
			long sleepTime = timeout - System.currentTimeMillis();
			if (sleepTime <= 0){
				LOGGER.finest("ACK Not Received("+request.getExchange().getHost().toHostString()+").TIME_WASTED :"+ATTEMPT_TIMEOUT);
				break;
			}

			synchronized (mACKMsg) {
				if (mACKMsg.contains(guid)) {
					mACKMsg.clear();
					LOGGER.finest("ACK Received");
					return true;
				}
			}

			try {
				Thread.currentThread().sleep(THREAD_SLEEP_TIME);
			} catch (InterruptedException e) {
				LOGGER.fine("Exchange interupted.");
			}
//			synchronized (mACKMsg) {
//			mACKMsg.clear();
//			}
		}
		LOGGER.finest("ACK Not Received");
		return false;
	}

	/**
	 * Send an ack message to the other peer
	 */
	protected void sendAck(GUID guid, PGridHost host) {
		ACKMessage ack = new ACKMessage(guid, ACKMessage.CODE_OK);

		Constants.LOGGER.finer("Acknowledging exchange reply ["+guid+"].");
		LOGGER.finest("Sending ACK to "+host.toHostString());


		MessageManager.sharedInstance().sendMessage(host, ack, null);
	}

	/**
	 * Send an nack message to the other peer
	 */
	protected void sendNAck(GUID guid, PGridHost host) {
		ACKMessage nack = new ACKMessage(guid, ACKMessage.CODE_NACK);

		Constants.LOGGER.finer("N-Acknowledging exchange reply ["+guid+"].");
		LOGGER.finest("Sending NACK to "+host.toHostString());

		MessageManager.sharedInstance().sendMessage(host, nack, null);
	}

	/**
	 * Processes a new exchange request.
	 *
	 * @param exchange the exchange request.
	 */
	public void newExchangeRequest(ExchangeMessage exchange) {
		long time = System.currentTimeMillis();
		synchronized(mRequestLock){
			mExchangeRequests.add(new ExchangeRequest(exchange.getExchange(), exchange.hasSeparetedDataMessages()));
//			LOGGER.finest("ExchangeRequest("+exchange.getExchange().getIndexTable().getTableName()+") receivedNew from different Thread.Size("+mExchangeRequests.size()+")");
			LOGGER.finest("ExchReq waited for :"+(System.currentTimeMillis()-time)/1000 +" secs");
		}
		broadcast();
	}

	/**
	 * Processes a new exchange invitation request.
	 *
	 * @param exchangeInvitation the exchange invitation request.
	 */
	public void newExchangeInvitation(ExchangeInvitationMessage exchangeInvitation) {
		long time = System.currentTimeMillis();
		synchronized(mRequestLock){
//			mExchangeInvitations.add(new ExchangeInvitationRequest(exchangeInvitation));
//			LOGGER.finest("ExchangeInvitation receivedNew from different Thread.Size("+mExchangeInvitations.size()+")");
			putEI(new ExchangeInvitationRequest(exchangeInvitation));
			LOGGER.finest("ExchangeInvitation receivedNew from different Thread.Size("+mEImap.size()+")");
			LOGGER.finest("ExchInv waited for :"+(System.currentTimeMillis()-time)/1000 +" secs");
		}
		broadcast();
	}

	/**
	 * A new exchange response was received.
	 *
	 * @param message the response message.
	 */
	public void newExchangeReply(ExchangeReplyMessage message) {
		long time = System.currentTimeMillis();
		synchronized (mRequestLock) {
			mExchangeReplies.add(message);
			LOGGER.finest("ExchangeReply("+message.getExchange().getIndexTable().getTableName()+") receivedNew from different Thread.Size("+mExchangeReplies.size()+")");
			LOGGER.finest("ExchReply waited for :"+(System.currentTimeMillis()-time)/1000 +" secs");
			mRequestLock.notifyAll();
		}
	}

	protected void prepareWorker() throws Exception {
		mThread = Thread.currentThread();
		LOGGER.config("Exchanger thread prepared.");
	}

	protected void releaseWorker() throws Exception {
		LOGGER.config("Exchanger thread released.");
	}

	Map<String, Long> exchInvTimeHandle = new Hashtable<String, Long>();
	Comparator<ExchangeInvitationRequest> EIComparator = new Comparator<ExchangeInvitationRequest>(){
		public int compare(ExchangeInvitationRequest o1,
				ExchangeInvitationRequest o2) {
			if(exchInvTimeHandle.containsKey(o1.getExchangeInvitation().getHeader().getHost().toHostString()) && exchInvTimeHandle.containsKey(o2.getExchangeInvitation().getHeader().getHost().toHostString()))
				return (int) (exchInvTimeHandle.get(o2.getExchangeInvitation().getHeader().getHost().toHostString()) - exchInvTimeHandle.get(o1.getExchangeInvitation().getHeader().getHost().toHostString()));
			else if(exchInvTimeHandle.containsKey(o1.getExchangeInvitation().getHeader().getHost().toHostString()))
				return +1;
			else if(exchInvTimeHandle.containsKey(o2.getExchangeInvitation().getHeader().getHost().toHostString()))
				return -1;
			else
				return (int) (o2.getStartTime() - o1.getStartTime());
		}

	};

	// iterate exchange requests first
	private List<ExchangeRequest> requests = null;

	protected void work() throws Exception {
		List invList =null;
		// if we are interrupted quit
		if (Thread.currentThread().isInterrupted()) return;

		// iterate exchange requests first
		requests = new ArrayList<ExchangeRequest>();

		synchronized(mExchangeLock) {
			synchronized (mRequestLock) {

				if (mExchangeRequests.size() > 0) {
					LOGGER.finest("REQUESTS_SIZE="+mExchangeRequests.size());
					if (mPGridP2P.getMaintenanceManager().isExchangeTime()) {
						requests.addAll(mExchangeRequests);
//						Collections.copy(requests, mExchangeRequests);
					} else {
						// currently no exchange allowed => ignore them
						Constants.LOGGER.finer("Exchanges ignored (no exchange time)!");
						if (PGridP2P.sharedInstance().isInTestMode())
							mPGridP2P.getStatistics().ExchangesIgnored++;
					}
				}
				mExchangeRequests.clear();
			}

			// iterate exchange invitation requests
			synchronized (mRequestLock) {
//				if (mExchangeInvitations.size() > 0) {
//					LOGGER.finest("INVITATIONS_SIZE="+mExchangeInvitations.size());
//					if (mPGridP2P.getMaintenanceManager().isExchangeTime()) {
//						invList = new ArrayList(mExchangeInvitations);
//					} else {
//						// currently no exchange allowed => ignore them
//						Constants.LOGGER.finer("Exchanges ignored (no exchange time)!");
//						if (PGridP2P.sharedInstance().isInTestMode())
//							mPGridP2P.getStatistics().ExchangesIgnored++;
//					}
//				}
//				mExchangeInvitations.clear();
				if (mEImap.size() > 0) {
					LOGGER.finest("INVITATIONS_SIZE="+mEImap.size());
					if (mPGridP2P.getMaintenanceManager().isExchangeTime()) {
						invList = new ArrayList(mEImap.values());
					} else {
						// currently no exchange allowed => ignore them
						Constants.LOGGER.finer("Exchanges ignored (no exchange time)!");
						if (PGridP2P.sharedInstance().isInTestMode())
							mPGridP2P.getStatistics().ExchangesIgnored++;
					}
				}
				mEImap.clear();
			}

		}
		// process exchanges sequentially
		if (requests != null) {
			mIdleFlag = false;
			Constants.LOGGER.finer("requests.size = " + requests.size());
			while(requests.size() != 0){
				ExchangeRequest req = requests.remove(0);

				Constants.LOGGER.finer("Exchange ["+req.getExchange().getGUID()+"] request received from " + req.getExchange().getHost().toHostString());

				MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_WORK_EXCH_REQ);

				LOGGER.finest("Exchange request received from " + req.getExchange().getHost().toHostString());
				LOGGER.finest("Handling Exchange Request BEGINS");
				handleExchangeRequest(req);
				LOGGER.finest("Handling Exchange Request ENDS");

				MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_WORK_EXCH_REQ);
				req = null;
			}
		}

		// process invitation request
		if (invList != null) {
//			Collections.reverse(invList);
			Collections.sort(invList, EIComparator);

			// process exchanges sequentially
			mIdleFlag = false;
			ExchangeInvitationRequest r = null;
			while (!invList.isEmpty()) {
				if (!mExchangeRequests.isEmpty()){
					LOGGER.finest("Breaking because ExchRequest Waiting");
					break;
				}
				r = (ExchangeInvitationRequest) invList.remove(0);
				Constants.LOGGER.finer("Exchange invitation request received from " + r.getExchangeInvitation().getHeader().getHost().toHostString());

				MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_WORK_EXCH_INVIT);

				LOGGER.finest("Exchange invitation request ["+r.getExchangeInvitation().getGUID()+"] received from " + r.getExchangeInvitation().getHeader().getHost().toHostString());
				LOGGER.finest("Handling Exchange invitation BEGINS");
				handleExchangeInvitation(r);
				LOGGER.finest("Handling Exchange invitation ENDS");
				r = null;
				MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_WORK_EXCH_INVIT);
			}
			if (!invList.isEmpty()) {
				synchronized (mRequestLock) {
//					Collections.reverse(invList);
//					mExchangeInvitations.addAll(0, invList);
				}
			}
		}

//		DbCsvUtils.sharedInstance().mergeRecvDistrCSVFile();
		mIdleFlag = true;
		if(isIdle() && mEImap.size() == 0){
//		if(isIdle() && mExchangeInvitations.size() == 0){

			mIdleFlag = false;
//			if(isItTimeForDistribution()){
//				distributeData();
//			}

			//Synchronize  DB and CSV only when there are no more exchanges 
			if(IS_SYNCH_PENDING /*isItTimeForCsv2DbSynch()*/){
//				if(!DbCsvUtils.sharedInstance().isDbUptodateWithCsv()){
//					LOGGER.finest("Csv 2 DB Started....");
//					time1 = System.currentTimeMillis();
//					DbCsvUtils.sharedInstance().csvToDb();
//					tCSV2DB = timeElapsed(time1);
//					LOGGER.finest(tCSV2DB    +"\t : CSV2DB");
//				}
//
//				if(!DbCsvUtils.sharedInstance().isCsvUptodateWithDb()){
//					time1 = System.currentTimeMillis();
//					DbCsvUtils.sharedInstance().dbToCsv();
//					tDBtoCSV = timeElapsed(time1);
//					LOGGER.finest(tDBtoCSV    +"\t : DBtoCSV");
//				}
				mDBCsvSynchManager.setDbToCsvSynchRequest();
				IS_SYNCH_PENDING = false;
			}
		}
		mIdleFlag = true;
	}

	protected boolean isCondition() {
		boolean isCondition = false;

		if (Thread.currentThread().isInterrupted()) {
			halt();
			return false;
		}

		synchronized(mRequestLock) {
//			isCondition = (mActive && !mShutdown && (!mExchangeRequests.isEmpty() || !mExchangeInvitations.isEmpty()));
			isCondition = (mActive && !mShutdown && (!mExchangeRequests.isEmpty() || !mEImap.isEmpty()));
		}
		return isCondition;
	}

	/**
	 * Checks if the exchanger thread is currently idle.
	 * @return <tt>true</tt> if idle, <tt>false</tt> otherwise.
	 */
	boolean isIdle() {
		boolean value;
		synchronized(mExchangeLock) {
//			value = mIdleFlag && mExchangeRequests.isEmpty() && mExchangeInvitations.isEmpty();
			value = mIdleFlag && mExchangeRequests.isEmpty() && mEImap.isEmpty();
		}
		return value;
	}

	/**
	 * Set the minimum storage before a split occurs
	 * @param min
	 */
	public void setMinStorage(int min) {
		mMinStorageEstimate = min;
		mPGridP2P.setProperty(Properties.EXCHANGE_MIN_STORAGE, ""+mMinStorageEstimate);
		if(PGridP2P.sharedInstance().isInTestMode()){
			mPGridP2P.getStatistics().MinStorage = mMinStorageEstimate;
		}
	}

	/**
	 * Returns the minimum storage before a split occures
	 */
	public int getMinStorage() {
		return mMinStorageEstimate;
	}

	/**
	 * Returns the exchanger lock
	 * @return the exchanger lock
	 */
	public Object getExchangerLock() {
		return mExchangeLock;
	}

	/**
	 * Shutdown
	 */
	public void shutdown() {
		halt();
		mShutdown=true;
		if (mThread != null) mThread.interrupt();
		if(synchThread != null) synchThread.interrupt();
	}

	/**
	 * This method is called when a ACK is received.
	 * @param msg the received message
	 * @param guid It's GUID
	 */
	public void newMessage(PGridMessage msg, GUID guid) {
		if (msg instanceof ACKMessage) {
			synchronized(mACKMsg) {
				if(((ACKMessage)msg).getCode()==ACKMessage.CODE_OK){
					Constants.LOGGER.finer("ACK recieved for exchange ["+msg.getHeader().getReferences().iterator().next()+"].");
					mACKMsg.addAll(msg.getHeader().getReferences());
					mACKMsg.notifyAll();
				}else if(((ACKMessage)msg).getCode()==ACKMessage.CODE_NACK){
					Constants.LOGGER.finer("NACK recieved for exchange ["+msg.getHeader().getReferences().iterator().next()+"].");
					mNACKMsg.addAll(msg.getHeader().getReferences());
					mACKMsg.notifyAll();
				}

			}
		}
	}

	public void newExchangeIndexEntriesMessage(ExchangeIndexEntriesMessage msg) {
		synchronized(mEntriesMsg) {
			mTotal = msg.getTotal();
			mEntriesMsg.add(msg);
			mEntriesMsg.notifyAll();
		}
		LOGGER.finest("*#*#**#*#**#*#**#*#**#*#**#*#**#*#**#*#**#*#**#*#*");
		LOGGER.finest("TOTAL......"+mIndexManager.getIndexTable().count());
		LOGGER.finest("....EXTotal :"+mTotal+" messages ;    Current : "+msg.getCurrent());
		LOGGER.finest("*#*#**#*#**#*#**#*#**#*#**#*#**#*#**#*#**#*#**#*#*");

	}

	private long tBegin,time1,time2,tInvStage,tDBtoCSV,tCSVFilter,tNetwrk,tDistribute,tMerge,tCSV2DB,tTotal;
	private long tWaitReply,tWaitAck,tExchAlg, tSendExch, tSendExchReply;

	private long timeElapsed(long t){
		return (System.currentTimeMillis() - t);
	}

	public void newExchangeCSVIndexEntriesMessage(ExchangeCSVIndexEntriesMessage msg) {
		synchronized (mCsvIeEntriesMsg) {
			LOGGER.finest("CSV_IE_MSG receivedNew from different Thread");
			mCsvIeEntriesMsg.add(msg);
		}
	}

	private void processNewCsvIeMessage(){
		isCSVExchangeProcessActive = false;
		waitForCSVUploadToComplete(0);
		synchronized (mCsvIeEntriesMsg) {
			LOGGER.finest("CSV_IE_MSG_SIZE="+mCsvIeEntriesMsg.size());
			ExchangeCSVIndexEntriesMessage msg = mCsvIeEntriesMsg.get(0);
			mCsvIeEntriesMsg.clear();

			LOGGER.finest("Received the CSV file : "+msg.getHeader().getAdditionalAttribute("fileName"));

			time1 = System.currentTimeMillis();
			DbCsvUtils.sharedInstance().mergeLocalAndRemoteCSVFiles(msg.getFileName());
			tMerge = timeElapsed(time1);

			tTotal = timeElapsed(tBegin);
			tNetwrk = tTotal - tInvStage - tDBtoCSV - tCSVFilter - tMerge;

			LOGGER.finest("**********");
			LOGGER.finest(tInvStage  +"\t : InitStage");
			LOGGER.finest(tExchAlg  +"\t : ExchAlgo");
			LOGGER.finest(tSendExch  +"\t : tSendExch");
			LOGGER.finest(tWaitReply  +"\t : tWaitReply");
			LOGGER.finest(tSendExchReply   +"\t : SendExchReply");
			LOGGER.finest(tWaitAck   +"\t : WaitAck");
			LOGGER.finest("------------");
			LOGGER.finest(tDBtoCSV   +"\t : DB2CSV");
			LOGGER.finest(tCSVFilter +"\t : CSV Filter");
			LOGGER.finest(tNetwrk    +"\t : NetworkTransfer");
			LOGGER.finest(tMerge     +"\t : Merging Files");
			LOGGER.finest("**********");
			LOGGER.finest("**** Exchange("+mPGridP2P.getLocalHost().toHostString()+"("+mPGridP2P.getLocalPath()+")"+"<--->"+msg.getHeader().getHost().toHostString()+"("+msg.getHeader().getHost().getPath()+")"+") took: "+tTotal+" ms.");

			tBegin = System.currentTimeMillis();
			mPGridP2P.getLocalHost().setProperty("Size",	 IndexManager.getInstance().getCSVIndexTable().count()+"");
			msg = null;

			tExchAlg=0;tSendExch = 0;tSendExchReply=0;tWaitAck=0;tWaitReply= 0;
		}
		lastUsefulExchangeTime = System.currentTimeMillis(); //receiving this message implies some useful exchange taking place; 
		IS_SYNCH_PENDING = true;

	}
	/**
	 * Start exchanger
	 */
	public void startExchanger() {
		mActive = true;
	}

	/**
	 * Stop exchanger
	 */
	public void stopExchanger() {
		mActive = false;
	}

	private boolean isItTimeForCsv2DbSynch(){
//		return true;
		return ((!mPGridP2P.getInitExchanges() && (lastUsefulExchangeTime!=0 && timeElapsed(lastUsefulExchangeTime)>TIME_SINCE_LAST_USEFUL_EXCHANGE))||IS_SYNCH_PENDING);

	}

	private boolean isItTimeForDb2CsvSynch(){
		return false;
//		return true;
	}

	private boolean isItTimeForDistribution(){
		return true;
//		return false;
	}

	/**
	 * EI's are put into the EImap only if EI from particular host doesn't exist or
	 * the existing EI is not new enough.
	 * @param ei
	 */
	private void putEI(ExchangeInvitationRequest ei){
		if(mEImap.containsKey(ei.getExchangeInvitation().getHeader().getHost().getGUID())){
			if(mEImap.get(ei.getExchangeInvitation().getHeader().getHost().getGUID()).getStartTime() < ei.getStartTime()){
				mEImap.put(ei.getExchangeInvitation().getHeader().getHost().getGUID(), ei);
			}
		}else{
			mEImap.put(ei.getExchangeInvitation().getHeader().getHost().getGUID(), ei);	
		}
	}

}