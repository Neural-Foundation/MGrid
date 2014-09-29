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

package pgrid.network.router;

import p2p.basic.*;
import p2p.basic.events.NoRouteToKeyException;
import pgrid.Constants;
import pgrid.PGridHost;
import pgrid.Properties;
import pgrid.Statistics;
import pgrid.core.RoutingTable;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.MessageManager;
import pgrid.network.PGridDecoder;
import pgrid.network.ConnectionManager;
import pgrid.network.PGridMessageMapping;
import pgrid.network.protocol.PGridMessage;
import pgrid.network.protocol.PGridCompressedMessage;
import pgrid.network.protocol.RouteHeader;
import pgrid.network.protocol.RouterACKMessage;
import pgrid.network.protocol.MessageHeader;
import pgrid.util.logging.LogFormatter;
import pgrid.util.TimerManager;
import test.demo.KnnQuery;
import test.demo.RangeQuery;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * The Router routes messages in the network.
 * 
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman
 *         Schmidt</a>
 * @version 1.0.0
 * 
 */
public class Router implements pgrid.util.TimerListener {

	/**
	 * The routing failed.
	 */
	public static final short ROUTE_FAILED = 2;

	/**
	 * The routing was successful.
	 */
	public static final short ROUTE_OK = 0;

	/**
	 * The routing has to be redone.
	 */
	public static final short ROUTE_PENDING = 1;

	/**
	 * The routing has to be redone.
	 */
	public static final short ROUTE_UNKNOWN_STRATEGY = 3;

	/**
	 * The PGrid.Router logger.
	 */
	protected static final Logger LOGGER = Logger.getLogger("PGrid.Router");

	/**
	 * The time to wait for reply messages.
	 */
	public static final int REPLY_TIMEOUT = 60 * 1000 * 1; // 1 min.

	/**
	 * Parallel algorithm which consist in sending the range query to all
	 * sub-tree this peer is responsible of.
	 */
	public static final String SHOWER_STRATEGY = "Shower";

	/**
	 * Sequential algorithm which consist in finding dynamicaly the next
	 * neighbor and send the range query to it.
	 */
	public static final String MINMAX_STRATEGY = "MinMax";

	/**
	 * Strategy sending a message with a greedy algorithm to a single host
	 */
	public static final String GREEDY_STRATEGY = "Greedy";

	/**
	 * Strategy sending a message to a list of receiver in an efficient way.
	 */
	public static final String ANYCAST_STRATEGY = "Anycast";

	/**
	 * Strategy sending a message to a list of receiver in an efficient way.
	 */
	public static final String BROADCAST_STRATEGY = "Broadcast";

	/**
	 * Strategy sending a message to a list of receiver in an efficient way.
	 */
	public static final String DIRECT_STRATEGY = "Direct";

	/**
	 * Strategy sending a message to the replicas sub network.
	 */
	public static final String REPLICA_STRATEGY = "Replica";

	/**
	 * Strategy sending a message to the replicas sub network.
	 */
	public static final String TOPOLOGY_STRATEGY = "Topologic";

	/**
	 * Strategy sending a message to a random peer for a given branch.
	 */
	public static final String RANDOM_STRATEGY = "Random";

	/**
	 * Connection manager
	 */
	protected ConnectionManager mConMng = null;

	/**
	 * Decode a PGridMessage data
	 */
	protected PGridDecoder mDecoder = new PGridDecoder();

	/**
	 * The P2P facility.
	 */
	protected P2P mP2P = null;

	/**
	 * GUID to routing listeners mapping
	 */
	private Hashtable<GUID, Vector<RouterListener>> mListener = new Hashtable<GUID, Vector<RouterListener>>();

	/**
	 * The message manager.
	 */
	protected MessageManager mMsgMgr = MessageManager.sharedInstance();

	/**
	 * The P2P facility.
	 */
	protected PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 * Queue of recently seen message to identify already seen msg which should
	 * be ignored.
	 */
	private final LinkedBlockingQueue mSeenMsg = new LinkedBlockingQueue(100);

	/**
	 * Thread pool
	 */
	protected Executor mThreadPool = null;

	/**
	 * The timer manager
	 */
	protected pgrid.util.TimerManager mTimerManager = TimerManager
			.sharedInstance();

	/**
	 * Route Handler
	 */
	private Hashtable<String, RoutingStrategy> mRoutingStrategies = new Hashtable<String, RoutingStrategy>();

