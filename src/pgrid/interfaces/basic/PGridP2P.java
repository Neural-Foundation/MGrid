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

package pgrid.interfaces.basic;

import p2p.basic.Key;
import p2p.basic.Message;
import p2p.basic.Peer;
import p2p.basic.KeyRange;
import p2p.basic.P2P;
import p2p.basic.events.P2PListener;
import pgrid.core.search.SearchManager;
import pgrid.core.index.IndexManager;
import pgrid.core.maintenance.identity.IdentityManager;
import pgrid.core.*;
import pgrid.core.maintenance.*;
import pgrid.network.*;
import pgrid.network.lookup.LookupManager;
import pgrid.network.generic.GenericManager;
import pgrid.network.router.Router;
import pgrid.network.protocol.*;
import pgrid.*;
import pgrid.util.PathComparator;
import pgrid.util.Tokenizer;
import pgrid.util.Utils;
import pgrid.util.TimerManager;
import pgrid.network.NaFTManager;
import java.util.*;
import java.net.UnknownHostException;
import java.net.InetAddress;
import pgrid.Properties;

/**
 * This class represents the PGridP2P facility.
 * It is responsible for all activities in the Gridella network (search,
 * exchange).
 * This class implements the <code>Singleton</code> pattern as defined by
 * Gamma et.al. As there could only exist one instance of this class, other
 * clients must use the <code>sharedInstance</code> function to use this class.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class PGridP2P implements P2P {

	/**
	 * The key for the debug level used to initialize the P-Grid facility.
	 */
	public static final String PROP_DEBUG_LEVEL = "DebugLevel";

	/**
	 * The key for the local port used to initialize the P-Grid facility.
	 */
	public static final String PROP_LOCAL_PORT = "LocalPort";

	/**
	 * The key for the log file used to initialize the P-Grid facility.
	 */
	public static final String PROP_LOG_FILE = "LogFile";

	/**
	 * The key for the property file used to initialize the P-Grid facility.
	 */
	public static final String PROP_PROPERTY_FILE = "PropertyFile";

    /**
     * The key for SSL Socket usage to initialize PGridSSL.
     */
  //  public static final String PROP_USE_SSLSOCKETS = "UseSSLSockets";

    /**
	 * The key for the start listener flag used to initialize the P-Grid facility.
	 */
	public static final String PROP_START_LISTENER = pgrid.Properties.START_LISTENER;

	/**
	 * The key for the verbose mode used to initialize the P-Grid facility.
	 */
	public static final String PROP_VERBOSE_MODE = "VerboseMode";

	/**
	 * The key for the replication factor.
	 */
	public static final String PROP_REPLICATION_FACTOR = "ReplicationFactor";


	/**
	 * The reference to the only instance of this class (Singleton
	 * pattern). This differs from the C++ standard implementation by Gamma
	 * et.al. since Java ensures the order of static initialization at runtime.
	 *
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	private static final PGridP2P SHARED_INSTANCE = new PGridP2P();

	/**
	 * The list of bootstrap hosts used to join the network at the first time.
	 */
	private Vector mBootstrapHosts = new Vector();

	/**
	 * The Data Manager.
	 */
	private ConnectionManager mConnManager = null;

	/**
	 * Debug flag
	 */
	private boolean mDebugMode = false;
	
	/**
	 * Monitored flag
	 */
	private boolean mMonitoredMode = false;

	/**
	 * True if the local peer has join the network
	 */
	private boolean mHasJoin = false;
	
	/**
	 * The Identity manager.
	 */
	private IdentityManager mIdentMgr = null;

	/**
	 * Number of time P-Grid was initialized
	 */
	private int numInit = 0;

	/**
	 * The Maintencance Manager.
	 */
	private MaintenanceManager mMaintencanceMgr = null;

	/**
	 * The Message Manager.
	 */
	private MessageManager mMsgManager = null;

	/**
	 * The Message mapper.
	 */
	private PGridMessageMapping mMsgMapper = PGridMessageMapping.sharedInstance();

	/**
	 * Lookup manager
	 */
	private LookupManager mLookupManager;

	/**
	 * The generic manager
	 */
	private GenericManager mGenericManager;

	/**
	 * The NAT and Firewall Traversal manager
	 */
	private NaFTManager mNaFTManager;
	
	/**
	 * The Download Manager
	 */
	private DownloadManager mDownloadManager;
	
	/**
	 * The PGridP2P indexing tree.
	 */
	private PGridTree mPGridTree = new PGridTree();

	/**
	 * The application properties.
	 */
	private pgrid.Properties mProperties = new pgrid.Properties();

	/**
	 * The PGrid router.
	 */
	private Router mRouter = null;

	/**
	 * The routing table.
	 */
	private LocalRoutingTable mRoutingTable = null;

	/**
	 * The search manager.
	 */
	private SearchManager mSearchManager = SearchManager.sharedInstance();

	/**
	 * IF this peer should start listening.
	 */
	private boolean mStartListener = true;

	/**
	 * True if the local peer is a super peer
	 */
	private boolean mIsSuperPeer = false;

	/**
	 * The data item manager.
	 */
	private IndexManager mIndexManager = null;

	/**
	 * The P-Grid statistics.
	 */
	private Statistics mStatistics = null;

	/**
	 * Test flag
	 */
	private boolean mTestMode = false;

    /**
	 * Uptime
	 */
	private long mUptime = 0;

	/**
	 * List of listeners to messages
	 */
	private Vector listeners = new Vector();


	/**
	 * The constructor must be protected to ensure that only subclasses can
	 * call it and that only one instance can ever get created. A client that
	 * tries to instantiate PGridP2P directly will get an error at compile-time.
	 */
	protected PGridP2P() {
	}

	/**
	 * This creates the only instance of this class. This differs from the C++ standard implementation by Gamma et.al.
	 * since Java ensures the order of static initialization at runtime.
	 *
	 * @return the shared instance of this class.
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	public static PGridP2P sharedInstance() {
		return SHARED_INSTANCE;
	}

	/**
	 * Initializes the P-Grid facility with the given properties.
	 *
	 * @param properties further initialization properties.
	 */
	synchronized public void init(java.util.Properties properties) {


		numInit++;
        // check if it has already been initialized
		if (numInit > 1) {
			Constants.LOGGER.warning("Several initializations: P-Grid was initialized " + numInit + " times.");
			Constants.LOGGER.warning("Deleting old routing table ...");
			mMaintencanceMgr.reset();
			mRoutingTable.clear();
			mRoutingTable.addFidget(getLocalHost());
			mRoutingTable.save();
		}
		
		// use constants for properties
		int debugLevel = -1;
		int localPort = Constants.DEFAULT_PORT;
		String logFile = Constants.LOG_FILE;
		String propFile = Constants.PROPERTY_FILE;
		boolean verboseMode = false;

		// get user properties for the logger
		if (properties != null) {
			if (properties.containsKey(PROP_DEBUG_LEVEL))
				debugLevel = Integer.parseInt(properties.getProperty(PROP_DEBUG_LEVEL));
			if (properties.containsKey(PROP_LOCAL_PORT))
				localPort = Integer.parseInt(properties.getProperty(PROP_LOCAL_PORT));
			if (properties.containsKey(PROP_LOG_FILE))
				logFile = properties.getProperty(PROP_LOG_FILE);
			if (properties.containsKey(PROP_PROPERTY_FILE))
				propFile = properties.getProperty(PROP_PROPERTY_FILE);
			if (properties.containsKey(PROP_START_LISTENER))
				mStartListener = Boolean.valueOf(properties.getProperty(PROP_START_LISTENER));
			if (properties.containsKey(PROP_VERBOSE_MODE))
				verboseMode = Boolean.valueOf(properties.getProperty(PROP_VERBOSE_MODE));
			if (properties.containsKey(PROP_REPLICATION_FACTOR))
				Constants.REPLICATION_FACTOR = Integer.parseInt(properties.getProperty(PROP_REPLICATION_FACTOR));		
		}

        // init logging facility
		Constants.initLogger(null, debugLevel, verboseMode, logFile);
		Constants.LOGGER.info("starting P-Grid " + Constants.VERSION + " ...");

		// load the properties from the property file and override them by the given properties
		mProperties.init(propFile, properties);
		
		// FIXME: commented out for testing purpose. Please uncomment it before production
		validateProperties();

        //init the PGridSocket class by passing whether to use SSL sockets or not
        PGridSocket.sharedInstance().init();
        
		// Message mapping
		Constants.LOGGER.config("starting Message mapper facility ...");
		mMsgMapper.init(this.propertyString(Properties.MESSAGE_MAPPING));

		// create the statistic facility
		mStatistics = new Statistics();

		// set modes
		mDebugMode = propertyBoolean(Properties.DEBUG_MODE);
		mTestMode = propertyBoolean(Properties.TEST_MODE);
		mIsSuperPeer = propertyBoolean(Properties.SUPER_PEER);
		mMonitoredMode = propertyBoolean(Properties.MONITORED_MODE);
		
		// init the statistics
		if (isInTestMode())
			mStatistics.init();

		mConnManager = ConnectionManager.sharedInstance();

		// Router
		mRouter = new Router();

		// Message manager
		mMsgManager = MessageManager.sharedInstance();

		// PGrid Routing Table
		Constants.LOGGER.config("initializing P-Grid Routing Table ...");
		mRoutingTable = new LocalRoutingTable(Constants.DATA_DIR+propertyString(pgrid.Properties.ROUTING_TABLE), localPort);
		
		// set local host properties
		getLocalHost().setSpeed(propertyInteger(pgrid.Properties.CONNECTION_SPEED));

		// Storage Manager
		Constants.LOGGER.config("starting Storage Manager ...");
		mIndexManager = IndexManager.getInstance();


		// Message Manager
		Constants.LOGGER.config("starting Message Manager ...");
		mMsgManager.init();

		// Message mapping
		Constants.LOGGER.config("registering remote message handlers ...");
		mMsgMapper.registerRemoteHandler();

		// Maintenance Manager
		mMaintencanceMgr = new MaintenanceManager(this);
		
		
		// NaFT Manager
		Constants.LOGGER.config("starting NaFT Message Manager ...");
		mNaFTManager = new NaFTManager(this);
		
		// Download Manager
		mDownloadManager = new DownloadManager(this);
		
		// Generic Message manager
		Constants.LOGGER.config("starting Generic Message Manager ...");
		mGenericManager = new GenericManager();
		
		// Lookup Manager
		Constants.LOGGER.config("starting Lookup Manager ...");
		mLookupManager = new LookupManager();


		// PGrid tree
		Constants.LOGGER.config("starting P-Grid Mapping Tree ...");
		mPGridTree.init();

		// Search Manager
		Constants.LOGGER.config("starting Search Manager ...");
		mSearchManager.init();
		Thread searchThread = new Thread(mSearchManager, "Search Manager");
		searchThread.setDaemon(true);
		searchThread.start();

		// Start all network specific managers of P-Grid
		_init();

        Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				shutdown();
			}
		});
	}

	/**
	 * Validate properties to avoid conflicting entries
	 */
	private void validateProperties() {
	}

	/**
	 * 	Start all network specific managers of P-Grid
	 */
	protected void _init() {
		Thread low = new Thread(LowPriorityMessageManager.sharedInstance(), "Low priority message Manager");
		low.setDaemon(true);
		low.setPriority(Constants.LOW_PRIORITY);
		low.start();

		// Connection Manager
		mConnManager.init(mStartListener);

		// maintenance manager
		Constants.LOGGER.config("starting Maintenance Manager ...");
		mMaintencanceMgr.init();
		Thread maintenanceThread = new Thread(mMaintencanceMgr, "Maintenance Manager");
		maintenanceThread.setDaemon(true);
		maintenanceThread.start();

		// Router
		mRouter.activate();

		// Generic Message Manager
		mGenericManager.init();

		// NaFT Message Manager
		mNaFTManager.init();
		
	}

  /**
   * Checks if the local peer is responsible for the given key.
   *
   * @param key the key to check.
   * @return <code>true</code> if the local peer is responsible, <code>false</code> otherwise.
   */
  public boolean isLocalPeerResponsible(Key key) {
	if (key == null)
	  throw new NullPointerException();

	String compath = Utils.commonPrefix(key.toString(), getLocalPath());
	if ((compath.length() == key.size()) || (compath.length() == getLocalPath().length()) || getLocalPath().length() == 0)
	  return true;
	else
	  return false;
  }

  /**
   * Checks if the local peer is responsible for the given key range.
   *
   * @param key the key range to check.
   * @return <code>true</code> if the local peer is responsible for at least a part of the range, <code>false</code> otherwise.
   */
  public boolean isLocalPeerResponsible(KeyRange key) {
	  if (key == null)
		  throw new NullPointerException();

	  PathComparator pathComparator = new PathComparator();
	  return getLocalPath().length() == 0 ||
			  ((pathComparator.compare(getLocalPath(), key.getMin().toString()) >= 0) &&
					  (pathComparator.compare(getLocalPath(), key.getMax().toString()) <= 0));
  }

	/**
	 * Joins the network.
	 */
	public void join() {
		mMaintencanceMgr.join();
		mHasJoin = true;
		mUptime = System.currentTimeMillis();
	}

	/**
	 * @see p2p.basic.P2P#join(p2p.basic.Peer)
	 */
	public void join(Peer peer) {
		if (peer == null)
			throw new NullPointerException();


		PGridHost host;

		// check if the peer is equal to the local peer
		if (peer instanceof PGridHost)
			host = (PGridHost) peer;
		else host = PGridHost.toPGridHost(peer);

		// bootstrap with the given host
		mMaintencanceMgr.bootstrap(host);
		mHasJoin = true;
		mUptime = System.currentTimeMillis();
	}

	/**
	 * @see p2p.basic.P2P#leave()
	 */
	public void leave() {
		gracefulShutdown("Leaving ...");
	}

	/**
	 * @see p2p.basic.P2P#lookup(p2p.basic.Key, long timeout)
	 */
	public Peer lookup(Key key, long timeout) {
		PeerLookupMessage msg = new PeerLookupMessage(
				key.toString(),
				PGridP2P.sharedInstance().getLocalHost());

		return mLookupManager.synchronousPeerLookup(msg, timeout);
	}

	/**
	 * @see p2p.basic.P2P#route(p2p.basic.Key, p2p.basic.Message)
	 */
	public void route(Key key, Message message) {
		// set message destination key and let PGridP2P route it
		mMsgManager.route(key, (PGridMessage) message, null, null);
	}

	/**
	 * @see p2p.basic.P2P#route(p2p.basic.Key[], p2p.basic.Message[])
	 */
	public void route(Key[] keys, Message[] message) {

	}

	/**
	 * @see p2p.basic.P2P#route(p2p.basic.KeyRange, p2p.basic.Message)
	 */
	public void route(KeyRange range, Message message) {
		mMsgManager.route(range, (PGridMessage) message, null, null);
	}

	/**
	 * @see p2p.basic.P2P#send(p2p.basic.Peer, p2p.basic.Message)
	 */
	public void send(Peer peer, Message message) {
		mMsgManager.sendMessage((PGridHost)peer, (PGridMessage)message, null);
	}

	/**
	 * @see p2p.basic.P2P#routeToReplicas(p2p.basic.Message)
	 */
	public void routeToReplicas(Message message) {
		mMsgManager.sendToReplicas((PGridMessage)message, null);
	}

	/**
	 * @see p2p.basic.P2P#getLocalPeer()
	 */
	public Peer getLocalPeer() {
		return getLocalHost();
	}

	/**
	 * @see p2p.basic.P2P#getNeighbors()
	 */
	public Peer[] getNeighbors() {
		RoutingTable rt = getRoutingTable();
		List neighbors = rt.getAllReferences();

		return (Peer[])neighbors.toArray(new Peer[neighbors.size()]);
	}

	/**
	 * Returns the uptime in millisecond
	 *
	 * @return the uptime in millisecond
	 */
	public long getUpTime() {
		return (mUptime==0?0:System.currentTimeMillis()-mUptime);
	}

	/**
	 * Register an object to be notified of new messages.
	 *
	 * @param listener the P2PListener implementation to register
	 */
	public void addP2PListener(P2PListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove registration of a current listener of new messages.
	 *
	 * @param listener the P2PListener implementation to unregister
	 */
	public void removeP2PListener(P2PListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Handles a new received generic message.
	 *
	 * @param generic the generic message.
	 * @param origin the originating host.
	 */
	public void newGenericMessage(GenericMessage generic, Peer origin) {
		for (Iterator it = listeners.iterator(); it.hasNext();) {
			P2PListener listener = (P2PListener)it.next();
			listener.newMessage(generic, origin);
		}
	}

	/**
	 * Returns the P-Grid lookup Manager.
	 * @return the lookup Manager.
	 */
	public LookupManager getLookupManager() {
		return mLookupManager;
	}

	/**
	 * Returns the P-Grid Maintenance Manager.
	 * @return the Maintenance Manager.
	 */
	public MaintenanceManager getMaintenanceManager() {
		return mMaintencanceMgr;
	}

	/**
	 * Returns the P-Grid Search Manager.
	 * @return the Search Manager.
	 */
	public SearchManager getSearchManager() {
		return mSearchManager;
	}

	/**
	 * Returns the P-Grid Storage Manager.
	 * @return the Storage Manager.
	 */
	public IndexManager getIndexManager() {
		return mIndexManager;
	}

	/**
	 * Returns the P-Grid Generic Manager.
	 * @return the Generic Manager.
	 */
	public GenericManager getGenericManager() {
		return mGenericManager;
	}

	/**
	 * Returns the P-Grid NaFT Manager.
	 * @return the NaFT Manager.
	 */
	public NaFTManager getNaFTManager() {
		return mNaFTManager;
	}
	
	public DownloadManager getDownloadManager(){
		return mDownloadManager;
	}
	
	/**
	 * Returns the local P-Grid router.
	 * @return the router.
	 */
	public Router getRouter() {
		return mRouter;
	}

	/**
	 * Checks if the given host is the local host.
	 * @param host the host to check.
	 * @return <tt>true</tt> if the host is the local host, <tt>false</tt> otherwise.
	 */
	public boolean isLocalHost(PGridHost host) {
		if (host.equals(getLocalHost())) {
			return true;
		}
		
		return false;
	}

	/**
	 * Invoked when a new challenge was received.
	 *
	 * @param host  the sending host.
	 * @param challenge the received challenge.
	 */
	public void newChallenge(PGridHost host, ChallengeMessage challenge) {
		Thread t = new Thread(new Challenger(host, challenge));
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Resets P-Grid by removing all hosts from the local routing table and all items from the local data table.
	 */
	public void reset() {
		Constants.LOGGER.info("reset P-Grid by clearing data table and routing table.");
		mMaintencanceMgr.reset();
		setLocalPath("");
		mIndexManager.getIndexTable().clear();
		mRoutingTable.clear();
		mIndexManager.getIndexTable().init((PGridHost)getLocalPeer());
		mRoutingTable.addFidget(getLocalHost());
		mRoutingTable.save();
	}

	/**
	 * Tests if PGridP2P automatically tries to initiate Exchanges.
	 *
	 * @return <code>true</code>, or <code>false</code>.
	 */
	public boolean getInitExchanges() {
		return propertyBoolean(pgrid.Properties.INIT_EXCHANGES);
	}

	/**
	 * Sets if PGridP2P should automatically initiate Exchanges.
	 *
	 * @param flag automatically initiate, or not.
	 */
	public void setInitExchanges(boolean flag) {
		if (flag == propertyBoolean(pgrid.Properties.INIT_EXCHANGES))
			return;
		if (flag) {
			setProperty(pgrid.Properties.INIT_EXCHANGES, Boolean.toString(true));
			mStatistics.InitExchanges = 1;
		} else {
			setProperty(pgrid.Properties.INIT_EXCHANGES, Boolean.toString(false));
			mStatistics.InitExchanges = 0;
		}
	}

	

	/**
	 * Returns the list of bootstrap hosts.
	 *
	 * @return the list of bootstrap hosts.
	 */
	public Vector getBootstrapHosts() {
		return mBootstrapHosts;
	}

	/**
	 * Updates the list of bootstrap hosts.
	 */
	public void updateBootstrapHosts() {

		mBootstrapHosts.clear();
		String[] hostStr = pgrid.util.Tokenizer.tokenize(propertyString(pgrid.Properties.BOOTSTRAP_HOSTS), ";");

		for (int i = 0; i < hostStr.length; i++) {
			String[] parts = Tokenizer.tokenize(hostStr[i], ":");
			PGridHost host = null;
			int port = Constants.DEFAULT_PORT;
			if (parts.length > 1) {
				try {
					port = Integer.parseInt(parts[1]);
				} catch (NumberFormatException e) {
					port = Constants.DEFAULT_PORT;
				}
			}
			try {
				host = (PGridHost)PGridP2PFactory.sharedInstance().createPeer(InetAddress.getByName(parts[0]), port);
			} catch (UnknownHostException e) {
			}
			try {
				host.resolve();
			} catch (UnknownHostException e) {
				Constants.LOGGER.warning("Unable to resolve bootstrap host's address: "+host.toHostString()+". ");
				continue;
			}

			if (((host.getAddressString() != null) && (host.getPort() > 0)) && (!((Host)host).equals(getLocalHost()))) {
				mMaintencanceMgr.bootstrap(host);
			}
		}

	}

	/**
	 * Returns the local host.
	 *
	 * @return the local host.
	 */
	public PGridHost getLocalHost() {
		return mRoutingTable.getLocalHost();
	}

	/**
	 * Returns the external IP of the peer
	 * @return external IP of the peer
	 */
	public InetAddress getExternalIP(){
		return mRoutingTable.getLocalHost().getExternalIP();
	}
	
	/**
	 * Returns the local path.
	 *
	 * @return the local path.
	 */
	public String getLocalPath() {
		if (mRoutingTable != null && mRoutingTable.getLocalHost() != null && mRoutingTable.getLocalHost().getPath() != null)
			return mRoutingTable.getLocalHost().getPath();
		return "n/a";
	}

	/**
	 * Sets the local path.
	 *
	 * @param path the local path.
	 */
	public synchronized void setLocalPath(String path) {
		if (getLocalPath().equals(path)) return;
		
		if (mRoutingTable != null) {
			mRoutingTable.getLocalHost().setPath(path);
			for (int i = mRoutingTable.getLevelCount() - 1; i >= path.length(); i--) {
				mRoutingTable.removeLevel(i);
			}
		}
		mStatistics.PathLength = path.length();
	}

	/**
	 * Returns the local PGridP2P Routing Table. The routing table is accessed by different thread concurrently, therefor
	 * when multiple access is needed, use acquierReadLock() and when the read sequence is finished, call releaseReadLock().
	 * It is capital that all lock are behing freed. To ensure this, use a try...finally section as followed:
	 * <br/>
	 * <code>
	 * RoutingTable rt = myPGridP2P.getRoutingTable();
	 * rt.acquireReadLock();
	 * try {
	 * 	....
	 * } finally {
	 * 	rt.releaseReadLock();
	 * }
	 * </code>
	 *
	 * @return the Routing Table.
	 */
	public LocalRoutingTable getRoutingTable() {
		return mRoutingTable;
	}

	/**
	 * Returns the P-Grid statistics.
	 *
	 * @return the P-Grid statistics.
	 */
	public Statistics getStatistics() {
		return mStatistics;
	}

	/**
	 * Returns the default property value as boolean.
	 *
	 * @param key the key of the property.
	 * @return the default value of the property.
	 */
	public boolean defaultPropertyBoolean(String key) {
		return mProperties.getDefaultBoolean(key);
	}

	/**
	 * Returns the default property value as integer.
	 *
	 * @param key the key of the property.
	 * @return the default value of the property.
	 */
	public int defaultPropertyInteger(String key) {
		return mProperties.getDefaultInteger(key);
	}

	/**
	 * Returns the default property value as string.
	 *
	 * @param key the key of the property.
	 * @return the default value of the property.
	 */
	public String defaultPropertyString(String key) {
		return mProperties.getDefaultString(key);
	}

	/**
	 * Returns the property value as boolean.
	 *
	 * @param key the key of the property.
	 * @return the value of the property.
	 */
	public boolean propertyBoolean(String key) {
		return mProperties.getBoolean(key);
	}

	/**
	 * Returns the property value as integer.
	 *
	 * @param key the key of the property.
	 * @return the value of the property.
	 */
	public int propertyInteger(String key) {
		return mProperties.getInteger(key);
	}

	/**
	 * Returns the property value as long.
	 *
	 * @param key the key of the property.
	 * @return the value of the property.
	 */
	public long propertyLong(String key) {
		return mProperties.getLong(key);
	}

	/**
	 * Returns the property value as string.
	 *
	 * @param key the key of the property.
	 * @return the value of the property.
	 */
	public String propertyString(String key) {
		return mProperties.getString(key);
	}

	/**
	 * Sets the property value by the delivered string.
	 *
	 * @param key   the key of the property.
	 * @param value the value of the property.
	 */
	public void setProperty(String key, String value) {
		mProperties.setString(key, value);
	}

	/**
	 * Shutdowns the P-Grid facility.
	 */
	synchronized protected void shutdown() {
		Constants.LOGGER.info("Shutdown P-Grid ...");
		TimerManager.sharedInstance().shutdown();
		LowPriorityMessageManager.sharedInstance().shutdown();
		ConnectionManager.sharedInstance().stopListening();
		if (mSearchManager != null)
			mSearchManager.shutdown();
		if (mMaintencanceMgr != null)
			mMaintencanceMgr.shutdown();
		if (mRouter != null)
			mRouter.shutdown();
		if (mRoutingTable != null)
			mRoutingTable.shutdown();
		if (mStatistics != null)
			mStatistics.shutdown();
		if (mIndexManager != null)
			mIndexManager.shutdown();		
	}
	
	synchronized public void gracefulShutdown(String msg) {
		shutdown();
		throw new RuntimeException("P-Grid will shutdown now: " + msg);
		
	}

	/**
	 * Returns true if this peer is a super peer.
	 * @return true if this peer is a super peer.
	 */
	public boolean isSuperPeer() {
		return mIsSuperPeer;
	}
    /**
	 * Returns true if the local peer has joined the network
	 * @return true if the local peer has joined the network
	 */
	public boolean hasJoined() {
		return mHasJoin;
	}

	/**
	 * Set the super peer status of this node
	 * @param newStatus the new status.
	 */
	public void setSuperPeerFlag(boolean newStatus) {
		mIsSuperPeer = newStatus;
		mMaintencanceMgr.setSuperPeerFlag(newStatus);
	}

	/**
	 * Set the behind firewall status of this node
	 * @param newStatus the new status.
	 */
	public void setFirewalledFlag(boolean newStatus) {
		if (getLocalHost().isBehindFirewall() != newStatus) {
			setProperty(Properties.BEHIND_FIREWALL, ""+newStatus);
			getLocalHost().setFirewalledStatus(newStatus);
			
			
			// FIXME: commented out for testing purpose. Please uncomment it before production
			if (newStatus) setSuperPeerFlag(false);
			
			mConnManager.setFirewallStatus(newStatus);
		}
	}


	/**
	 * True if this peer is in debug mode
	 * @return True if this peer is in debug mode
	 */
	public boolean isInDebugMode() {
		return mDebugMode;
	}

	/**
	 * True if this peer is in test mode
	 * @return True if this peer is in test mode
	 */
	public boolean isInTestMode() {
		return mTestMode;
	}
	
	/**
	 * True if this peer is in monitored mode
	 * @return True if this peer is in monitored mode
	 */
	public boolean isInMonitoredMode() {
		return mMonitoredMode;
	}

	/**
	 * reset preferences
	 */
	public void clearProperties() {
		mProperties.clearProperties();

		// set modes
		mDebugMode = propertyBoolean(Properties.DEBUG_MODE);
		mTestMode = propertyBoolean(Properties.TEST_MODE);

	}
}