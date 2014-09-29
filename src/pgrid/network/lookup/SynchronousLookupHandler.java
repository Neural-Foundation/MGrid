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

package pgrid.network.lookup;

import p2p.basic.GUID;
import p2p.basic.Peer;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.MessageManager;
import pgrid.network.router.Router;
import pgrid.network.router.RoutingStrategyException;
import pgrid.network.router.TopologicRoutingData;
import pgrid.network.router.MessageWaiter;
import pgrid.network.protocol.PeerLookupMessage;
import pgrid.network.protocol.PGridMessage;
import pgrid.network.protocol.PeerLookupReplyMessage;
import pgrid.PGridHost;

import java.util.Hashtable;

/**
 * This class processes remote lookup requests.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
class SynchronousLookupHandler implements MessageWaiter {

	/**
	 * Lock objec
	 */
	private final Object mLock = new Object();

	/**
	 * The Message Manager.
	 */
	private MessageManager mMsgMgr = null;

	/**
	 * The PGridP2P facility.
	 */
	private PGridP2P mPGridP2P = null;

	/**
	 * Wait object
	 */
	private final Hashtable mWaiter = new Hashtable();

	/**
	 * Wait object
	 */
	private final Hashtable<GUID, PeerLookupReplyMessage> mReplies = new Hashtable<GUID, PeerLookupReplyMessage>();

	/**
	 * Creates a new RemoteSearchHandler.
	 *
	 * @param pgridP2P the PGridP2P facility.
	 */
	SynchronousLookupHandler(PGridP2P pgridP2P) {
		mPGridP2P = pgridP2P;
		mMsgMgr = MessageManager.sharedInstance();
	}

	/**
	 * Handles the query received by the sender.
	 *
	 * @param msg the lookup message.
	 * @param timeout maximum time to wait
	 */
	public Peer lookup(PeerLookupMessage msg, long timeout) {
		TopologicRoutingData data = new TopologicRoutingData(msg.getPath(), TopologicRoutingData.ANY);
		long t = timeout + System.currentTimeMillis();
		long now = System.currentTimeMillis();
		PGridHost host = null;

		// try to route the message further
		try {
			mMsgMgr.route(msg, Router.TOPOLOGY_STRATEGY, data, null, this);
		} catch (RoutingStrategyException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}

		while (now < t) {
			synchronized(mLock) {
				try {
					mLock.wait(t-now);
				} catch (InterruptedException e) {
					e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
				}
			}
			synchronized(mReplies) {
				host = mReplies.remove(msg.getGUID()).getHost();
				if (host != null) break;
				else now = System.currentTimeMillis();
			}
		}

		return host;
	}

	public void newMessage(PGridMessage msg, GUID guid) {
		if (msg instanceof PeerLookupReplyMessage) {
			mReplies.put(((PeerLookupReplyMessage) msg).getReferencedMsgGUID(), (PeerLookupReplyMessage) msg);

			synchronized(mLock) {
				mLock.notifyAll();
			}

		}
	}
}