	private Hashtable<GUID, RouteAttempt> mRouteAttempts = new Hashtable<GUID, RouteAttempt>();

	/**
	 * True if the router is activated
	 */
	private boolean mActive = false;

	/**
	 * Key range key
	 */
	protected static Object KEY = new Object();

	static {
		LogFormatter formatter = new LogFormatter();
		formatter.setDateFormat("HH:mm:ss");
		formatter.setFormatPattern(LogFormatter.DATE + ": "
				+ LogFormatter.MESSAGE + LogFormatter.NEW_LINE
				+ LogFormatter.THROWABLE);
		Constants.initChildLogger(LOGGER, formatter, null);
	}

	/**
	 * Creates a new router.
	 * 
	 */
	public Router() {
		super();

		// create a thread pool of 10 threads.
		mThreadPool = Executors.newFixedThreadPool(32);

		mConMng = ConnectionManager.sharedInstance();

		// Register strategies
		registerStrategy(new ShowerRoutingStrategy(this));
		registerStrategy(new ReplicaRoutingStrategy(this));
		registerStrategy(new GreedyRoutingStrategy(this));
		registerStrategy(new TopologicRoutingStrategy(this));
		registerStrategy(new RandomWalkRoutingStrategy(this));
		registerStrategy(new BroadcastRoutingStrategy(this));

		mTimerManager.register(1000, null, this, true);
	}

	/**
	 * Creates a new router.
	 */
	protected Router(boolean init) {

	}

	/**
	 * Register a new routing strategy.
	 * 
	 * @param strategy
	 *            to be registreted
	 */
	public void registerStrategy(pgrid.network.router.RoutingStrategy strategy) {
		mRoutingStrategies.put(strategy.getStrategyName(), strategy);
	}

	/**
	 * Returns a reference to the Router logger. This logger can be used in
	 * strategies or requests object
	 * 
	 * @return a reference to the router logger.
	 */
	public static Logger getLogger() {
		return LOGGER;
	}

	/**
	 * Check new response was received.
	 * 
	 * @param message
	 *            the response message.
	 */
	protected void checkAcknowledgment(RouterACKMessage message) {
		Collection<GUID> refs = message.getHeader().getReferences();
		for (GUID guid : refs) {
			RouteAttempt attempt = mRouteAttempts.remove(guid);
			if (attempt == null) {
				continue;
			}
			PGridMessage msg = (PGridMessage) attempt.getMessage();

			if (message.getCode() == RouterACKMessage.CODE_OK) {
				// the message was routed correctly
				Router.LOGGER.finer("Received ACK OK for message \"" + guid
						+ "\".");
				routingSucceeded(guid);
				return;
			} else if (message.getCode() == RouterACKMessage.CODE_MSG_ALREADY_SEEN
					|| message.getCode() == RouterACKMessage.CODE_WRONG_ROUTE
					|| message.getCode() == RouterACKMessage.CODE_CANNOT_ROUTE
					|| message.getCode() == RouterACKMessage.CODE_NOT_SUPERPEER) {

				// keep statistic
				if (PGridP2P.sharedInstance().isInTestMode()) {
					if (message.getCode() == RouterACKMessage.CODE_MSG_ALREADY_SEEN)
						mPGridP2P.getStatistics().incMessageStat(
								Statistics.messageStats.alreadySeen,
								msg.getHeader().getDesc());
					else if (message.getCode() == RouterACKMessage.CODE_WRONG_ROUTE)
						mPGridP2P.getStatistics().incMessageStat(
								Statistics.messageStats.badRequest,
								msg.getHeader().getDesc());
					else if (message.getCode() == RouterACKMessage.CODE_CANNOT_ROUTE)
						mPGridP2P.getStatistics().incMessageStat(
								Statistics.messageStats.cannotRoute,
								msg.getHeader().getDesc());
					else if (message.getCode() == RouterACKMessage.CODE_NOT_SUPERPEER)
						mPGridP2P.getStatistics().incMessageStat(
								Statistics.messageStats.notSuperpeer,
								msg.getHeader().getDesc());
				}

				// if we get a not super peer error message, inform the
				// maintenance manager so that it can correct the
				// routing table
				if (message.getCode() == RouterACKMessage.CODE_NOT_SUPERPEER)
					mPGridP2P.getMaintenanceManager().removeRemotePeer(
							message.getHeader().getHost());

				// message has been seen by host already => try another host if
				// available, otherwise routing failed
				if (attempt.getIterator().hasNext()) {
					Router.LOGGER.finer("Route failed for message \"" + guid
							+ "\". Try to find an other path.");
					mThreadPool.execute(attempt);
					return;
				} else {
					Router.LOGGER.finer("Route failed for message \"" + guid
							+ "\". No other path available.");
					routingFailed(guid);
					mMsgMgr.failedToRoute(message);
					return;
				}
			}
		}
	}

