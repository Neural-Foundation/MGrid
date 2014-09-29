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

import p2p.basic.P2P;
import p2p.basic.Peer;
import p2p.basic.GUID;
import p2p.index.events.SearchListener;
import pgrid.Constants;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.MessageManager;
import pgrid.network.protocol.PeerLookupMessage;
import pgrid.network.protocol.PeerLookupReplyMessage;
import pgrid.network.protocol.PGridMessage;
import pgrid.network.router.*;
import pgrid.util.logging.LogFormatter;

import java.util.logging.Logger;
import java.util.Vector;
import java.util.Hashtable;

/**
 * This class will look for a particular host in the network. This is a pgrid.helper class
 * that can given a path and some criteria retrieve a host.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public class LookupManager implements MessageWaiter, RouterListener {

	/**
	 * The PGrid.Router logger.
	 */
	protected static final Logger LOGGER = Logger.getLogger("PGrid.Lookup");

	/**
	 * The P2P facility.
	 */
	protected P2P mP2P = null;

	protected Hashtable<GUID, SearchListener> mListeners = new Hashtable<GUID, SearchListener>();

	/**
	 * The message manager.
	 */
	protected MessageManager mMsgMgr = MessageManager.sharedInstance();

	/**
	 * The P2P facility.
	 */
	protected PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 * Synchronous request handler
	 */
	protected SynchronousLookupHandler mSynchronousLookupHandler = new SynchronousLookupHandler(mPGridP2P);


	static {
		LogFormatter formatter = new LogFormatter();
		formatter.setDateFormat("HH:mm:ss");
		formatter.setFormatPattern(LogFormatter.DATE + ": " + LogFormatter.MESSAGE + LogFormatter.NEW_LINE + LogFormatter.THROWABLE);
		Constants.initChildLogger(LOGGER, formatter, null);
	}

	/**
	 * Start a peer lookup mechanism to retrieve a peer responsible for the
	 * given path
	 * @param msg the lookup message
	 * @param listener an object to notify when results arrive
	 */
	public void peerLookup(PeerLookupMessage msg, SearchListener listener) {
		TopologicRoutingData data = new TopologicRoutingData(msg.getPath(), TopologicRoutingData.ANY);

		GUID guid = msg.getHeader().getReferences().iterator().next();
		mListeners.put(guid, listener);

		// try to route the message further
		try {
			mMsgMgr.route(msg, Router.TOPOLOGY_STRATEGY, data, null, this);
		} catch (RoutingStrategyException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}

	/**
	 * Start a peer lookup mechanism to retrieve a peer responsible for the
	 * given path. This method is synchronous.
	 * @param msg the lookup message
	 */
	public Peer synchronousPeerLookup(PeerLookupMessage msg, long timeout) {
		return mSynchronousLookupHandler.lookup(msg, timeout);
	}

	/**
	 * A new peer lookup reply was received.
	 *
	 * @param msg the response message.
	 */                                                                                                                                                 
	protected void newPeerLookupReply(PeerLookupReplyMessage msg) {
		SearchListener listener = mListeners.remove(msg.getHeader().getReferences().iterator().next());

		if (listener == null) {
			Constants.LOGGER.fine("No listener for lookup (" + msg.getGUID().toString() + ") with reference ("+msg.getReferencedMsgGUID()+") from " + msg.getHeader().getHost().toHostString() + ".");
			return;
		}

		Constants.LOGGER.fine("Response for lookup (" + msg.getGUID().toString() + ") with reference ("+msg.getReferencedMsgGUID()+")  from " + msg.getHeader().getHost().toHostString() + " received.");
		// something message
		if (msg.getType() == PeerLookupReplyMessage.TYPE_OK) {
			// local request => add results
			Constants.LOGGER.fine("Responsible host found.");
			Vector hosts = new Vector();
			hosts.add(msg.getHost());
			listener.newSearchResult(msg.getReferencedMsgGUID(), hosts);
		} else if (msg.getType() == PeerLookupReplyMessage.TYPE_NO_PEER_FOUNDS) {
			PGridP2P.sharedInstance().getStatistics().LookupNotFound++;
			Constants.LOGGER.fine("Return NO_PEER_FOUNDS for the lookup (" + msg.getGUID().toString() + ") from host " + msg.getHeader().getHost().toHostString() + ".");
			listener.noResultsFound(msg.getReferencedMsgGUID());
		}

		//We have found the host (or not, who cares?) we can remove the reference to the request
		listener.searchFinished(msg.getReferencedMsgGUID());
	}

	public void newMessage(PGridMessage msg, GUID guid) {
		if (msg instanceof PeerLookupReplyMessage) newPeerLookupReply((PeerLookupReplyMessage)msg);
	}

	/**
	 * Invoked when a routing failed.
	 *
	 * @param guid the GUID of the original query
	 */
	public void routingFailed(GUID guid) {
		SearchListener listener = mListeners.remove(guid);

		if (listener == null) {
			Constants.LOGGER.fine("No listener for lookup (" + guid + ").");
			return;
		}

		listener.searchFailed(guid);
	}

	/**
	 * Invoked when a routing finished.
	 *
	 * @param guid the GUID of the original query
	 */
	public void routingFinished(GUID guid) {
		SearchListener listener = mListeners.remove(guid);

		if (listener == null) {
			Constants.LOGGER.fine("No listener for lookup (" + guid + ").");
			return;
		}

		listener.searchFinished(guid);
	}

	/**
	 * Invoked when a routing started (reached a responsible peer).
	 *
	 * @param guid	the GUID of the original query
	 * @param message the explanation message.
	 */
	public void routingStarted(GUID guid, String message) {
		SearchListener listener = mListeners.remove(guid);

		if (listener == null) {
			Constants.LOGGER.fine("No listener for lookup (" + guid + ").");
			return;
		}

		listener.searchStarted(guid, "Lookup started.");
	}
}
