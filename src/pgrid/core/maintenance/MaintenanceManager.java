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

import p2p.index.events.IndexListener;
import pgrid.*;
import pgrid.core.index.IndexManager;
import pgrid.core.maintenance.loadbalancing.ReplicationBalancer;
import pgrid.core.maintenance.loadbalancing.ReplicateBalancerListener;
import pgrid.core.RoutingTable;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.protocol.*;
import pgrid.network.router.MessageWaiter;
import pgrid.network.MessageManager;
import pgrid.util.monitoring.MonitoringManager;
import java.util.Collection;
import java.util.Vector;
import java.util.logging.Level;


/**
 * This class manages all maintenance tasks of P-Grid.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class MaintenanceManager extends pgrid.util.WorkerThread implements IndexListener, ReplicateBalancerListener, MessageWaiter {

	/**
	 * Local peer runs.
	 */
	static final long DYNAMIC_JOIN_TIMEOUT = 2*60*1000; //2 min

	/**
	 * Local peer sleeps.
	 */
	static final short PHASE_SLEEPS = 0;

	/**
	 * Local peer bootstraps.
	 */
	static final short PHASE_BOOTSTRAP = 1;

	/**
	 * Local peer perform a dynamic join.
	 */
	static final short PHASE_DYNAMIC_JOIN = 5;

	/**
	 * Peer build an unstructured network by exchanging their fidget lists.
	 */
	static final short PHASE_FIDGET_EXCHANGE = 2;

	/**
	 * Local peer replicates its data.
	 */
	static final short PHASE_REPLICATE = 3;

	/**
	 * Local peer runs.
	 */
	static final short PHASE_RUN = 4;

	/**
	 * The PGrid bootstrapper.
	 */
	private Bootstrapper mBootstrapper = null;

	/**
	 * The time the construction starts.
	 */
	private long mConsructionStartTime = Long.MAX_VALUE;

	/**
	 * The dynamic load balancer of P-Grid
	 */
	private ReplicationBalancer mBalancer = null;

	/**
	 * True iff performing a dynamic join
	 */
	private boolean mDynamicJoining = false;

	/**
	 * The initiator for Exchanges.
	 */
	private ExchangeInitiator mExchangeInitiator = null;

	/**
	 * The PGrid exchanger.
	 */
	private Exchanger mExchanger = null;

	/**
	 * The exchanger thread
	 */
	private Thread mExchangerThread = null;

	/**
	 * Arbitrary maintenance tasks
	 */
	private Vector<Runnable> mArbitraryMaintenanceTask = new Vector<Runnable>();

	/**
	 * Next dynamic join tentative
	 */
	private long mNextDynamicJoin = 0;

	/**
	 * The current phase.
	 */
	private short mPhase = PHASE_SLEEPS;

	/**
	 * The PGridP2P facility.
	 */
	private PGridP2P mPGridP2P = null;

	/**
	 * The time the replication starts.
	 */
	private long mReplicationStartTime = Long.MAX_VALUE;

	/**
	 * The replicator.
	 */
	private Replicator mReplicator = null;
	
	/**
	 * Keep a counter on the number of time data have been replicated in the replication phase.
	 */
	private int mReplicationCount = 0;

	private boolean isCondition = true;

	/**
	 * Data lock
	 */
	private final Object mDataLock = new Object();

	/**
	 * Reference to the worker thread
	 */
	private Thread mThread;

	/**
	 * Constructs the Exchanger.
	 *
	 * @param p2p the P-Grid P2P facility.
	 */
	public MaintenanceManager(PGridP2P p2p) {
		super();
		mPGridP2P = p2p;
		Constants.LOGGER.config("starting P-Grid Maintenance manager ...");

		// set the phase to sleep until we join.
		mPhase = PHASE_SLEEPS;

		if (mPGridP2P.isInMonitoredMode()) 
			MonitoringManager.sharedInstance().reportPhase(PHASE_SLEEPS);

		// Replicator
		mReplicator = new Replicator(mPGridP2P);

		// Exchange Initiator
		mExchangeInitiator = new ExchangeInitiator(mPGridP2P);

		// Exchanger
		mExchanger = new Exchanger(mPGridP2P, this);
		mExchanger.setMinStorage(mPGridP2P.propertyInteger(Properties.EXCHANGE_MIN_STORAGE));

		// start bootstrap manager
		mBootstrapper = new Bootstrapper(mPGridP2P, this);

		// Dynamic load balancing
		mBalancer = new ReplicationBalancer();
		Thread thread = new Thread(mBalancer, "ReplicationBalancer");
		thread.setDaemon(true);
		thread.start();

		// register the maintenance manager as a storage listener
		// in order to compute the min storage
		IndexManager.getInstance().addIndexListener(this);


		// set worker thread timeout
		setTimeout(1000);
	}

	/**
	 * Initialize the maintenance manager
	 */
	public void init() {
		Constants.LOGGER.fine("Super peer status: " + mPGridP2P.isSuperPeer());
		
		if (mPGridP2P.isSuperPeer() && mExchangerThread == null) {
			createExchangerThread();
			mExchangerThread.start();
		}
	}	

	/**
	 * Tries to re-bootstrap in case exchanges were unsuccessful.
	 */
	void bootstrap() throws Exception {
		mBootstrapper.bootstrap();
	}

	/**
	 * Adds the given host to the list of bootstrap hosts.
	 *
	 * @param host the new bootstrap host.
	 */
	public void bootstrap(PGridHost host) {
		// if the current state is sleeping then the bootstrapping phase starts
		synchronized(mDataLock) {
			if (mPhase == PHASE_SLEEPS) {
				mPhase = PHASE_BOOTSTRAP;
				if (mPGridP2P.isInMonitoredMode()) 
					MonitoringManager.sharedInstance().reportPhase(PHASE_BOOTSTRAP);
			}
		}
		mBootstrapper.addBootstrapHost(host);

		broadcast();
	}

	/**
	 * Returns the used bootstrap hosts.
	 * @return the bootstrap hosts.
	 */
	public Collection getBootstrapHosts() {
		return mBootstrapper.getHosts();
	}

	/**
	 * Returns true iff the local host is a bootstrap host
	 * @return true if the localhost is a bootstrap host
	 */
	public boolean isBootstrapHost() {
		return mBootstrapper.isBootstrapHost();
	}


	/**
	 * Returns the construction phase start time.
	 * @return the start time.
	 */
	long getConstructionStartTime() {
		long time;
		synchronized(mDataLock) {
			time = mConsructionStartTime;
		}
		return time;
	}

	/**
	 * Returns the current phase of P-Grid.
	 * @return the current phase.
	 */
	short getPhase() {
		short phase;

		synchronized(mDataLock) {
			phase = mPhase;
		}

		return phase;
	}

	/**
	 * Returns the replication phase start time.
	 * @return the start time.
	 */
	long getReplicationStartTime() {
		long time;

		synchronized(mDataLock) {
			time = mReplicationStartTime;
		}
		return time;
	}

	protected void handleError(Throwable t) {
		if (t instanceof InterruptedException) {
			Constants.LOGGER.finer("Maintenance manager interupted.");
			halt();
		} else {
			Constants.LOGGER.log(Level.WARNING, "Error in Maintenance thread", t);
		}
	}

	protected boolean isCondition() {
		return isCondition;
	}

	/**
	 * Joins the network.
	 */
	public void join() {
		synchronized(mDataLock) {

			// if this peer has already join the network in the past, initialize it with past value
			if (mPGridP2P.getLocalPath().length() != 0) {
				// set the start times to default values if they are unknown
				mReplicationStartTime = mPGridP2P.propertyLong(Properties.REPLICATION_START_TIME);
				mConsructionStartTime = mPGridP2P.propertyLong(Properties.CONSTRUCTION_START_TIME);

				mPhase = PHASE_RUN;

				if (mPGridP2P.isInMonitoredMode())
					MonitoringManager.sharedInstance().reportPhase(PHASE_RUN);
				
			} else {
				mExchanger.setMinStorage(0);

		
				// if we are a bootstrap host, set replication and construction value
				if (mBootstrapper.isBootstrapHost()) {
				
					// set the start times to default values if they are unknown
					mReplicationStartTime = mPGridP2P.propertyLong(Properties.REPLICATION_START_TIME);
					mConsructionStartTime = mPGridP2P.propertyLong(Properties.CONSTRUCTION_START_TIME);

					// if no properties are available, take default
					if ((mReplicationStartTime == 0)) {
						long currentTime = System.currentTimeMillis();
						mReplicationStartTime = currentTime + Constants.BOOTSTRAP_REPLICATION_DELAY;
						mPGridP2P.setProperty(Properties.REPLICATION_START_TIME, Long.toString(mReplicationStartTime));
					}
					if ((mConsructionStartTime == 0)) {
						long currentTime = System.currentTimeMillis();
						mConsructionStartTime = currentTime + Constants.BOOTSTRAP_CONSTRUCTION_DELAY;
						mPGridP2P.setProperty(Properties.CONSTRUCTION_START_TIME, Long.toString(mConsructionStartTime));
					}
				} else {
					if (PGridP2P.sharedInstance().isInTestMode()) {
						// if the peer is not a bootstrap host, ensure that it will not start replication and construction
						// before boostrapping with an appropriate host.
						mReplicationStartTime = Long.MAX_VALUE;
						mConsructionStartTime = Long.MAX_VALUE;
						mPGridP2P.setProperty(Properties.REPLICATION_START_TIME, Long.toString(Long.MAX_VALUE) /*Properties.REPLICATION_START_TIME*/);
						mPGridP2P.setProperty(Properties.CONSTRUCTION_START_TIME, Long.toString(Long.MAX_VALUE) /*Properties.CONSTRUCTION_START_TIME*/);
						
					}
				}

				if (mPhase == PHASE_SLEEPS) {
					if (mBootstrapper.hasBootstrapped() && mPGridP2P.isSuperPeer()){
						mPhase = PHASE_FIDGET_EXCHANGE;
						if (mPGridP2P.isInMonitoredMode())
							MonitoringManager.sharedInstance().reportPhase(PHASE_FIDGET_EXCHANGE);
					} else {
						mPhase = PHASE_BOOTSTRAP;
						if (mPGridP2P.isInMonitoredMode())
							MonitoringManager.sharedInstance().reportPhase(PHASE_BOOTSTRAP);
					}
				}
			}
			if (mPGridP2P.isSuperPeer())
				setInitExchanges(true);
		}

		Constants.LOGGER.finer("Joining P-Grid network.");
		
		broadcast();
	}

	/**
	 * Processes a new addBootstrapHost request.
	 *
	 * @param bootstrap the addBootstrapHost request.
	 */
	public void newBootstrapRequest(BootstrapMessage bootstrap) {
		mBootstrapper.newBootstrapRequest(bootstrap);
	}

	/**
	 * Processes a new addBootstrapHost response.
	 *
	 * @param bootstrapReply the addBootstrapHost response.
	 */
	public void newBootstrapReply(BootstrapReplyMessage bootstrapReply) {
		// copy received fidget hosts to the list of fidget hosts
		// one of this hosts will be used by the Exchanger to initiate the first Exchange
		// if the local peer is in bootstrap phase => use the replication and construction delays
		mBootstrapper.newBootstrapReply(bootstrapReply);
		synchronized(mDataLock) {
			if (mPhase == PHASE_BOOTSTRAP) {
				long currentTime = System.currentTimeMillis();
				if (mPGridP2P.isSuperPeer()) {
					
					mConsructionStartTime = currentTime + bootstrapReply.getConstructionDelay();
					mReplicationStartTime = currentTime + bootstrapReply.getReplicationDelay();
					
					//Constants.LOGGER.config("Receiving BootstrapReply. Setting BOOTSTRAP_CONSTRUCTION_DELAY to " + mConsructionStartTime + " and BOOTSTRAP_REPLICATION_DELAY to " + mReplicationStartTime);
					
					// check if we should do a dynamic join or if the network is not constructed
					if (bootstrapReply.getConstructionDelay() == 0 &&
							bootstrapReply.getReplicationDelay() == 0 &&
							mPGridP2P.propertyBoolean(Properties.DYNAMIC_JOIN)) {
						// so we missed the party :'(

						// create a good fidget list and try to clone an overloaded peer
						mDynamicJoining = true;
						mBalancer.addListener(this);
					}
				} else {
					mReplicationStartTime = currentTime + Constants.BOOTSTRAP_REPLICATION_DELAY;
					mConsructionStartTime = currentTime + Constants.BOOTSTRAP_CONSTRUCTION_DELAY;
				}
			}

			// if the current peer has bootstrapped, start the fidget exchange phase if the local peer is a
			// super peer or fall back into an idle state.
			if (mBootstrapper.hasBootstrapped()) {
				mPhase = PHASE_FIDGET_EXCHANGE;
				if (mPGridP2P.isInMonitoredMode())
					MonitoringManager.sharedInstance().reportPhase(PHASE_FIDGET_EXCHANGE);
			}

		}
	}

	/**
	 * Invoked when a new search path was received.
	 *
	 * @param host       the sending host.
	 * @param searchPath the search path message.
	 */
	public void newSearchPath(PGridHost host, SearchPathMessage searchPath) {
		mBalancer.remoteSearchPath(host, searchPath);
	}

	/**
	 * Resumes the replication balancer.
	 * This method is invoked after each exchange.
	 */
	public void replicationBalance() {
		// TODO: find a way to avoid load balancing when peer is doing an exchange
	}


	/**
	 * Returns the load balancer.
	 *
	 * @return the load balancer.
	 */
	public ReplicationBalancer getBalancer() {
		return mBalancer;
	}


	/**
	 * Processes a new fidget exchange request request.
	 *
	 * @param exchange the addBootstrapHost request.
	 */
	public void newFidgetExchangeRequest(FidgetExchangeMessage exchange) {
		mBootstrapper.newFidgetExchangeRequest(exchange);
	}

		/**
	 * Processes a new addBootstrapHost response.
	 *
	 * @param exchangeReply the addBootstrapHost response.
	 */
	public void newFidgetExchangeReply(FidgetExchangeReplyMessage exchangeReply) {
		mBootstrapper.newFidgetExchangeReply(exchangeReply);
	}

	/**
	 * Processes a new exchange invitation request.
	 *
	 * @param exchangeInvitation the exchange invitation request.
	 */
	public void newExchangeInvitation(ExchangeInvitationMessage exchangeInvitation) {
		// if we are not a super peer, short cut exchanger
		if (!mPGridP2P.isSuperPeer()) {
			mPGridP2P.send(exchangeInvitation.getHeader().getHost(), new ACKMessage(exchangeInvitation.getGUID(), ACKMessage.CODE_NOT_SUPERPEER));
		} else {
			mExchanger.newExchangeInvitation(exchangeInvitation);
		}
	}

	/**
	 * Processes a new exchange request.
	 *
	 * @param exchange the exchange request.
	 */
	public void newExchangeRequest(ExchangeMessage exchange) {
		if (!mPGridP2P.isSuperPeer()) {
			mPGridP2P.send(exchange.getHeader().getHost(), new ACKMessage(exchange.getGUID(), ACKMessage.CODE_NOT_SUPERPEER));
		} else {
			mExchanger.newExchangeRequest(exchange);
		}
	}

	/**
	 * A new exchange response was received.
	 *
	 * @param message the response message.
	 */
	public void newExchangeReply(ExchangeReplyMessage message) {
		if (!mPGridP2P.isSuperPeer()) {
			mPGridP2P.send(message.getHeader().getHost(), new ACKMessage(message.getGUID(), ACKMessage.CODE_NOT_SUPERPEER));
		} else {
			mExchanger.newExchangeReply(message);
		}
	}

	public void newExchangeIndexEntriesMessage(ExchangeIndexEntriesMessage msg) {
		if (mExchanger != null)
			mExchanger.newExchangeIndexEntriesMessage(msg);
	}

	public void newExchangeCSVIndexEntriesMessage(ExchangeCSVIndexEntriesMessage msg) {
		if (mExchanger != null)
			mExchanger.newExchangeCSVIndexEntriesMessage(msg);
	}

	/**
	 * Invoked when a new replicate request was received.
	 *
	 * @param host      the requesting host.
	 * @param message	the replication message.
	 */
	public void newReplicateRequest(PGridHost host, ReplicateMessage message) {
		if (!mPGridP2P.isSuperPeer()) {
			mPGridP2P.send(message.getHeader().getHost(), new ACKMessage(message.getGUID(), ACKMessage.CODE_NOT_SUPERPEER));
		} else {
			mReplicator.replicateRequest(host, message.getIndexEntries());
		}
	}
	
	protected void prepareWorker() throws Exception {
		mThread = Thread.currentThread();
		Constants.LOGGER.config("Maintenance thread prepared.");
	}

	/**
	 * Invites one of the delivered hosts for an exchange.
	 *
	 * @param hosts     the hosts.
	 * @param recursion the recursion value.
	 * @param lCurrent  the current common length.
	 */
	public void randomExchange(Collection hosts, int recursion, int lCurrent) {
		if (mPGridP2P.isSuperPeer())
			mExchangeInitiator.randomExchange(hosts, recursion, lCurrent);
	}

	protected void releaseWorker() throws Exception {
		Constants.LOGGER.config("Maintenance thread released.");
	}

	/**
	 * Sets if PGridP2P should automatically initiate Exchanges.
	 *
	 * @param flag automatically initiate, or not.
	 */
	public void setInitExchanges(boolean flag) {
		if (PGridP2P.sharedInstance().isInTestMode()) {
			if (flag) {
				mPGridP2P.getStatistics().InitExchanges = 1;
			} else {
				mPGridP2P.getStatistics().InitExchanges = 0;
			}
		}

		if (flag == mPGridP2P.propertyBoolean(pgrid.Properties.INIT_EXCHANGES))
			return;

		mPGridP2P.setProperty(pgrid.Properties.INIT_EXCHANGES, Boolean.toString(flag));
	}

	/**
	 * Pause, unpause worker
	 * @param cond
	 */
	public void setReady(boolean cond) {
		isCondition = cond;
	}

	/**
	 * Consumer in Producer/consumer design pattern.
	 * @throws Exception
	 */
	protected void work() throws Exception {

		// perform arbitrary maintenance task first if any
		Collection<Runnable> runnables = null;
		synchronized(mArbitraryMaintenanceTask) {
			runnables = (Vector<Runnable>) mArbitraryMaintenanceTask.clone();
			mArbitraryMaintenanceTask.clear();
		}                                                                                     	
		for (Runnable task: runnables) {
			task.run();
		}
		
		// determine the current phase
		short phase;
		long currentTime = System.currentTimeMillis();
		synchronized(mDataLock) {
			if (mDynamicJoining && mPGridP2P.isSuperPeer()) {
				if (currentTime >= mNextDynamicJoin) {
					// try to join
					mPhase = PHASE_DYNAMIC_JOIN;
					if (mPGridP2P.isInMonitoredMode())
						MonitoringManager.sharedInstance().reportPhase(PHASE_DYNAMIC_JOIN);
					mNextDynamicJoin = currentTime+DYNAMIC_JOIN_TIMEOUT;
				} else if (mPhase == PHASE_DYNAMIC_JOIN) {
					// nothing should be done before joining
					return;
				}
			} else if (mPhase != PHASE_BOOTSTRAP) {                                        	
				if (currentTime >= mConsructionStartTime) {
					if (mPGridP2P.isSuperPeer()){
						mPhase = PHASE_RUN;
						if (mPGridP2P.isInMonitoredMode())
							MonitoringManager.sharedInstance().reportPhase(PHASE_RUN);
					} else {
						mPhase = PHASE_SLEEPS;
						if (mPGridP2P.isInMonitoredMode())
							MonitoringManager.sharedInstance().reportPhase(PHASE_SLEEPS);
					}
				} else if (currentTime >= mReplicationStartTime) {
					mPhase = PHASE_RUN;
					if (mPGridP2P.isInMonitoredMode())
						MonitoringManager.sharedInstance().reportPhase(PHASE_RUN);
				}
			}
			phase = mPhase;
		}

		if (PGridP2P.sharedInstance().isInTestMode())
			mPGridP2P.getStatistics().Phase = phase;


		if (phase == PHASE_SLEEPS) {
			// If we are a client, check if we should become a super peer

		} else if (phase == PHASE_BOOTSTRAP) {
			
			MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_MTNCE_MGR_BOOTSTRAP);
			mBootstrapper.bootstrap();
			MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_MTNCE_MGR_BOOTSTRAP);
			
		} else if (phase == PHASE_FIDGET_EXCHANGE) {
			
			MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_MTNCE_FIDGET_EXCH);
			mBootstrapper.fidgetExchange();
			MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_MTNCE_FIDGET_EXCH);
			
		} else if (phase == PHASE_DYNAMIC_JOIN) {
			
			MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_MTNCE_CLONE_RND_PEER);
			mBalancer.cloneRandomPeer(mPGridP2P.getRoutingTable().getFidgetVector());
			MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_MTNCE_CLONE_RND_PEER);
			
		} else if (phase == PHASE_REPLICATE) {
			if (mPGridP2P.isSuperPeer()){
				MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_MTNCE_REPLICATE);
				mReplicator.replicate();
				MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_MTNCE_REPLICATE);
				
			} else {
				if (mReplicationCount == 0) {
					
					MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_MTNCE_POPULATE_DATA);
					populateData();
					MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_MTNCE_POPULATE_DATA);
					
					mReplicationCount++;
				}
				synchronized(mDataLock) {
					mPhase = PHASE_SLEEPS;
					if (mPGridP2P.isInMonitoredMode())
						MonitoringManager.sharedInstance().reportPhase(PHASE_SLEEPS);
				}
			}
		} else if (phase == PHASE_RUN && mPGridP2P.propertyBoolean(Properties.INIT_EXCHANGES) && mExchanger.isIdle()){
			if (mExchanger.getMinStorage() == 0) {
				int minStorage;

//				minStorage = IndexManager.getInstance().getPredictionSubset().count()*Constants.REPLICATION_FACTOR;
				minStorage = Constants.PREDICTION_SUBSET_SIZE*Constants.REPLICATION_FACTOR;
				
				mExchanger.setMinStorage(minStorage);
			}
			mExchangeInitiator.inviteHost(mExchanger.getMinStorage());
		}

		// wait a bit
		try{
			synchronized (getLock()) {
				getLock().wait(getTimeout());
			}
		} catch(InterruptedException e) {
			handleError(e);
			halt();
		}

	}

	/**
	 * Propagate local data items into the network of super peers.
	 */
	private void populateData() {
		mPGridP2P.getIndexManager().propagateAllLocalIndexes();
	}

	/**
	 * Shutdown
	 */
	public void shutdown() {
		setInitExchanges(false);
		mExchanger.shutdown();
		if (mThread != null) mThread.interrupt();
	}

	// reset the maintenance manager
	public void reset() {
		long currentTime = System.currentTimeMillis();
		synchronized(mDataLock) {
			mPhase = PHASE_BOOTSTRAP;
			if (mPGridP2P.isInMonitoredMode())
				MonitoringManager.sharedInstance().reportPhase(PHASE_BOOTSTRAP);
			mExchanger.shutdown();
			mReplicationStartTime = currentTime + Constants.BOOTSTRAP_REPLICATION_DELAY;
			mConsructionStartTime = currentTime + Constants.BOOTSTRAP_CONSTRUCTION_DELAY;
			mPGridP2P.setProperty(Properties.REPLICATION_START_TIME, Long.toString(mReplicationStartTime));
			mPGridP2P.setProperty(Properties.CONSTRUCTION_START_TIME, Long.toString(mConsructionStartTime));
		}

		// Exchanger
		mExchanger = new Exchanger(mPGridP2P, this);
		mExchanger.setMinStorage(mPGridP2P.propertyInteger(Properties.EXCHANGE_MIN_STORAGE));
		createExchangerThread();
		mExchangerThread.start();
	}

	/**
	 * Returns true if it is time for exchanges
	 */
	public boolean isExchangeTime() {
		synchronized(mDataLock) {
			return (mPhase == PHASE_RUN);
		}
	}

	/**
	 * Invoked when data items were added to the data table.
	 *
	 * @param items the added data item.
	 */
	public void indexItemsAdded(Collection items) {

	}

	/**
	 * Invoked when data items were removed from the data table.
	 *
	 * @param items the removed data item.
	 */
	public void indexItemsRemoved(Collection items) {
		/*int minStorage;

		if (mPGridP2P.getLocalPath().length() == 0)
			minStorage = mExchanger.getMinStorage()-items.size()*Constants.REPLICATION_FACTOR;
		else
			minStorage = mExchanger.getMinStorage()-items.size();

		mExchanger.setMinStorage(minStorage); */
	}

	/**
	 * Invoked when data items were updated from the data table.
	 *
	 * @param items the removed data item.
	 */
	public void indexItemsUpdated(Collection items) {
		//Nothing to do here
	}

	/**
	 * Invoked when the data table is cleared.
	 */
	public void indexTableCleared() {
		mExchanger.setMinStorage(0);
	}
	
	/**
	 * This class is called when the local host change its path
	 *
	 * @param path			the new path of this peer
	 * @param minStorage	MinStorage constant of the replica
	 */
	public void localPathChanged(String oldPath, String path, int minStorage) {
		if (mExchanger.getMinStorage() == 0) {

			mExchanger.setMinStorage(minStorage);
		}

		mDynamicJoining = false;
		if (mPhase == PHASE_RUN)
			mPGridP2P.setInitExchanges(true);
	}

	/**
	 * This method is called when the cloning process failed.
	 */
	public void cloningFailed() {
		mNextDynamicJoin = System.currentTimeMillis();
		broadcast();
	}

	/**
	 * Returns the minimum storage before a split occures
	 */
	public int getMinStorage() {
		return mExchanger.getMinStorage();
	}

	/**
	 * Returns the exchanger lock
	 * @return the exchanger lock
	 */
	public Object getExchangerLock() {
		return mExchanger.getExchangerLock();
	}

	/**
	 * Set the super peer status of this node
	 * @param flag the new status.
	 */
	public void setSuperPeerFlag(boolean flag) {
		mArbitraryMaintenanceTask.add(new SuperPeerSwitcher(flag));
		broadcast();
	}

	/**
	 * This method will remove a host from the routing table.
	 * @param host host to be removed
	 */
	public void removeRemotePeer(PGridHost host) {
		mArbitraryMaintenanceTask.add(new HostRemover(host));
		broadcast();
	}

	public void newMessage(PGridMessage msg, p2p.basic.GUID guid) {
		if (msg instanceof ACKMessage) {
			int code = ((ACKMessage)msg).getCode();
			if (code == ACKMessage.CODE_NOT_SUPERPEER) {
				removeRemotePeer(msg.getHeader().getHost());
			}
		}
	}

	/**
	 * This method is called when a maintenance message arrives to the local host
	 * @param host
	 * @param maintenanceMessage
	 */
	public void newMaintenanceMessage(PGridHost host, MaintenanceMessage maintenanceMessage) {
		if (maintenanceMessage.getCode() == MaintenanceMessage.CODE_NOT_SUPERPEER) {
			removeRemotePeer(host);
		}
	}

	private void createExchangerThread() {
		mExchangerThread = new Thread(mExchanger, "Exchanger");
		mExchangerThread.setDaemon(true);
		mExchangerThread.setPriority(Constants.LOW_PRIORITY);
	}

	/**
	 * Runnable used to remove a host from routing table
	 */
	protected class HostRemover implements Runnable{
		private PGridHost mHost;

		HostRemover(PGridHost host) {
			mHost = host;
		}

		public void run() {
			RoutingTable rt = mPGridP2P.getRoutingTable();
			// reset Routing table and index table
			rt.remove(mHost);

			// check if the removal of this host produced an empty level
			if (rt.hasEmptyLevels()) {
				// reinitiate exchange
				setInitExchanges(true);
			}

			Constants.LOGGER.finer("Host "+mHost.toHostString()+" not a super peer anymore, removing it from routing table.");
		}
	}                                                                                        	 

	/**
	 * Runnable used to remove a host from routing table
	 */
	protected class SuperPeerSwitcher implements Runnable{
		private boolean mFlag;

		SuperPeerSwitcher(boolean flag) {
			mFlag = flag;
		}

		public void run() {
			boolean superpeer = mPGridP2P.isSuperPeer();

			if (superpeer == mFlag) {
				Constants.LOGGER.finest("Super status is the same as before (" + superpeer + "): returning.");
				return;
			}

			if (superpeer && !mFlag) {
				// stop exchange thread
				if (mPhase == PHASE_RUN) {
					mExchanger.stopExchanger();
				}

				// send a message to all replicas that this peer is leaving super peer set
				MessageManager.sharedInstance().sendToReplicas(new MaintenanceMessage(MaintenanceMessage.CODE_NOT_SUPERPEER), null);
				                                           	
				// wait a bit to ensure message has been sent
				synchronized(Thread.currentThread()) {
					try {
						Thread.currentThread().wait(1000);
					} catch (InterruptedException e) {
						// do nothing
					}
				}

				// reset Routing table and index table
				mPGridP2P.getRoutingTable().clear();

				// reset dynamic join
				mBalancer.resetStatistics();

				// reset the bootstratp status
				synchronized(mDataLock) {
					mPhase = PHASE_BOOTSTRAP;
					if (mPGridP2P.isInMonitoredMode())
						MonitoringManager.sharedInstance().reportPhase(PHASE_BOOTSTRAP);
				}
				mBootstrapper.resetBootstrapFlag();

				// reset path
				mPGridP2P.setLocalPath("");


				mPGridP2P.setSuperPeerFlag(mFlag);
			} else {
				mPGridP2P.setSuperPeerFlag(mFlag);
				
				// we are a client and we want to become a super peer.
				synchronized(mDataLock) {
					mPhase = PHASE_BOOTSTRAP;
					if (mPGridP2P.isInMonitoredMode())
						MonitoringManager.sharedInstance().reportPhase(PHASE_BOOTSTRAP);
				}
				mBootstrapper.resetBootstrapFlag();

				// start exchanger thread.
				if (mExchangerThread == null) {
					createExchangerThread();
					mExchangerThread.start();
				}
				mExchanger.startExchanger();

				// if we have already join the network as a client, re-join the network as a super peer
				if (mPGridP2P.hasJoined())
					join();
			}

		}
	}

	public Exchanger getExchanger() {
		return mExchanger;
	}
}