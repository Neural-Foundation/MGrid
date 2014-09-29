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

import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.protocol.*;
import pgrid.PGridHost;

import java.util.Vector;

/**
 * The Query Router routes query messages in the network.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */
class BroadcastRoutingStrategy extends RoutingStrategy {

	/**
	 * Stategy name
	 */
	public static String STRATEGY_NAME = Router.BROADCAST_STRATEGY;

	/**
	 * Maximum TTL for a given message
	 */
	public static final int MAX_TTL = 5;

	/**
	 * The P2P facility.
	 */
	protected PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 * default constructor
	 *
	 * @param router
	 */
	public BroadcastRoutingStrategy(Router router) {
		super(router);
	}

	/**
	 * Routes a range query to the responsible peer.
	 *
	 * @param request the msg route request.
	 */
	public short route(Request request) {
		if (request.getMessage().getHeader().getRouteHeader().getDepth() >= MAX_TTL)
			return RouterACKMessage.CODE_OK;

		PGridMessage msg = (PGridMessage) request.getMessage();


		MessageHeader header = msg.getHeader();
		RouteHeader rheader = header.getRouteHeader();

		// incrise TTL value.
		rheader.setDepth(rheader.getDepth()+1);

		Vector<PGridHost> rhosts = msg.getHeader().getRouteHeader().getHosts();

		if (rhosts == null) {
			rhosts = new Vector<PGridHost>();
		}

		Vector<PGridHost> hosts = null;
		hosts = new Vector<PGridHost>(mPGridP2P.getRoutingTable().getFidgetVector());

		hosts.remove(mPGridP2P.getLocalHost());

		hosts.removeAll(rhosts);
		rhosts.addAll(hosts);
		if (!rhosts.contains(mPGridP2P.getLocalHost())) rhosts.add(mPGridP2P.getLocalHost());

		rheader.setHosts(rhosts);

		mRouter.route(hosts, msg);
		return RouterACKMessage.CODE_OK;
	}

	public String getStrategyName() {
		return BroadcastRoutingStrategy.STRATEGY_NAME;
	}

	/**
	 * Fill all needed routing information
	 *
	 * @param msg
	 * @param strategy
	 * @param routingInfo
	 */
	public void fillRoutingInfo(PGridMessage msg, String strategy, Object routingInfo) throws RoutingStrategyException {
		msg.getHeader().setRoutingHeader(new RouteHeader(null, BroadcastRoutingStrategy.STRATEGY_NAME, 0, null, null));
	}

	/**
	 * Returns true iff the local peer is reponsible for this message
	 *
	 * @param msg
	 * @return true iff the local peer is reponsible for this message
	 */
	public boolean isResponsible(PGridMessage msg) {
		return true;
	}

}