	/**
	 * Registers a listener for a search request.
	 * 
	 * @param guid
	 *            guid of the message
	 * @param listener
	 *            the listener
	 */
	public void registerRouterListener(GUID guid, RouterListener listener) {
		synchronized (mListener) {
			Vector<RouterListener> listeners = mListener.get(guid);

			if (listeners == null)
				listeners = new Vector<RouterListener>();
			listeners.add(listener);
			mListener.put(guid, listeners);
		}
	}

	/**
	 * Route a message to a random host at the given level
	 * 
	 * @param msg
	 *            message to route
	 * @param level
	 *            to choose the host from
	 * @return true if the attemp has succeeded
	 */
	protected boolean routeAtLevel(Message msg, int level, Key key) {
		RoutingTable rTable = mPGridP2P.getRoutingTable();

		// determine the responsible routing table level
		PGridHost[] hosts = rTable.getLevel(level);

		// if no hosts are available in this level => throw exception
		if ((hosts == null) || (hosts.length == 0)) {
			throw new NoRouteToKeyException("Message GUID: " + msg.getGUID() +" "+key.toString()
					+ ", level: " + level + ", host: ["
					+ mPGridP2P.getLocalHost().toHostString()+ "].");
		}

		// create and shuffle hosts list and iterate throw it
		List list = Arrays.asList(hosts);

	//	Collections.shuffle(list);
		Iterator it = list.iterator();

		RouteAttempt attempt = new RouteAttempt(msg, list, it);

		// send query message
		return route(attempt);
	}
	/**
	 * Route a message to a random host at the given level
	 * 
	 * @param msg
	 *            message to route
	 * @param level
	 *            to choose the host from
	 * @return true if the attemp has succeeded
	 */
	protected boolean routeAtLevel(Message msg, int level) {
		RoutingTable rTable = mPGridP2P.getRoutingTable();

		// determine the responsible routing table level
		PGridHost[] hosts = rTable.getLevel(level);

		// if no hosts are available in this level => throw exception
		if ((hosts == null) || (hosts.length == 0)) {
			throw new NoRouteToKeyException("Message GUID: " + msg.getGUID()
					+ ", level: " + level + ", host: ["
					+ mPGridP2P.getLocalHost().toHostString()+ "].");
		}

		// create and shuffle hosts list and iterate throw it
		List list = Arrays.asList(hosts);

	//	Collections.shuffle(list);
		Iterator it = list.iterator();

		RouteAttempt attempt = new RouteAttempt(msg, list, it);
		// send query message
		return route(attempt);
	}

