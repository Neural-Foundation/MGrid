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

import pgrid.PGridHost;
import pgrid.util.PathComparator;
import pgrid.core.RoutingTable;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.MessageManager;
import pgrid.network.router.Router;
import pgrid.network.router.Request;
import pgrid.network.protocol.*;
import pgrid.util.Utils;

import java.util.Hashtable;
import java.util.Vector;

/**
 * The Query Router routes query messages in the network.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */
class TopologicRoutingStrategy extends pgrid.network.router.RoutingStrategy {

	/**
	 * Stategy name
	 */
	public static String STRATEGY_NAME = Router.TOPOLOGY_STRATEGY;

	/**
	 * Key range key
	 */
	protected static Object KEY = new Object();

	/**
	 * Mode of the togological routing
	 */
	protected static String MODE = "Mode";

	  /**
	 * The list of all already seen Querys.
		*/
	  private Hashtable mQueries = new Hashtable();

	/**
	 * The P2P facility.
	 */
	protected PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 * Message manager
	 */
	protected MessageManager mMsgMng = MessageManager.sharedInstance();

	/**
	 * default constructor
	 *
	 * @param router
	 */
	public TopologicRoutingStrategy(Router router) {
		super(router);
	}

	/**
	 * Routes a query to the responsible peer.
	 *
	 * @param request the query route request.
	 */
	public short route(Request request) {
		MessageHeader header = request.getMessage().getHeader();
		RouteHeader rheader = header.getRouteHeader();
		int mode = (Integer)rheader.getProperty(MODE);

		String path = (String)rheader.getProperty(KEY);

		String localPath = mPGridP2P.getLocalPath();
		boolean sent = false;
		int compath = Utils.commonPrefix(localPath, path).length();

		//First part of the algorithm. Route to a peer responsible for path
		if (path.length() > compath && localPath.length() > compath) {
			Router.LOGGER.fine("Searching for a subtree compatible with path: '" + path + "'.");
			// add query to the already seen queries list
			rheader.setDepth(compath);

			// send query message
			sent = mRouter.routeAtLevel(request.getMessage(), compath);

			if (!sent) {
				//TODO do something like sending an ACK
			}

		} else if ((localPath.length() > compath) &&
				(mode != TopologicRoutingData.ANY)) {
			//second part of the algorithm: find the left or right most peer
			Router.LOGGER
					.fine("Searching for the " + ((mode == TopologicRoutingData.RIGHT_MOST) ? "right" : "left") + " most peer of key space: '" + path
					+ "'.");
			PGridHost hosts[];
			Vector list;
			int cmp;
			PathComparator pathComparator = new PathComparator();
			RoutingTable routingTable = mPGridP2P.getRoutingTable();
			int routingLevelCount = routingTable.getLevelCount();


			for (int index = path.length(); index < routingLevelCount && !sent; index++) {
				hosts = routingTable.getLevel(index);
				if (hosts.length == 0)
					continue;
				cmp = pathComparator.compare(hosts[0].getPath(), localPath);
				if ((cmp > 0 && mode == TopologicRoutingData.RIGHT_MOST)
						|| (cmp < 0 && mode == TopologicRoutingData.LEFT_MOST)) {
					//Try to forward the lookup message
					PGridMessage msg = (PGridMessage)request.getMessage().clone();

					msg.getHeader().getRouteHeader().setDepth(index);
					sent = getRouter().routeAtLevel(msg, index);

					if (sent) {
						Router.LOGGER
								.fine("Lookup message has been forwarded.");
						//we should not send a lookup reply
						break;
					}
				}
			}
		}
		if (!sent) {
			getRouter().informLocalPeer(request.getMessage());
		}

		return RouterACKMessage.CODE_OK;

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
		if (!(routingInfo instanceof TopologicRoutingData)) {
			throw new RoutingStrategyException("Key should be of type: "+TopologicRoutingData.class.toString());
		}
		TopologicRoutingData data = (TopologicRoutingData)routingInfo;

		msg.getHeader().setRoutingHeader(new RouteHeader(data.getPath(), Router.TOPOLOGY_STRATEGY, 0, null, null));
		
		msg.getHeader().getRouteHeader().setAdditionalAttribute(MODE, data.getMode()+"");

		msg.getHeader().getRouteHeader().setProperty(KEY, data.getPath());
		msg.getHeader().getRouteHeader().setProperty(MODE, data.getMode());
	}

	/**
	 * Returns true iff the local peer is reponsible for this message
	 *
	 * @param msg
	 * @return true iff the local peer is reponsible for this message
	 */
	public boolean isResponsible(PGridMessage msg) {
		// the responsability is determined in the route method
		return false;
	}

	/**
	 * This method is called when a new message is received. This method can be used to cast key string into something
	 * more meaningful for the routing strategy.
	 */
	public void preProcessMessage(PGridMessage msg) {
		MessageHeader header = msg.getHeader();
		RouteHeader rheader = header.getRouteHeader();

		String key = rheader.getKey();
		int mode = Integer.parseInt(rheader.getAdditionalAttribute(MODE));

		rheader.setProperty(KEY, key);
		rheader.setProperty(MODE, mode);
	}

}