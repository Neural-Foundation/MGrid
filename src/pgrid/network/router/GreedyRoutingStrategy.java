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

import p2p.basic.Key;
import pgrid.*;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.protocol.*;
import pgrid.util.Utils;

/**
 * The Query Router routes query messages in the network.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */
class GreedyRoutingStrategy extends pgrid.network.router.RoutingStrategy {

	/**
	 * Stategy name
	 */
	public static String STRATEGY_NAME = Router.GREEDY_STRATEGY;

	/**
	 * Key range key
	 */
	protected static Object KEY = new Object();

	/**
	 * The P2P facility.
	 */
	protected PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 * default constructor
	 *
	 * @param router
	 */
	public GreedyRoutingStrategy(Router router) {
		super(router);
	}

	/**
	 * Routes a query to the responsible peer.
	 *
	 * @param routingRequest the query route request.
	 */
	public short route(Request routingRequest) {
		MessageHeader header = routingRequest.getMessage().getHeader();
		RouteHeader rheader = header.getRouteHeader();

		Key key = (Key)rheader.getProperty(KEY);
		
		// add query to the already seen queries list
		int compath=0;
		boolean sent;
		if (PGridP2P.sharedInstance().getLocalPath().length() == 0) {
			// Network is not ready for the moment, send the query to the unstructured network
			try {
				mRouter.reRoute(routingRequest.getMessage(), Router.BROADCAST_STRATEGY, null);
			} catch (RoutingStrategyException e) {
				Router.LOGGER.warning("Cannot broadcast message: "+routingRequest.getMessage().getHeader().getGUID()+"!");
			}

			return RouterACKMessage.CODE_OK;
		}
		else compath = Utils.commonPrefix(key.toString(), PGridP2P.sharedInstance().getLocalPath()).length();
		rheader.setDepth(compath);

		if (compath == mPGridP2P.getLocalPath().length() || compath == key.toString().length()) {
			// this peer is reponsible for this message, it should not be send further on
			return RouterACKMessage.CODE_OK;
		} else {
			// send query message
			sent = mRouter.routeAtLevel(routingRequest.getMessage(), compath, key);
		}
		if (sent) return RouterACKMessage.CODE_OK;
		return RouterACKMessage.CODE_CANNOT_ROUTE;
	}

	public String getStrategyName() {
		return STRATEGY_NAME;
	}

	/**
	 * Fill all needed routing information
	 *
	 * @param msg
	 * @param strategy
	 * @param routingInfo
	 */
	public void fillRoutingInfo(PGridMessage msg, String strategy, Object routingInfo) throws RoutingStrategyException {
		if (!(routingInfo instanceof Key))
			throw new RoutingStrategyException("Get \""+routingInfo.getClass().getName()+
					"\" as key where strategy "+STRATEGY_NAME+" was expecting ."+Key.class.getName());

		msg.getHeader().setRoutingHeader(new RouteHeader(routingInfo.toString(), STRATEGY_NAME, 0, null, null));
		msg.getHeader().getRouteHeader().setProperty(KEY, routingInfo);
	}

	/**
	 * Returns true iff the local peer is reponsible for this message
	 *
	 * @param msg
	 * @return true iff the local peer is reponsible for this message
	 */
	public boolean isResponsible(PGridMessage msg) {
		MessageHeader header = msg.getHeader();
		RouteHeader rheader = header.getRouteHeader();

		Key keyRange = (Key)rheader.getProperty(KEY);

		if (keyRange == null) {
			Router.LOGGER.warning("Key format is corrupted: "+rheader.getKey()+".");
			return false;
		}

		return mPGridP2P.isLocalPeerResponsible(keyRange);
	}

	/**
	 * This method is called when a new message is received. This method can be used to cast key string into something
	 * more meaningful for the routing strategy.
	 */
	public void preProcessMessage(PGridMessage msg) {
		MessageHeader header = msg.getHeader();
		RouteHeader rheader = header.getRouteHeader();

		Key key = new PGridKey(rheader.getKey());

		rheader.setProperty(KEY, key);
	}

}