	/**
	 * Route a message to a random host at the given level
	 * 
	 * @param msg
	 *            message to route
	 * @param level
	 *            to choose the host from
	 * @return true if the attemp has succeeded
	 */
	protected boolean routeAtLevel(Message msg, int level, KeyRange keyRange,
			boolean otherTrie) {

		RoutingTable rTable = mPGridP2P.getRoutingTable();
		// determine the responsible routing table level
		PGridHost[] hosts = rTable.getLevel(level);
		String localpath = mPGridP2P.getLocalPath();
		int prefixLength = localpath.length();

		String tempLowerkey = keyRange.getMin().toString()
				.substring(0, prefixLength);
		Long lkey = Long.parseLong(tempLowerkey, 2);
		String tempHigherKey = keyRange.getMax().toString()
				.substring(0, prefixLength);
		Long hkey = Long.parseLong(tempHigherKey, 2);
		Long localKey = Long.parseLong(localpath,2);
		List<PGridHost> list = new ArrayList<PGridHost>(Arrays.asList(hosts));
		final Iterator<PGridHost> itList = list.iterator();
		boolean balancedTrie = mPGridP2P
				.propertyBoolean(Properties.BALANCED_TRIE);
		if (balancedTrie) {
			// case 1, the lower and upper bound of query do not have the same
			// common prefix
			if (!tempLowerkey.equalsIgnoreCase(tempHigherKey)) {
				while (itList.hasNext()) {
					Long hostPath = Long.parseLong(itList.next().getPath(), 2);
					if (!(lkey <= hostPath & hostPath <= hkey)) {
						itList.remove();
					}
				}
			}
			if (tempLowerkey.equalsIgnoreCase(tempHigherKey)) {
				// traverse the hosts and delete the references of hosts which
				// do not have the same common prefix
				while (itList.hasNext()) {
					if (!itList.next().getPath()
							.equalsIgnoreCase(tempHigherKey)) {
						itList.remove();
					}
				}
			}
		}
		// if no hosts are available in this level => throw exception
		if ((hosts == null) || (hosts.length == 0)) {
			// decrease the request count
			KnnQuery.requestCount--;
		}
	
		String tableName = new String(mPGridP2P.propertyString(Properties.HTABLE_NAME));
		// if not msg has common prefix
		if (!tableName.equalsIgnoreCase("%")) {
		if (!(hkey == lkey) ) {
						while (itList.hasNext()) {
					Long hostPath = Long.parseLong(itList.next().getPath(), 2);
					if ( lkey < localKey & localKey < hkey) {
						// local peer is reposible for range query message, remove all the hosts
				//		System.out.println("in first if lkey: "+lkey+" localkey "+localKey+" hkey "+hkey+" for query range "+keyRange.getMin().toString()+" "+keyRange.getMax().toString());
						if (hostPath != localKey) {
							itList.remove();
						}
					} 
						
						if (lkey == localKey || hkey == localKey ) {
						//	System.out.println("in second if lkey: "+lkey+" localkey "+localKey+" hkey "+hkey+" for query range "+keyRange.getMin().toString()+" "+keyRange.getMax().toString());
						//	System.out.println(" hostlist is "+ list.size()+" for query range "+keyRange.getMin().toString()+" "+keyRange.getMax().toString());
							if (hostPath != localKey) {
								itList.remove();
						//		System.out.println(" removing hostpath "+Long.toBinaryString(hostPath)+" list size "+list.size());
						}
						}			
		} // end of while 
						if (list.size() > 1) {
						//	System.out.println("inside list size check hostlist is "+ list.size()+" for query range "+keyRange.getMin().toString()+" "+keyRange.getMax().toString());
							for (int i =list.size(); i > 1; i-- )
								list.remove(i-1);
						//	System.out.println(" hostlist now is "+ list.size()+" for query range "+keyRange.getMin().toString()+" "+keyRange.getMax().toString());
						}
		} else {
			if  (hkey == lkey)  {
				while (itList.hasNext()) {
					Long hostPath = Long.parseLong(itList.next().getPath(), 2);
				//	System.out.println("lkey  = hkey, hostlist is "+ list.size()+" for query range "+keyRange.getMin().toString()+" "+keyRange.getMax().toString());
					if (hostPath != lkey) {
						itList.remove();
					//	System.out.println(" removed hostpath "+Long.toBinaryString(hostPath)+" new list size "+list.size());
					}
				}
			}
		}
		
		} 
		// create and shuffle hosts list and iterate throw it

		Iterator<PGridHost>  it = list.iterator();
		RouteAttempt attempt = new RouteAttempt(msg, list,it);
		// send query message
	//	System.out.println("final host list size "+list.size()+" for query range "+keyRange.getMin().toString()+" "+keyRange.getMax().toString());
		return route(attempt);
	}

