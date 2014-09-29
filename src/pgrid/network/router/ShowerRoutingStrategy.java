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

import pgrid.*;
import pgrid.util.Utils;
import pgrid.network.protocol.*;
import pgrid.interfaces.basic.PGridP2P;

import p2p.basic.KeyRange;
import test.demo.KnnQuery;;

/**
 * The Query Router routes query messages in the network.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */
class ShowerRoutingStrategy extends RoutingStrategy {

	/**
	 * Stategy name
	 */
	public static String STRATEGY_NAME = Router.SHOWER_STRATEGY;

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
	public ShowerRoutingStrategy(Router router) {
		super(router);
	}

	/**
	 * This method is called when a new message is received. This method can be used to cast key string into something
	 * more meaningful for the routing strategy.
	 */
	 public void preProcessMessage(PGridMessage msg) {
		MessageHeader header = msg.getHeader();
		RouteHeader rheader = header.getRouteHeader();

		KeyRange keyRange = new PGridKeyRange(rheader.getKey());
		
		rheader.setProperty(KEY, keyRange);
	}

	/**
	 * Routes a range query to the responsible peer.
	 *
	 * @param request the msg route request.
	 */
	public short route(Request request) {
		if (mPGridP2P.getLocalPath().equals("")) {
			Router.LOGGER.fine("P-Grid is currently not structured. Fallback to unstructured network.");
			try {
				mRouter.reRoute(request.getMessage(), Router.BROADCAST_STRATEGY, null);
			} catch (RoutingStrategyException e) {
				Router.LOGGER.warning("Cannot broadcast message: "+request.getMessage().getHeader().getGUID()+"!");
			}

			return RouterACKMessage.CODE_OK;
		}

		MessageHeader header = request.getMessage().getHeader();
		RouteHeader rheader = header.getRouteHeader();
		KeyRange keyRange = (KeyRange)rheader.getProperty(KEY);
		if (keyRange == null) {
			Router.LOGGER.warning("Key range format is corrupted: "+rheader.getKey()+".");
			return RouterACKMessage.CODE_CANNOT_ROUTE;
		}
		//forward the range query to all subtrees
		String localpath = mPGridP2P.getLocalPath();

		// calculate number of requests
		int maxLevel = 0;
		int size = mPGridP2P.getRoutingTable().getLevelCount();
		String tmpPath = "";
		char[] charLocalPath = localpath.toCharArray();
		char[] charLower = keyRange.getMin().toString().toCharArray();
		char[] charHigher = keyRange.getMax().toString().toCharArray();
		int qIndex = request.getMessage().getHeader().getRouteHeader().getDepth();
		String info ="";
		boolean otherTrie ;
		if (keyRange.withinRange(new PGridKey(localpath))) {
			maxLevel = localpath.length()-1;
		} else {
			int min = Utils.commonPrefix(keyRange.getMin().toString(), localpath).length();
			int max = Utils.commonPrefix(keyRange.getMax().toString(), localpath).length();
			maxLevel = Math.min(Math.max(min, max), localpath.length()-1);
		}
		if (maxLevel >= qIndex)
			Router.LOGGER.fine("Try to route message ("+request.getMessage().getHeader().getGUID().toString()+"). ");
		// keep track of router
		
		for (int level = maxLevel; level >= qIndex; level--) {
			// don't send a range query when not necessary wtf is this?
			if ((!(request.isLocallyStarted()) && level == 0) || level >= size) {
				continue;
			}
			Router.LOGGER.fine("Try to contact a host at level " + level + ".");
			// Standard case
			if (level != 0) {
				tmpPath = localpath.substring(0, level) + ((charLocalPath[level] == '1') ? '0' : '1');
				info = "Starting parallel remote search to handle subtree '"+tmpPath+"'.";
				otherTrie = false;
			}
			// side change
			else if (charLocalPath[0] != charLower[0] || charLocalPath[0] != charHigher[0]) {
				otherTrie = true;
				tmpPath = (charLocalPath[0] == '1') ? "0" : "1";
				info = "Sending range message to the other side of the trie "+tmpPath;
			} else {
				continue;
			}
			
			// send the message
			String tableName = new String(mPGridP2P.propertyString(Properties.HTABLE_NAME));
			if (keyRange.withinRange(new PGridKey(tmpPath))) {
				PGridMessage msg = request.getMessage();
				if (!tableName.equalsIgnoreCase("%")) {
					msg.getHeader().getRouteHeader().setDepth(localpath.length());
				} else {
					msg.getHeader().getRouteHeader().setDepth(level+1);
				}
				
				Router.LOGGER.fine(info+" msg guid: "+msg.getGUID().toString()+" depth: "+msg.getHeader().getRouteHeader().getDepth());
				boolean sent = mRouter.routeAtLevel(msg, level,keyRange,otherTrie);
			}
		}

		return RouterACKMessage.CODE_OK;
	}
/*	*//**
	 * Routes a range query to the responsible peer.
	 *
	 * @param request the msg route request.
	 *//*
	public short route(Request request) {

		if (mPGridP2P.getLocalPath().equals("")) {
			Router.LOGGER.fine("P-Grid is currently not structured. Fallback to unstructured network.");
			try {
				mRouter.reRoute(request.getMessage(), Router.BROADCAST_STRATEGY, null);
			} catch (RoutingStrategyException e) {
				Router.LOGGER.warning("Cannot broadcast message: "+request.getMessage().getHeader().getGUID()+"!");
			}

			return RouterACKMessage.CODE_OK;
		}

		MessageHeader header = request.getMessage().getHeader();
		RouteHeader rheader = header.getRouteHeader();

		KeyRange keyRange = (KeyRange)rheader.getProperty(KEY);
		if (keyRange == null) {
			Router.LOGGER.warning("Key range format is corrupted: "+rheader.getKey()+".");
			return RouterACKMessage.CODE_CANNOT_ROUTE;
		}
	//	System.out.println("keyrange: "+keyRange.getMin().toString()+" "+keyRange.getMax().toString());
		
		//forward the range query to all subtrees
		String localpath = mPGridP2P.getLocalPath();
		int prefixLength = localpath.length();
		
		// calculate number of requests
		String tempLowerkey = keyRange.getMin().toString().substring(0, prefixLength);
		Long lkey = Long.parseLong(tempLowerkey, 2);
		String tempHigherKey = keyRange.getMax().toString().substring(0, prefixLength);
		Long hkey = Long.parseLong(tempHigherKey, 2);
		Long numPeers = (hkey - lkey)+1;

	//	System.out.println("increasing request count from :"+	KnnQuery.requestCount + " for "+tempLowerkey+" "+tempHigherKey);
		KnnQuery.requestCount+=numPeers;
	//	System.out.println("increased request count to "+KnnQuery.requestCount);
		
		int maxLevel;
		int size = mPGridP2P.getRoutingTable().getLevelCount();
		String tmpPath = "";
		char[] charLocalPath = localpath.toCharArray();
		char[] charLower = keyRange.getMin().toString().toCharArray();
		char[] charHigher = keyRange.getMax().toString().toCharArray();
		int qIndex = request.getMessage().getHeader().getRouteHeader().getDepth();
		String info ="";

		if (keyRange.withinRange(new PGridKey(localpath))) {
			maxLevel = localpath.length()-1;
			
		} else {
			int min = Utils.commonPrefix(keyRange.getMin().toString(), localpath).length();
			int max = Utils.commonPrefix(keyRange.getMax().toString(), localpath).length();

			maxLevel = Math.min(Math.max(min, max), localpath.length()-1);
		}
	
		
		if (maxLevel >= qIndex)
			Router.LOGGER.fine("Try to route message ("+request.getMessage().getHeader().getGUID().toString()+").");
		
		// keep track of router
		boolean otherTrie ;
		for (int level = maxLevel; level >= qIndex; level--) {
			// don't send a range query when not necessary wtf is this?
			if ((!(request.isLocallyStarted()) && level == 0) || level >= size) {
				continue;
			}

			Router.LOGGER.fine("Try to contact a host at level " + level + ".");
			// Standard case
			if (level != 0) {
				tmpPath = localpath.substring(0, level) + ((charLocalPath[level] == '1') ? '0' : '1');
				info = "Starting parallel remote search to handle subtree '"+tmpPath+"'.";
				otherTrie = false;
			}
			// side change
			else if (charLocalPath[0] != charLower[0] || charLocalPath[0] != charHigher[0]) {
				otherTrie = true;
				tmpPath = (charLocalPath[0] == '1') ? "0" : "1";
				info = "Sending range message to the other side of the trie "+tmpPath;
			} else {
				continue;
			}
			
			// send the message
			if (keyRange.withinRange(new PGridKey(tmpPath))) {
				PGridMessage msg = request.getMessage();
				msg.getHeader().getRouteHeader().setDepth(level+1);
				Router.LOGGER.fine(info);
				boolean sent = mRouter.routeAtLevel(msg, level,keyRange,otherTrie);
			}
		}

		return RouterACKMessage.CODE_OK;
	}*/
	public String getStrategyName() {
		return ShowerRoutingStrategy.STRATEGY_NAME;
	}

	/**
	 * Fill all needed routing information
	 *
	 * @param msg
	 * @param strategy
	 * @param routingInfo
	 */
	public void fillRoutingInfo(PGridMessage msg, String strategy, Object routingInfo) throws RoutingStrategyException {
		if (!(routingInfo instanceof KeyRange))
			throw new RoutingStrategyException("Get \""+routingInfo.getClass().getName()+
					"\" as key where strategy "+STRATEGY_NAME+" was expecting ."+KeyRange.class.getName());

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

		KeyRange keyRange = (KeyRange)rheader.getProperty(KEY);

		if (keyRange == null) {
			Router.LOGGER.warning("Key range formart is corrupted: "+rheader.getKey()+".");
			return false;
		}

		return mPGridP2P.isLocalPeerResponsible(keyRange);
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
