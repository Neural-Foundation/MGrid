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
import pgrid.network.protocol.RouteHeader;
import pgrid.network.protocol.RouterACKMessage;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.PGridHost;

import java.util.Vector;

/**
 * Class description goes here
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public class ReplicaRoutingStrategy extends RoutingStrategy {
	public final String STRATEGY_NAME = Router.REPLICA_STRATEGY;

	/**
	 * The P2P facility.
	 */
	protected PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 * default constructor
	 *
	 * @param router
	 */
	public ReplicaRoutingStrategy(Router router) {
		super(router);
	}

	/**
	 * Routes a message to the responsible peer.
	 *
	 * @param req
	 */
	public short route(Request req) {
		Vector<PGridHost> hosts = req.getMessage().getHeader().getRouteHeader().getHosts();

		if (hosts == null) {
			hosts = new Vector<PGridHost>();
		}

		// add all hosts which are not in the already send list
		Vector<PGridHost> toSend = new Vector<PGridHost>();
		for (PGridHost host: mPGridP2P.getRoutingTable().getReplicaVector()) {
			if (!hosts.contains(host))	toSend.add(host);
		}

		// update the already send list
		hosts.addAll(toSend);
		hosts.add(mPGridP2P.getLocalHost());

		req.getMessage().getHeader().getRouteHeader().setHosts(hosts);

		// send message
		mPGridP2P.getRouter().route(toSend, req.getMessage());
		mPGridP2P.getRouter().routingSucceeded(req.getMessage().getHeader().getGUID());

		return RouterACKMessage.CODE_OK;
	}

	/**
	 * Retruns the name of the strategy. This name must be unique
	 *
	 * @return name of the strategy
	 */
	public String getStrategyName() {
		return STRATEGY_NAME;  //To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Fill all needed routing information
	 *
	 * @param msg
	 * @param strategy
	 * @param routingInfo
	 */
	public void fillRoutingInfo(PGridMessage msg, String strategy, Object routingInfo) throws RoutingStrategyException {
		if (routingInfo == null) routingInfo = new Vector<PGridHost>();

		if (routingInfo instanceof Vector<?>/* &&
				(!((Vector)routingInfo).isEmpty() && ((Vector)routingInfo).get(0) instanceof PGridHost)*/)
			msg.getHeader().setRoutingHeader(new RouteHeader(null, STRATEGY_NAME, -1, (Vector<PGridHost>)routingInfo, null));
		else {
			throw new RoutingStrategyException("routingInfo is of type "+routingInfo.getClass().getName()+" where it should be have a type of Vector<PGridHost>!");
		}

	}

	/**
	 * Returns true iff the local peer is reponsible for this message
	 *
	 * @param msg
	 * @return true iff the local peer is reponsible for this message
	 */
	public boolean isResponsible(PGridMessage msg) {
		return msg.getHeader().getHost().getPath().equals(PGridP2P.sharedInstance().getLocalPath())
				&& !msg.getHeader().getHost().equals(mPGridP2P.getLocalHost());
	}
}