	/**
	 * Route attempt
	 * 
	 * @param attempt
	 * @return true if the message was sent
	 */
	protected boolean route(RouteAttempt attempt) {
		PGridMessage msg = (PGridMessage) attempt.getMessage();
		GUID guid = msg.getHeader().getGUID();
		Iterator it = attempt.getIterator();
		attempt.resetSentTime();
		mRouteAttempts.put(guid, attempt);

		while (it.hasNext()) {
			PGridHost host;
			try {
				host = (PGridHost) it.next();
			} catch (NoSuchElementException e) {
				return false;
			}
		
			LOGGER.fine("Try to send message (" + guid.toString() + ") to "
					+ host.toHostString() +" message content "+msg.toXMLString()+  ".");
			boolean sent = mConMng.sendPGridMessage(host, msg);
			if (!sent) {
				if (it.hasNext()) {
					LOGGER.fine("Failed to send message (" + guid.toString()
							+ "), try an other host.");
					continue;
				} else {
					LOGGER.fine("not sent and no more hosts for message ("
							+ guid.toString() + ").");
					PGridMessage recvMsg = (PGridMessage) attempt.getMessage();
					if (recvMsg instanceof PGridCompressedMessage)
						recvMsg = mDecoder
								.decode((PGridCompressedMessage) attempt
										.getMessage());

					if (recvMsg != null)
						mMsgMgr.failedToRoute(recvMsg);
					return false;
				}
			} else {
				LOGGER.fine("Message (" + guid.toString() + ") sent to "
						+ host.toHostString() + ".");
				if (PGridP2P.sharedInstance().isInTestMode()) {
					int type = msg.getHeader().getDesc();
					if (!msg.getHeader().getHost()
							.equals(mPGridP2P.getLocalHost()))
						mPGridP2P.getStatistics().incMessageStat(
								Statistics.messageStats.forwarded, type);
				}
			}

			break;
		}
		return true;
	}

	/**
	 * Route a message to multiple hosts
	 * 
	 * @param hosts
	 *            list of host to route to
	 * @param msg
	 *            message to be routed
	 */
	protected void route(Collection<PGridHost> hosts, PGridMessage msg) {
		for (PGridHost host : hosts) {
			LOGGER.fine("Try to send message (" + msg.getGUID().toString()
					+ ") to " + host.toHostString() + ".");
			boolean sent = mConMng.sendPGridMessage(host, msg);
			if (!sent) {
				LOGGER.fine("not sent and no more hosts for message ("
						+ msg.getGUID().toString() + ").");
			} else {
				LOGGER.fine("Message (" + msg.getGUID().toString()
						+ ") sent to " + host.toHostString() + ".");
				if (PGridP2P.sharedInstance().isInTestMode()) {
					int type = msg.getHeader().getDesc();
					if (!msg.getHeader().getHost()
							.equals(mPGridP2P.getLocalHost()))
						mPGridP2P.getStatistics().incMessageStat(
								Statistics.messageStats.forwarded, type);
				}
			}
		}
	}

	/**
	 * Routes a message using a given algorithm.
	 * 
	 * @param msg
	 *            Message to route
	 * @param strategy
	 *            Strategy to use
	 * @param routingInfo
	 *            a routing data object. This object is routing strategy
	 *            dependent.
	 * @param routerListener
	 *            A router listener
	 */
	public void route(PGridMessage msg, String strategy, Object routingInfo,
			RouterListener routerListener) throws RoutingStrategyException {
		// check if the router is activated:
		if (!mActive)
			return;

		RoutingStrategy rs = mRoutingStrategies.get(strategy);

		// check if the strategy is known
		if (rs == null) {
			throw new RoutingStrategyException("Routing strategy \"" + strategy
					+ "\" is unknown.");
		}

		// keep statistic on router
		if (PGridP2P.sharedInstance().isInTestMode())
			mPGridP2P.getStatistics().incMessageStat(
					Statistics.messageStats.initiated,
					msg.getHeader().getDesc());

		// fill routing information
		rs.fillRoutingInfo(msg, strategy, routingInfo);
		// register all routing listener
		if (routerListener != null) {
			registerRouterListener(msg.getGUID(), routerListener);

		}
		registerMsg(new Request(msg, rs, true));
	}

	/**
	 * Routes a message using a given algorithm.
	 * 
	 * @param msg
	 *            Message to route
	 * @param strategy
	 *            Strategy to use
	 * @param routingInfo
	 *            a routing data object. This object is routing strategy
	 *            dependent.
	 */
	protected void reRoute(PGridMessage msg, String strategy, Object routingInfo)
			throws RoutingStrategyException {
		RoutingStrategy rs = mRoutingStrategies.get(strategy);

		// check if the strategy is known
		if (rs == null) {
			throw new RoutingStrategyException("Routing strategy \"" + strategy
					+ "\" is unknown.");
		}

		// fill routing information
		rs.fillRoutingInfo(msg, strategy, routingInfo);

		registerMsg(new Request(msg, rs, true, true));
	}

