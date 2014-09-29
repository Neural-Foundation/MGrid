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
import pgrid.PGridKey;
import pgrid.PGridHost;
import pgrid.util.Utils;
import p2p.basic.Key;

import java.security.SecureRandom;

/**
 * The Query Router routes query messages in the network.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */
class RandomWalkRoutingStrategy extends RoutingStrategy {

	/**
	 * Stategy name
	 */
	public static String STRATEGY_NAME = Router.RANDOM_STRATEGY;


	/**
	 * The randomizer delivers random numbers.
	 */
	private static SecureRandom mRandomizer = new SecureRandom();

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
	public RandomWalkRoutingStrategy(Router router) {
		super(router);
	}

	/**
	 * This method is called when a new message is received. This method can be used to cast key string into something
	 * more meaningful for the routing strategy.
	 */
	 public void preProcessMessage(PGridMessage msg) {
		MessageHeader header = msg.getHeader();
		RouteHeader rheader = header.getRouteHeader();

		Key key = new PGridKey(rheader.getKey());

		rheader.setProperty(RandomWalkRoutingStrategy.KEY, key);
	}

	/**
	 * Routes a range query to the responsible peer.
	 *
	 * @param request the msg route request.
	 */
	public short route(Request request) {
		//check if it is the correct request
		MessageHeader header = request.getMessage().getHeader();
		RouteHeader rheader = header.getRouteHeader();
		int commonLen = rheader.getDepth();

		if (request.isLocallyStarted() || request.isMessageDelegated()) {
			mRouter.routeAtLevel(request.getMessage(), commonLen);
		} else {
			PGridHost host = header.getHost();
			String rPath = rheader.getKey();
			Router.LOGGER.fine("received remote messeage from host " + host.toHostString() + " with path " + rPath + ", and common len " + rheader.getDepth() + ".");

			int len = Utils.commonPrefix(mPGridP2P.getLocalPath(), rPath).length();

			// the common length of the paths are not as required => reply with 'Path Changed' code
			if ((len != commonLen) || (mPGridP2P.getLocalPath().length() == 0) && rPath.length() != 0) {
				Router.LOGGER.fine("common length unequal do local common length => reply 'Path Changed'.");
				/*SearchPathReplyMessage replyMsg = new SearchPathReplyMessage(msg.getGUID(), msg.getPath());
							mMsgMgr.sendMessage(host, replyMsg, null);   */
				return RouterACKMessage.CODE_WRONG_ROUTE;
			}

			int l0 = commonLen+1;
			// Check if this message is a dynamic joining of the network
			if (rPath.length() == 0) {
				Router.LOGGER.fine("Remote path corrupted \"" + rPath + "\".");

			} else {
				// dynamic load balancing
				while ((mRandomizer.nextDouble() > 0.5) && (l0 < mPGridP2P.getLocalPath().length())) {
					l0++;
				}
			}


			if (l0 >= mPGridP2P.getLocalPath().length()) {
				// local peer is responsible for this message
				Router.LOGGER.fine("local peer is responsible for this message.");
				getRouter().informLocalPeer(request.getMessage());
			} else {
				// try to find another peer
				Router.LOGGER.fine("try to find another peer at level: "+l0+".");
				PGridMessage msg = (PGridMessage) request.getMessage().clone();

				msg.getHeader().getRouteHeader().setDepth(l0);
				msg.getHeader().getRouteHeader().setKey(mPGridP2P.getLocalPath());

				mRouter.routeAtLevel(msg, l0);
			}
		}

		return RouterACKMessage.CODE_OK;
	}

	public String getStrategyName() {
		return RandomWalkRoutingStrategy.STRATEGY_NAME;
	}

	/**
	 * Fill all needed routing information
	 *
	 * @param msg
	 * @param strategy
	 * @param routingInfo
	 */
	public void fillRoutingInfo(PGridMessage msg, String strategy, Object routingInfo) throws RoutingStrategyException {
		if (!(routingInfo instanceof Integer))
			throw new RoutingStrategyException("Get \""+routingInfo.getClass().getName()+
					"\" as key where strategy "+ RandomWalkRoutingStrategy.STRATEGY_NAME +" was expecting ."+Integer.class.getName());

		msg.getHeader().setRoutingHeader(new RouteHeader(mPGridP2P.getLocalPath(), RandomWalkRoutingStrategy.STRATEGY_NAME, (Integer)routingInfo, null, null));
	}

	/**
	 * Returns true iff the local peer is reponsible for this message
	 *
	 * @param msg
	 * @return true iff the local peer is reponsible for this message
	 */
	public boolean isResponsible(PGridMessage msg) {
		return false;
	}

	/**
	 *
	 * @param id
	 */
	public void timerTriggered(Object id) {
		/*Iterator it = mQueries.values().iterator();
		Vector guids = new Vector();
		long currentTime = System.currentTimeMillis();
		RangeQueryRoutingRequest request;

		while(it.hasNext()) {
			request = (RangeQueryRoutingRequest)it.next();
			if (request.getStartTime()+Constants.QUERY_PROCESSING_TIMEOUT < currentTime)
				guids.add(request.getQuery().getGUID());
		}

		if (!guids.isEmpty()) {
			it = guids.iterator();
			while(it.hasNext()) {
				request = (RangeQueryRoutingRequest)mQueries.remove(it.next());
				if (request != null) {
					Router.LOGGER.fine("["+request.getQuery().getGUID()+"]: Removing range query request reference.");
					request.getSearchListener().searchFinished(request.getQuery().getGUID());
				}
			}
		}*/
	}
}
