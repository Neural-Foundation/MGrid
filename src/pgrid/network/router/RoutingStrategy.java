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

import pgrid.network.router.Router;
import pgrid.network.router.Request;
import pgrid.network.protocol.PGridMessage;
import pgrid.network.protocol.PGridCompressedMessage;

/**
 * Abstract routing strategy class
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 */
abstract class RoutingStrategy {

	/**
	 * Link to the router object
	 */
	protected Router mRouter;

	/**
	 * default constructor
	 * @param router
	 */
	public RoutingStrategy(Router router) {
		mRouter = router;
	}

	/**
	 * Routes a message to the responsible peer.
	 *
	 * @param req
	 *
	 * @return Acknowledgment code
	 */
	public abstract short route(Request req) ;

	/**
	 * Retruns the name of the strategy. This name must be unique
	 * @return name of the strategy
	 */
	public abstract String getStrategyName();

	/**
	 * Returns the router associated with this strategy
	 * @return the router object
	 */
	public Router getRouter() {
		return mRouter;
	}

	/**
	 * This method is called when a new message is received. This method can be used to cast key string into something
	 * more meaningful for the routing strategy.
	 */
	public void preProcessMessage(PGridMessage msg) {

	}

	/**
	 * Fill all needed routing information
	 * @param msg
	 * @param strategy
	 * @param routingInfo
	 */
	public abstract void fillRoutingInfo(PGridMessage msg, String strategy, Object routingInfo) throws RoutingStrategyException;

	/**
	 * Returns true iff the local peer is reponsible for this message
	 * @param msg
	 * @return true iff the local peer is reponsible for this message
	 */
	public abstract boolean isResponsible(PGridMessage msg);
}