	/**
	 * Main entry point for all network message. If the peer is responsible for
	 * this message, it will be send to the message manager, otherwise it will
	 * be routed.
	 * 
	 * @param message
	 *            new incoming message
	 */
	public void incomingMessage(PGridMessage message) {
		boolean delegated = false;
		PGridMessage msg = message;

		// check if the router is activated:
		if (!mActive) {
			sendACK(msg.getHeader().getHost(),
					RouterACKMessage.CODE_CANNOT_ROUTE, msg.getHeader()
							.getGUID());
			return;
		}

		// check if we haven't seen this message before.
		if (alreadySeen(msg)) {
			sendACK(msg.getHeader().getHost(),
					RouterACKMessage.CODE_MSG_ALREADY_SEEN, msg.getHeader()
							.getGUID());
			return;
		}

		// this is a direct message, no routing is needed
		if (msg.getHeader().getRouteHeader() == null) {
			PGridHost delegator = msg.getHeader().getClientAddress();

			if (mPGridP2P.isSuperPeer() && delegator != null
					&& !delegator.equals(msg.getHeader().getHost())) {
				// register this message for redirection
				registerMsg(new RequestToDelegator(delegator, msg));
			} else {
				PGridMessage recvMsg;

				if (msg instanceof PGridCompressedMessage) {
					recvMsg = mDecoder.decode((PGridCompressedMessage) msg);
					if (recvMsg == null)
						return;
				} else
					recvMsg = msg;

				mMsgMgr.dispatchMessage(recvMsg);
			}

			return;
		} else if (!mPGridP2P.isSuperPeer()) {
			// if this peer is not a super peer and it receive a non-direct
			// message, send an error message to sender
			sendACK(msg.getHeader().getHost(),
					RouterACKMessage.CODE_NOT_SUPERPEER, msg.getHeader()
							.getGUID());
			return;
		}

		// Retreive routing strategy
		String strategy = msg.getHeader().getRouteHeader().getStrategy();
		RoutingStrategy rs = mRoutingStrategies.get(strategy);

		// check if the strategy is known
		if (rs == null) {
			Router.LOGGER.warning("Routing strategy \"" + strategy
					+ "\" is unknown.");
			sendACK(msg.getHeader().getHost(), ROUTE_UNKNOWN_STRATEGY, msg
					.getHeader().getGUID());
			return;
		}

		// Do some pre processing on Routing header if needed
		rs.preProcessMessage(msg);

		// check if local peer is responsible for this message
		if (rs.isResponsible(msg)) {
			if (msg instanceof PGridCompressedMessage) {
				msg = mDecoder.decode((PGridCompressedMessage) msg);
				if (msg == null)
					return;
			}
			if (PGridP2P.sharedInstance().isInTestMode())
				mPGridP2P.getStatistics().incMessageStat(
						Statistics.messageStats.resolved,
						msg.getHeader().getDesc());
		}

		// if sending host is behind a firewall, take requestor's place and
		// route this message
		if (msg.getHeader().getHost().isBehindFirewall()
				|| msg.getHeader().getDelegateStatus() == MessageHeader.DelegateStatus.toBeDelegated) {
			Router.LOGGER.finer("Local peer become delegate for message ["
					+ msg.getGUID() + "].");

			if (msg.getHeader().getHost().isBehindFirewall()) {
				// sender host delegates the routing process.
				PGridHost host = msg.getHeader().getRequestorHost();
				msg.getHeader().setRequestorHost(mPGridP2P.getLocalHost());
				msg.getHeader().setClientAddress(host);
			}
			msg.getHeader().setDelegateStatus(
					MessageHeader.DelegateStatus.hasBeenDelegated);
			delegated = true;
		}

		// register this message
		registerMsg(new Request(msg, rs, delegated));

	}

	/**
	 * This method should be called by the routing strategy <b>if and only
	 * if</b> it has reply <code>false</code> when isResponsible was called and
	 * because of some evenement, this has changed. <br/>
	 * This method is called, for instance, in the Route method of some routing
	 * strategy when the responsible peer depend on whether or not the message
	 * was successfuly forwarded.
	 * 
	 * @param msg
	 */
	public void informLocalPeer(PGridMessage msg) {
		PGridMessage recvMsg = msg;

		if (msg instanceof PGridCompressedMessage) {
			recvMsg = mDecoder.decode((PGridCompressedMessage) msg);
			if (recvMsg == null)
				return;
		}

		if (PGridP2P.sharedInstance().isInTestMode())
			mPGridP2P.getStatistics()
					.incMessageStat(Statistics.messageStats.resolved,
							msg.getHeader().getDesc());

		mMsgMgr.dispatchMessage(recvMsg);
	}

