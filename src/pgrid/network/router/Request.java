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

import pgrid.network.protocol.PGridMessage;
import pgrid.network.protocol.RouterACKMessage;
import pgrid.network.protocol.MessageHeader;
import pgrid.network.ConnectionManager;
import pgrid.network.MessageManager;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.PGridHost;
import pgrid.Statistics;

import java.util.*;

/**
 * This interface represent a  request. Each type of request should implement this interface.
 *
 * A request is comparable to a context in the strategy design partern from Gamma et al.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

class Request implements Runnable {

	PGridP2P mPGrid = PGridP2P.sharedInstance();

	PGridMessage message;

	RoutingStrategy mRoutingStrategy;

	RouteAttempt mRouteAttempt=null;

	boolean mLocallyStarted;

	boolean mDelegated = false;

	boolean mRerouting = false;

	public Request() {}

	public Request(PGridMessage msg, RoutingStrategy rs, boolean locallyStarted) {
		this.message = msg;
		this.mRoutingStrategy = rs;
		mLocallyStarted = locallyStarted;
		mDelegated = (msg.getHeader().getDelegateStatus()==MessageHeader.DelegateStatus.hasBeenDelegated);
	}

	public Request(PGridMessage msg, RoutingStrategy rs, boolean locallyStarted, boolean rerouting) {
		this(msg,rs,locallyStarted);
		mRerouting = true;
	}

	public Request(RouteAttempt ra) {
		this.mRouteAttempt = ra;
	}

	public PGridMessage getMessage() {
		return message;
	}

	public RoutingStrategy getStrategy() {
		return mRoutingStrategy;
	}

	public boolean isLocallyStarted() {
		return (message.getHeader().getHops()==0 || (mDelegated && message.getHeader().getHops()==1));
	}

	/**
	 * True iff this message has been delegated to this host and this host should act like it has issue the message.
	 * @return True iff this message has been delegated to this host and this host should act like it has issue the message.
	 */
	public boolean isMessageDelegated() {
		return mDelegated;
	}

	/**
	 * If the local peer is a super peer, use the strategy to route the message, otherwise, forward this message
	 * to a super peer.
	 */
	public void run() {
		if (mRouteAttempt == null) {

			// check if the local host is responsible for this message
			// This check should be performed only on super peer.
			if (mRoutingStrategy.isResponsible(message) && mPGrid.isSuperPeer() && !mRerouting) {
				if (PGridP2P.sharedInstance().isInTestMode())
					mPGrid.getStatistics().incMessageStat(Statistics.messageStats.resolved,message.getHeader().getDesc());
				MessageManager.sharedInstance().dispatchMessage(message);
			}

			// Proceed to routing
			if (mPGrid.isSuperPeer()) {
				int code = getStrategy().route(this);
				if ((isLocallyStarted() && code != RouterACKMessage.CODE_OK) || !isLocallyStarted())
					mPGrid.getRouter().sendACK(getMessage().getHeader().getHost(),
							code, getMessage().getHeader().getGUID());
			} else {
				// the local peer is not a super peer. Forward the message to a super peer.
				// set the delegation flag of this message to true
				message.getHeader().setDelegateStatus(MessageHeader.DelegateStatus.toBeDelegated);
				PGridHost host;

				// we need to differentiate clients from client behind a firewall since the later will use a longer
				// connection.
				if (mPGrid.getLocalHost().isBehindFirewall()) {
					host = 	mPGrid.getRoutingTable().getProxy();
				} else {
					Collection hosts = mPGrid.getRoutingTable().getFidgetVector();
					if (hosts.isEmpty()) return;
					List list = new LinkedList(hosts);
					Collections.shuffle(list);
					host = (PGridHost) list.get(0);
				}
				ConnectionManager.sharedInstance().sendPGridMessage(host,message);
			}
		} else {
			mPGrid.getRouter().route(mRouteAttempt);	
		}
	}
}
