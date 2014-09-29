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

package pgrid.network;

import p2p.basic.GUID;
import p2p.basic.Key;
import p2p.basic.KeyRange;
import p2p.index.events.NoSuchTypeException;
import pgrid.Constants;
import pgrid.PGridHost;
import pgrid.core.maintenance.identity.IdentityManager;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.protocol.*;
import pgrid.network.router.*;
import test.demo.*;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * This class represents the message manager for all sent and received
 * messages.
 * This class implements the <code>Singleton</code> pattern as defined by
 * Gamma et.al. As there could only exist one instance of this class, other
 * clients must use the <code>sharedInstance</code> function to use this class.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class MessageManager implements MessageDispatcher {

	/**
	 * The reference to the only instance of this class (Singleton
	 * pattern). This differs from the C++ standard implementation by Gamma
	 * et.al. since Java ensures the order of static initialization at runtime.
	 *
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	private static final MessageManager SHARED_INSTANCE = new MessageManager();

	/**
	 * The PGridP2P facility.
	 */
	private PGridP2P mPGridP2P = null;

	/**
	 * Router layer
	 */
	private Router mRouter = null;

	/**
	 * The threads waiting for a response.
	 * INFO: Using a WeakHashMap remove a big mem leak but could introduce some hard bug to track...
	 */
	private Map<GUID, MessageWaiter> mWaiters = Collections.synchronizedMap(new HashMap<GUID, MessageWaiter>());
	private Map<Integer, RemoteMessageHandler> mRemoteHandler = Collections.synchronizedMap(new HashMap<Integer, RemoteMessageHandler>());

	/**
	 * The constructor must be protected to ensure that only subclasses can
	 * call it and that only one instance can ever get created. A client that
	 * tries to instantiate PGridP2P directly will get an error at compile-time.
	 */
	protected MessageManager() {
	}

	/**
	 * This creates the only instance of this class. This differs from the C++ standard implementation by Gamma et.al.
	 * since Java ensures the order of static initialization at runtime.
	 *
	 * @return the shared instance of this class.
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	public static MessageManager sharedInstance() {
		return SHARED_INSTANCE;
	}

	/**
	 * Initializes the Message Manager.
	 */
	public void init() {
		mPGridP2P = PGridP2P.sharedInstance();
		mRouter = mPGridP2P.getRouter();
		//mIdentMgr = IdentityManager.sharedInstance();
	}

	/**
	 * Called by the router when a new message appears.
	 *
	 * @param msg	the new message
	 * @throws IOException 
	 * @throws NoSuchTypeException 
	 */
	public void dispatchMessage(PGridMessage msg) throws NoSuchTypeException{
		// process the message if needed
		RemoteMessageHandler handler = mRemoteHandler.get(msg.getHeader().getDesc());
		if (handler != null) {
			handler.newRemoteMessage(msg, msg.getHeader().isBroadcasted());
		} else {
			TreeSet<GUID> refs = new TreeSet();
			MessageWaiter mw;

			if (msg.getHeader().getReferences() != null) refs.addAll(msg.getHeader().getReferences());
			refs.add(msg.getGUID());

			// send a notification to all listener
			for (GUID guid : refs) {
				mw = mWaiters.get(guid);
				if (mw != null) {
					mw.newMessage(msg, guid);
				}
			}
		}
	}

	/**
	 * Inform the remote message handler that the local peer was unable to send further the given message.
	 *
	 * @param msg
	 */
	public void failedToRoute(PGridMessage msg) {
		RemoteMessageHandler handler = mRemoteHandler.get(msg.getHeader().getDesc());
		if (handler != null) {
			handler.failedToRoute(msg);
		}
	}


	/**
	 * Sends the delivered message to the replicas subnetwork
	 *
	 * @param msg           the message to send.
	 */
	public void sendToReplicas(PGridMessage msg, RouterListener listener) {
		try {
			mRouter.route(msg, Router.REPLICA_STRATEGY, null, listener);
		} catch (RoutingStrategyException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}

	/**
	 * Sends the delivered message to the delivered host. Warning: waiters are kept in a weak hash table where the
	 * message GUID is used as the key. If the message GUID is garbage collected, the waiter will be removed from
	 * the waiter list and, therefor, not called when a message having this GUID as ref. is received.
	 *
	 * @param host          the receiving host.
	 * @param msg           the message to send.
	 * @param waiter if the calling thread should be notified, if a response was received.
	 * @return true if the message has been send
	 */
	public boolean sendMessage(PGridHost host, PGridMessage msg, MessageWaiter waiter){
		return sendMessage(host, msg, waiter, null);
	}

	/**
	 * Sends the delivered message to the delivered host. Warning: waiters are kept in a weak hash table where the
	 * message GUID is used as the key. If the message GUID is garbage collected, the waiter will be removed from
	 * the waiter list and, therefor, not called when a message having this GUID as ref. is received.
	 *
	 * @param host          the receiving host.
	 * @param msg           the message to send.
	 * @param waiter if the calling thread should be notified, if a response was received.
	 * @param msgReference if not null, message waiter will be registrated with this GUID, not the message GUID
	 * @return true if the message has been send
	 */
	public boolean sendMessage(PGridHost host, PGridMessage msg, MessageWaiter waiter, GUID msgReference){
		if ((host == null) || (msg == null))
			throw new NullPointerException();
		// add waiter
		if (waiter != null) {
			mWaiters.put((msgReference==null?msg.getGUID():msgReference), waiter);
		}

		// if the destination peer is the localhost, short circuit the connection manager.
		if (host.equals(mPGridP2P.getLocalHost())) {
			mRouter.incomingMessage(msg);
			return true;
		}

		return ConnectionManager.sharedInstance().sendPGridMessage(host, msg);
	}

	/**
	 * Sends the delivered message to the delivered path.
	 *
	 * @param msg           the message to send.
	 * @param key           The key where this message should be routed
	 * @param routerListener to whom routing information should be send or null.
	 * @param waiter if the calling thread should be notified, if a response was received.
	 */
	public void route(PGridMessage msg, String routingStrategy, Object key, RouterListener routerListener, MessageWaiter waiter) throws RoutingStrategyException {
		// add waiter
		if (waiter != null) {
			mWaiters.put(msg.getGUID(), waiter);
		}
		mRouter.route(msg, routingStrategy, key, routerListener);
	}

	/**
	 * Sends the delivered message to the delivered host.
	 *
	 * @param key           The key where this message should be routed
	 * @param msg           the message to send.
	 * @param routerListener to whom routing information should be send or null.
	 * @param waiter if the calling thread should be notified, if a response was received.
	 */
	public void route(Key key, PGridMessage msg, RouterListener routerListener, MessageWaiter waiter) {
		// add waiter
		if (waiter != null) {
			mWaiters.put(msg.getGUID(), waiter);
		}
		try {
			mRouter.route(msg, Router.GREEDY_STRATEGY, key, routerListener);
		} catch (RoutingStrategyException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}

	/**
	 * Sends the delivered message to random host starting from a given level.
	 *
	 * @param level           the level to which this message should be sent
	 * @param msg           the message to send.
	 * @param routerListener to whom routing information should be send or null.
	 * @param waiter if the calling thread should be notified, if a response was received.
	 */
	public void randomRoute(PGridMessage msg, int level, RouterListener routerListener, MessageWaiter waiter) {
		// add waiter
		if (waiter != null) {
			mWaiters.put(msg.getGUID(), waiter);
		}
		try {
			mRouter.route(msg, Router.RANDOM_STRATEGY, level, routerListener);
		} catch (RoutingStrategyException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}

	/**
	 * Sends the delivered message to the delivered host.
	 *
	 * @param key           The key where this message should be routed
	 * @param msg           the message to send.
	 * @param routerListener to whom routing information should be send or null.
	 * @param waiter if the calling thread should be notified, if a response was received.
	 */
	public void route(KeyRange key, PGridMessage msg, RouterListener routerListener, MessageWaiter waiter) {
		// add waiter
		if (waiter != null) {
			mWaiters.put(msg.getGUID(), waiter);
		}
		try {
			mRouter.route(msg, Router.SHOWER_STRATEGY, key, routerListener);
		} catch (RoutingStrategyException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}

	/**
	 * Sends the delivered message to the delivered list of path.
	 *
	 * @param keys           Set of all keys to where this message should be sent
	 * @param msg            the message to send.
	 * @param routerListener to whom routing information should be send or null.
	 * @param waiter if the  calling thread should be notified, if a response was received.
	 */
	public void route(Collection<Key> keys, PGridMessage msg, RouterListener routerListener, MessageWaiter waiter) {
		// add waiter
		if (waiter != null) {
			mWaiters.put(msg.getGUID(), waiter);
		}
		try {
			mRouter.route(msg, Router.ANYCAST_STRATEGY, keys, routerListener);
		} catch (RoutingStrategyException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}

	/**
	 * Sends the delivered message to the delivered list of path.
	 *
	 * @param host			 host where to send reply.
	 * @param replyMsg       the reply message to send.
	 * @param recievedMsg    Message for which reply is sent.
	 * @param routerListener to whom routing information should be send or null.
	 * @param waiter if the  calling thread should be notified, if a response was received.
	 */
	public void reply(PGridHost host, PGridMessage replyMsg, PGridMessage recievedMsg, RouterListener routerListener, MessageWaiter waiter) {
		if ((host == null) || (replyMsg == null || recievedMsg==null))
			throw new NullPointerException();
		// add waiter
		if (waiter != null) {
			mWaiters.put(replyMsg.getGUID(), waiter);
		}

		// copy header information to reply message
		copySessioninformaiton(recievedMsg, replyMsg);
		ConnectionManager.sharedInstance().sendPGridMessage(host, replyMsg);
	}
	
	/**
	 * This private method copy all extra info from received message header to its reply.
	 * @param msg received message
	 * @param reply reply message
	 */
	private void copySessioninformaiton(PGridMessage msg, PGridMessage reply) {
		// copy header information to reply message
		if (!msg.getHeader().getReferences().isEmpty())
			reply.getHeader().setReferences(new Vector<GUID>(msg.getHeader().getReferences()));
		if (!msg.getHeader().getAdditionalAttributes().isEmpty())
			reply.getHeader().setAdditionalAttributes(msg.getHeader().getAdditionalAttributes());
		if (msg.getHeader().getRequestorHost()!=null)
			reply.getHeader().setRequestorHost(msg.getHeader().getRequestorHost());
		if (msg.getHeader().getClientAddress()!=null)
			reply.getHeader().setClientAddress(msg.getHeader().getClientAddress());

		reply.getHeader().setHops(msg.getHeader().getHops());

	}

	/**
	 * Registers a waiter for a given message GUID.
	 *
	 * @param guid the message guid.
	 * @param waiter the message waiter.
	 */
	public void registerWaiter(GUID guid, MessageWaiter waiter) {
		if ((guid != null) && (waiter != null)) {
			mWaiters.put(guid, waiter);
		}
	}

	/**
	 * Registers a message handler for a given message type (desc). All remote mesage handler found int the mapping
	 * XML file are automatically registereted
	 *
	 * @param type the message type.
	 * @param handler the message handler.
	 */
	public void registerMessageRemoteHandler(int type, RemoteMessageHandler handler) {
		mRemoteHandler.put(type, handler);
	}

	private void sendToDelegator(PGridHost host, PGridMessage msg) {
		ConnectionManager.sharedInstance().sendPGridMessage(host, msg);
	}

}