	/**
	 * Register a routing request
	 * 
	 * @param request
	 *            the routing request
	 */
	private void registerMsg(Request request) {
		mThreadPool.execute(request);
	}

	/**
	 * Send an acknowledgement message.
	 * 
	 * @param host
	 * @param code
	 * @param guid
	 *            the guid of the ACK message
	 */
	protected void sendACK(PGridHost host, int code, GUID guid) {
		RouterACKMessage ack = new RouterACKMessage(guid, code);
		mConMng.sendPGridMessage(host, ack);
	}

	/**
	 * Shutdown
	 */
	public void shutdown() {
		Router.LOGGER.finer("Shutting down router...");
		mActive = false;
		if (mThreadPool != null)
			((ExecutorService) mThreadPool).shutdown();
	}

	/**
	 * Timer triggered callback method. This method will remove all routing
	 * attempts older then a certain amount of time and all old client
	 * informations.
	 * 
	 * @param id
	 */
	public void timerTriggered(Object id) {
		// check if there was a time out
		long now = System.currentTimeMillis();
		int mult = 1;

		Vector<RouteAttempt> attemps = new Vector(mRouteAttempts.values());
		for (RouteAttempt a : attemps) {
			if ((now - a.getSentTime()) * mult >= Constants.ROUTE_TIMEOUT) {
				if (PGridP2P.sharedInstance().isInTestMode())
					mPGridP2P.getStatistics().incMessageStat(
							Statistics.messageStats.timeout,
							((PGridMessage) a.getMessage()).getHeader()
									.getDesc());
				registerMsg(new Request(a));
			}
		}
	}

	/**
	 * Return true if this message has already been seen.
	 * 
	 * @param msg
	 * @return true if this message has already been seen.
	 */
	private boolean alreadySeen(PGridMessage msg) {

		/**
		 * Check if it's not a streaming message. Streaming message replies have
		 * the same GUID GetFileReply message has type 31, according to
		 * MessageMapping.xml
		 */
		if (PGridMessageMapping.sharedInstance().isFileStreaming(
				msg.getHeader().getDesc())
				&& msg.getHeader().getDesc() == 31) {
			return false;
		}

		// check if the query was already seen
		GUID qID = msg.getHeader().getGUID();

		synchronized (mSeenMsg) {
			if (mSeenMsg.contains(qID)) {
				Constants.LOGGER.finer("Remote search request for query ("
						+ qID + ") already seen.");
				return true;
			} else {
				// add query to seen query queue
				if (mSeenMsg.remainingCapacity() == 0)
					mSeenMsg.poll();
				mSeenMsg.offer(qID);
				return false;
			}
		}
	}

	/**
	 * Inform the listener that the message has been successfuly sent
	 * 
	 * @param guid
	 */
	protected void routingSucceeded(GUID guid) {
		Collection<RouterListener> listeners = mListener.remove(guid);

		if (listeners != null) {
			for (RouterListener listener : listeners) {
				listener.routingFinished(guid);
			}
		}
	}

	/**
	 * Inform the listener that the message has not been successfully sent
	 * 
	 * @param guid
	 */
	protected void routingFailed(GUID guid) {
		Collection<RouterListener> listeners = mListener.remove(guid);

		if (listeners != null) {
			for (RouterListener listener : listeners) {
				listener.routingFailed(guid);
			}
		}
	}

	/**
	 * Activate the router.
	 */
	public void activate() {
		mActive = true;
	}

	/**
	 * Desactivate the router.
	 */
	public void desactivate() {
		mActive = false;
	}

	protected class RequestToDelegator extends Request {
		PGridHost mHost;
		PGridMessage mMsg;

		public RequestToDelegator() {
			super();
		}

		RequestToDelegator(PGridHost host, PGridMessage msg) {
			mHost = host;
			mMsg = msg;
		}

		public void run() {
			Router.LOGGER.finer("Forwarding message [" + mMsg.getGUID()
					+ "] to client host [" + mHost.toHostString() + "].");
			mMsg.getHeader().setClientAddress(null);
			ConnectionManager.sharedInstance().sendPGridMessage(mHost, mMsg);
		}
	}
}