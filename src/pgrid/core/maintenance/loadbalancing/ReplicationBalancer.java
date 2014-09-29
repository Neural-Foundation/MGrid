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

package pgrid.core.maintenance.loadbalancing;

import pgrid.Constants;
import pgrid.PGridHost;
import pgrid.Properties;
import p2p.basic.GUID;
import pgrid.core.index.IndexManager;
import pgrid.core.maintenance.loadbalancing.TransitionProbability;
import pgrid.core.maintenance.loadbalancing.ExchangeStatistics;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.MessageManager;
import pgrid.network.router.MessageWaiter;
import pgrid.network.protocol.SearchPathMessage;
import pgrid.network.protocol.SearchPathReplyMessage;
import pgrid.network.protocol.PGridMessage;
import pgrid.util.logging.LogFormatter;

import java.security.SecureRandom;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * This class represents the replication balancer for load balancing in P-Grid.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class ReplicationBalancer extends pgrid.util.WorkerThread implements MessageWaiter {

	/**
	 * The constant to reduce oscillatory behavior.
	 */
	private static final double BL = 0.1;

	/**
	 * The PGridP2P.Exchanger logger.
	 */
	public static final Logger LOGGER = Logger.getLogger("PGridP2P.Balancer");

	/**
	 * The logging file.
	 */
	public static final String LOG_FILE = "Balancer.log";

	/**
	 * The minimum of amount of accumulated samples.
	 */
	//private static final int MIN_CHANGE = 10; // 10 is a good value
	private static final int MIN_CHANGE = 10; // 4 for debug

	/**
	 * The interval between two path change checks.
	 */
	private static final int PATH_CHANGE_INTERVAL = 2*60*1000; // 2 min. for debug

	/**
	 * Further probability attenuete factor.
	 */
	private static final double PROB_C = 0.25;

	/**
	 * The time to wait for responses.
	 */
	private static final int REPLY_TIMEOUT = 120000; // 2 min.

	/**
	 * Request type the worker has to deal with
	 */
	private enum RequestType {localReply, remoteReply, remoteSearch}

	/**
	 * Contains all load pending balancing
	 */
	private Vector<Request> mAttempts = new Vector<Request>();

	/**
	 * Contains the reply
	 */
	private Request mReplyRequest = null;

	/**
	 * Mapping bt GUID and host to forward message to
	 */
	HashMap<p2p.basic.GUID, SearchPathMessage> mGUID2MsgMapping = new HashMap<p2p.basic.GUID, SearchPathMessage>();

	/**
	 * Bounded fifo containing keys of mGUIDHostMapping
	 */
	private ArrayBlockingQueue<p2p.basic.GUID> mRemoteMsgGUID = new ArrayBlockingQueue<p2p.basic.GUID>(50);

	/**
	 * Current replication message
	 */
	private SearchPathMessage mCurrentMsg = null;

	/**
	 * Last replication phase
	 */
	private long mLastReplication = System.currentTimeMillis();

	/**
	 * List of listener
	 */
	private Vector<ReplicateBalancerListener> mListeners = new Vector<ReplicateBalancerListener>(2);

	/**
	 * The message manager.
	 */
	private MessageManager mMsgMgr = MessageManager.sharedInstance();

	/**
	 * The PGridP2P facility.
	 */
	private PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 * The transition probabilities.
	 */
	private TransitionProbability mProbTrans = new TransitionProbability();

	/**
	 * The randomizer delivers random numbers.
	 */
	private SecureRandom mRandomizer = new SecureRandom();

	/**
	 * The statistics collected by exchanges.
	 */
	private ExchangeStatistics mStats = new ExchangeStatistics();

	/**
	 * The statistics collected by exchanges.
	 */
	private ExchangeStatistics mOldStats = new ExchangeStatistics();

	/**
	 * The data item manager.
	 */
	private IndexManager mIndexManager = null;

	/**
	 * Synchronize helper object
	 */
	private final Object mWaiter = new Object();

	private Thread mThread = null;

	/**
	 * The worker timeout
	 */
	private static final int WORKER_TIMOUT = 1*60*1000; // 1 min.

	static {
		LogFormatter formatter = new LogFormatter();
		formatter.setDateFormat("HH:mm:ss");
		formatter.setFormatPattern(LogFormatter.DATE + ": " + LogFormatter.MESSAGE + LogFormatter.NEW_LINE + LogFormatter.THROWABLE);
		Constants.initChildLogger(LOGGER, formatter, null); //LOG_FILE);
	}

	/**
	 * Enable or disable load balancing
	 * @param balance true to enable load balancing
	 */
	public void setLoadBalancing(boolean balance) {
		mPGridP2P.setProperty(Properties.REPLICATION_BALANCE, balance+"");
	}

	/**
	 * Creates and starts the replication balancer.
	 */
	public ReplicationBalancer() {
		mIndexManager = mPGridP2P.getIndexManager();
		this.setTimeout(WORKER_TIMOUT);
	}

	/**
	 * Checks if the task can proceed.
	 *
	 * @return <tt>true</tt> if this task can proceed, for example after been waiting on the internal lock.
	 */
	protected boolean isCondition() {
		boolean cond = (((mPGridP2P.propertyBoolean(Properties.REPLICATION_BALANCE) || mPGridP2P.propertyBoolean(Properties.DYNAMIC_JOIN)) &&
				(System.currentTimeMillis()-mLastReplication) >= PATH_CHANGE_INTERVAL)) || !mAttempts.isEmpty();

		return cond;
	}

	/**
	 * Handles an occured exception.
	 *
	 * @param t error to be handled
	 */
	protected void handleError(Throwable t) {

	}

	/**
	 * Called just after run() method starts
	 */
	protected void prepareWorker() throws Exception {
		mThread = Thread.currentThread();
	}

	/**
	 * Called just before run() method stops
	 */
	protected void releaseWorker() throws Exception {

	}

	/**
	 * Resets the statistics.
	 */
	public void resetStatistics() {
		mStats.reset();
	}

	/**
	 * Substract checkpoint stat to the current statistics.
	 */
	protected void revertStatistics() {
		for (int i = 0; i < mPGridP2P.getLocalPath().length(); i++) {
			mStats.substractCheckpoint(i);
		}
	}

	/**
	 * Does a task's job; can wait on the internal lock Object or any other
	 * Objects during execution
	 *
	 * @throws Exception: including InterruptedException if the task was
	 *                    interrupted by thread's interrupt()
	 */
	protected void work() throws Exception {
		// this method is called each time a task should be done
		// Stop the exchanger thread

		mLastReplication = System.currentTimeMillis();

		// 1. no pending processing, so it's time to probe the network
		if (mAttempts.isEmpty() && mPGridP2P.propertyBoolean(Properties.REPLICATION_BALANCE) && mPGridP2P.getRoutingTable().getReplicaVector().size() != 0) {
			// ChangePath Alg. (Algorithm 6)
			int l = mPGridP2P.getLocalPath().length();
			boolean stop = false;
			mProbTrans = new TransitionProbability();
			while ((l > 0) && (!stop)) {
				if (mStats.samePath(l) >= mStats.compPath(l)) {
					if (mStats.count(l) > MIN_CHANGE) {
						mProbTrans.union(l, PROB_C * Math.max(mStats.samePath(l) - mStats.compPath(l) - BL, 0) / (2.0 * mStats.samePath(l)));
					}
				} else {
					stop = true;
				}
				l--;
			}
			double[][] sortedProbTrans = mProbTrans.sort();
			int k = 1;
			boolean change = false;
			while ((!change) && (k < sortedProbTrans[0].length)) {
				double prob = mRandomizer.nextDouble();
				if (prob < sortedProbTrans[1][k]) {
					synchronized(mPGridP2P.getMaintenanceManager().getExchangerLock()) {
						mReplyRequest = null;
						searchPathAndClone((int) sortedProbTrans[0][k]);
						change = waitReply();
						resetStatistics();
					}
				}
				k++;
			}
			if (!change) {
				for (int i = 1; i <= mPGridP2P.getLocalPath().length(); i++) {
					if (mStats.count(i) >= (2 * MIN_CHANGE)) {
						revertStatistics();
					}
				}
			}

			} else {
			// 2. there is some pending task, execute them now
			Vector<Request> requests = null;
			synchronized (mAttempts) {
				requests = (Vector<Request>) mAttempts.clone();
				mAttempts.clear();
			}

			for (Request request: requests) {
				switch(request.mType) {
					case localReply:
						processLocalReply(request);
					break;
					case remoteSearch:
						processRemoteSearchPath(request);
					break;
				}
			}
		}
	}

	private boolean waitReply() {
		// wait for reply message
		long starttime = System.currentTimeMillis();
		long timeout;
		while (true) {
			timeout = REPLY_TIMEOUT - (System.currentTimeMillis()-starttime);
			if (timeout <= 0) break;


			synchronized(mWaiter) {
				if (mReplyRequest != null) {
				   	processLocalReply(mReplyRequest);

					if (mReplyRequest.getCode() == SearchPathReplyMessage.CODE_OK) {
					    mReplyRequest = null;
						return true;
					}
				}
				try {
					mWaiter.wait(timeout);
				} catch (InterruptedException e) {
					timeout = System.currentTimeMillis() + REPLY_TIMEOUT;
				}
			}
		}
		mReplyRequest = null;
		return false;
	}

	/**
	 * This method will ask a random peer to find an overloaded partition,
	 * if it cannot find one, the random peer will be cloned.
	 * @param hosts a list of random peers
	 */
	public void cloneRandomPeer(Collection hosts) {
		if (hosts.size() == 0) {
			LOGGER.log(Level.WARNING, "Try to dynamically join the network with an empty list of random hosts.");
			return;
		}

		// shake it, shake it!
		Vector<PGridHost> rndList = new Vector<PGridHost>(hosts);
		Collections.shuffle(rndList, mRandomizer);
		PGridHost host = null;

		SearchPathMessage msg = new SearchPathMessage(mPGridP2P.getLocalHost(), true);

		mMsgMgr.registerWaiter(msg.getHeader().getGUID(), this);

		// send the message
		boolean sent = false;
		for (int i=0;i<rndList.size() && !sent; i++) {
			host = rndList.get(i);
			if (host.equals(mPGridP2P.getLocalHost())) continue;

			LOGGER.fine("ask peer " + host.toHostString() + " for a peer to clone.");

			sent = MessageManager.sharedInstance().sendMessage(host, msg, null);
			if (sent) {
				mCurrentMsg=msg;
			} else {
				LOGGER.fine("Unable to send the cloning message to " + host.toHostString() + ".");
			}
		}
		// wait for reply
		synchronized(mPGridP2P.getMaintenanceManager().getExchangerLock()) {
			waitReply();
		}
	}

	/**
	 * Invoked when a remote search path message was received.
	 *
	 * @param host the sending host.
	 * @param msg  the received message.
	 */
	public void remoteSearchPath(PGridHost host, SearchPathMessage msg) {
		Request request = new Request();

		request.mType = RequestType.remoteSearch;
		request.mMsg = msg;

		mAttempts.add(request);
		broadcast();
	}

	/**
	 * Process the newly received search path message
	 *
	 * @param request contains the received message.
	 */
	private void processRemoteSearchPath(Request request) {
		//check if it is the correct request
		if (request.mType != RequestType.remoteSearch) {
			// maybe I should throw an exception
			LOGGER.log(Level.WARNING, "Wrong request for processRemoteSearchPath");
			return;
		}

		SearchPathMessage msg = request.mMsg;
		PGridHost host = msg.getHeader().getHost();

		boolean dynJoin = msg.isDynJoin();
		LOGGER.fine("received remote search path messeage from host " + host.toHostString() + " for a " + (dynJoin?"dynamic join":"dynamic load balancing") + " of the network.");

		int l0;
		// Check if this message is a dynamic joining of the network
		if (dynJoin) {
			msg.setDynJoin(false);

			// dynamic join
			l0 = findOverLoadedLevel();
			if (l0 >= mPGridP2P.getLocalPath().length()) {
				//updateStatistics(l0, 0,mPGridP2P.getLocalPath().length()-l0, 0);
			} else {
				LOGGER.fine("Dynamic join: a peer at level " + l0 + " will be cloned for message " + msg.getGUID() + ".");
				mMsgMgr.randomRoute(msg, l0, null, null);
				return;
			}

		}

		// local peer is to be cloned
		LOGGER.fine("local peer will be cloned.");
		SearchPathReplyMessage replyMsg = new SearchPathReplyMessage(msg.getGUID(), mPGridP2P.getLocalPath(), mPGridP2P.getRoutingTable(), mIndexManager.getIndexTable(), mPGridP2P.getMaintenanceManager().getMinStorage());
		mMsgMgr.reply(msg.getRequestingHost(), replyMsg, msg, null, null);
		mPGridP2P.getRoutingTable().acquireWriteLock();
		try {
			PGridHost rhost = msg.getRequestingHost();
			mPGridP2P.getRoutingTable().remove(rhost);
			rhost.setPath(mPGridP2P.getLocalPath(), rhost.getRevision()+1);
			mPGridP2P.getRoutingTable().addReplica(rhost);
		} finally{
			mPGridP2P.getRoutingTable().releaseWriteLock();	
		}
	}

	/**
	 * Searches for a suitable peer to clone if the local peer decided to balance load.
	 *
	 * @param level the level to search for a peer to clone.
	 */
	protected void searchPathAndClone(final int level) {
		LOGGER.fine("search path and clone at level " + (level - 1) + ".");
		mCurrentMsg = new SearchPathMessage(mPGridP2P.getLocalHost(), false);
		mMsgMgr.randomRoute(mCurrentMsg, level-1, null, this);
	}

	/**
	 * Process an incomming reply message triggered by searchPathAndClone method
	 * @param request contains the reply message
	 */
	protected void processLocalReply(Request request) {
		String oldPath = "";

		//check if it is the correct request
		if (request.mType != RequestType.localReply) {
			// maybe I should throw an exception
			LOGGER.log(Level.WARNING, "Wrong request for processLocalReply");
			return;
		}

		PGridHost host = request.mReply.getHeader().getHost();

		if (request.getCode() == SearchPathReplyMessage.CODE_PATH_CHANGED) {
			// the path of the reference has changed => remove reference and try another host
			mPGridP2P.getRoutingTable().removeLevel(host);
			int level = pgrid.util.Utils.commonPrefix(mPGridP2P.getLocalPath(),
						request.mReply.getPath()).length();

			ReplicationBalancer.LOGGER.fine("received local search path reply messeage from host " + host.toHostString() + " with code 'Path Changed'.");
			searchPathAndClone(level);

		} else if (request.getCode() == SearchPathReplyMessage.CODE_OK) {
			// a host was found => clone it
			Collection owndata;
			ReplicationBalancer.LOGGER.fine("received local search path reply messeage from host " + host.toHostString() + " with code 'OK'.");
		   	oldPath = mPGridP2P.getLocalPath();
			mPGridP2P.getRoutingTable().acquireWriteLock();
			try {
				mPGridP2P.setLocalPath(request.mReply.getPath());
				mPGridP2P.getRoutingTable().clear();
				mPGridP2P.getRoutingTable().setFidgets(request.mReply.getRoutingTable().getFidgetVector());
				for (int i = 0; i < request.mReply.getPath().length(); i++) {
					mPGridP2P.getRoutingTable().setLevel(i, request.mReply.getRoutingTable().getLevelVector(i));
				}
				mPGridP2P.getRoutingTable().setReplicas(request.mReply.getRoutingTable().getReplicaVector());
				mPGridP2P.getRoutingTable().save();
			} finally{
				mPGridP2P.getRoutingTable().releaseWriteLock();
			}

			// if this peer is changing path to balance the load,
			// drop its data table, otherwise (dynamic join) keep it
			owndata = mIndexManager.getIndexTable().getIndexEntries();

			if (request.mReply.getIndexTable() != null) {
				mIndexManager.getIndexTable().addAll(request.mReply.getIndexTable().getIndexEntries());
			}
			ReplicationBalancer.LOGGER.config("cloned host " + host.toHostString() + " with path " + request.mReply.getPath() + ".");

			mIndexManager.insertIndexEntries(owndata, true);

			// inform lister of changed path
			pathChanged(oldPath, request.mReply.getPath(), request.mReply.getMinStorage());
		}
	}

	/**
	 * Updates the statistics during an exchange.
	 *
	 * @param commonLen  the common length of exchanging peers.
	 * @param currentLen the current length of a previous peer.
	 * @param lLen       the remaining length of the local path (path.len-commonLen).
	 * @param rLen       the remaining length of the remote path (path.len-commonLen).
	 */
	public void updateStatistics(int commonLen, int currentLen, int lLen, int rLen) {
		int l = currentLen;
		while ((l <= commonLen) && (lLen >= commonLen) && (rLen >= commonLen)) {
			mStats.incCount(l + 1);
			if (commonLen > l) {
				mStats.incSamePath(l + 1, rLen);
			} else {
				mStats.incCompPath(l + 1, rLen);
			}

			if (mStats.count(l + 1) == MIN_CHANGE) {
				mStats.checkpoint(l);
			}
			l++;
		}
	}

	/**
	 * A new search path reply message has been received by the message manager.
	 * @param searchPathReply the message
	 */
	public void newSearchPathReply(SearchPathReplyMessage searchPathReply) {
		Request request = new Request();
		GUID guid = searchPathReply.getReferencedMsgGUID();

		// differentiate local load balancing then remote one
		if (mCurrentMsg != null && mCurrentMsg.getGUID().equals(guid)) {
			request.mType = RequestType.localReply;
			request.mReply = searchPathReply;
			request.setCode(searchPathReply.getCode());

			synchronized(mWaiter) {
				mReplyRequest = request;
				mWaiter.notifyAll();
			}

		}

	}

	public void newMessage(PGridMessage msg, GUID guid) {
		if (msg instanceof SearchPathReplyMessage)
			newSearchPathReply((SearchPathReplyMessage) msg);
	}

	/**
	 * Returns the owner of the pending message if it exists or null.
	 * @param guid message GUID
	 * @return the owner of the pending message if it exists or null.
	 */
	protected SearchPathMessage getPendingMsg (p2p.basic.GUID guid) {
		SearchPathMessage msg = null;

		synchronized(mRemoteMsgGUID) {
			msg = mGUID2MsgMapping.get(guid);
		}

		return msg;
	}

	/**
	 * Returns an overloaded level of the trie
	 */
	private int findOverLoadedLevel() {
		int overloadedLevel = -1;
		int len = mPGridP2P.getLocalPath().length();
		boolean stop = false;
		mProbTrans = new TransitionProbability();
		while ((len > 0) && (!stop)) {
			if (mStats.samePath(len) >= mStats.compPath(len)) {
				//if (mStats.count(len) > MIN_CHANGE) {
					mProbTrans.union(len, PROB_C * Math.max(mStats.samePath(len) - mStats.compPath(len) - BL, 0.0) / (2.0 * mStats.samePath(len)));
				//}
			} else {
				//stop = true;
			}
			len--;
		}
		double[][] sortedProbTrans = mProbTrans.sort();
		int k = 1;
		if (k < sortedProbTrans[0].length) {
			overloadedLevel = (int) sortedProbTrans[0][k]-1;
		}

		if(PGridP2P.sharedInstance().isInDebugMode()) {
			LOGGER.finest("Probability listing:\n");
			for (int i=1;i<sortedProbTrans[0].length; i++)
				LOGGER.finest("Level "+(sortedProbTrans[0][i]-1)+": prob: "+sortedProbTrans[1][i]);
		}

		// if no overloaded level is founded, return it-self
		if (overloadedLevel == -1) return mPGridP2P.getLocalPath().length();

		return overloadedLevel;
	}

	/**
	 * inform listener about the path change
	 *
	 * @param path			the new path of this peer
	 * @param minStorage	MinStorage constant of the replica
	 */
	private void pathChanged(String oldPath, String path, int minStorage) {
		for(ReplicateBalancerListener listener: mListeners) {
			listener.localPathChanged(oldPath, path, minStorage);
		}
	}

	/**
	 * Add a listener to this class
	 * @param listener
	 */
	public void addListener(ReplicateBalancerListener listener) {
		if (!mListeners.contains(listener)) {
			mListeners.add(listener);
		}
	}

	/**
	 * Remove a listener to this class
	 * @param listener
	 */
	public void removeListener(ReplicateBalancerListener listener) {
	   	mListeners.remove(listener);
	}

	public void shutdown() {
		if (mThread != null) mThread.interrupt();
	}

	/**
	 * This class represent a token for the worker
	 */
	class Request {
		private int mCode;
		public RequestType mType;
		public SearchPathReplyMessage mReply;
		public SearchPathMessage mMsg;

		public void setCode(int code){mCode = code;}
		public int getCode(){return mCode;}
	}

}
