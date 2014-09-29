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
package pgrid.core.maintenance;

import pgrid.interfaces.basic.PGridP2P;
import pgrid.PGridHost;
import pgrid.Constants;
import pgrid.network.protocol.BootstrapMessage;
import pgrid.network.protocol.BootstrapReplyMessage;
import pgrid.network.protocol.FidgetExchangeMessage;
import pgrid.network.protocol.FidgetExchangeReplyMessage;

/**
 * Class description goes here
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public class ClientBootstrapper extends Bootstrapper {
	/**
	 * Creates a new router.
	 *
	 * @param p2p the P2P facility.
	 */
	ClientBootstrapper(PGridP2P p2p, MaintenanceManager maintManager) {
		super(p2p, maintManager);
	}

	/**
	 * Adds the given host to the list of addBootstrapHost hosts.
	 *
	 * @param host the new addBootstrapHost host.
	 */
	void addBootstrapHost(PGridHost host) {
		if (mPGridP2P.isLocalHost(host)) {
			Constants.LOGGER.warning("Localhost host " + host.toHostString() + " is a bootstrap host. This host should be a super peer!");
		} else {
			Constants.LOGGER.finer("Add host " + host.toHostString() + " as a bootstrap host.");
			mHostsNew.add(host);
		}
	}

	/**
	 * Exchanges the fidget list with one of the hosts of the fidget list.
	 */
	public void fidgetExchange() throws Exception {
		Constants.LOGGER.warning("Fidget exchange is de-activated. Localhost is not a super peer.");
	}

	/**
	 * Processes a new addBootstrapHost request.
	 *
	 * @param bootstrap the addBootstrapHost request.
	 */
	public void newBootstrapRequest(BootstrapMessage bootstrap) {
		Constants.LOGGER.warning("Received a bootstrap request. This host is not a super peer. Ignore.");
	}

	/**
	 * Processes a new addBootstrapHost response. The whole fidget list while be copied inside the routing table.
	 *
	 * @param bootstrapReply the addBootstrapHost response.
	 */
	public void newBootstrapReply(BootstrapReplyMessage bootstrapReply) {

		if (bootstrapReply.getRoutingTable() != null) {
			// copy received fidget hosts to the list of fidget hosts
			// one of this hosts will be used by the Exchanger to initiate the first Exchange

			mPGridP2P.getRoutingTable().setFidgets(bootstrapReply.getRoutingTable().getFidgets());
			if (mPGridP2P.getRoutingTable().getFidgetVector().size() >= mMaxFidget) {
				mHasBootstrapped = true;
			}

			mPGridP2P.getRoutingTable().save();
		} else {
			// if we have no RoutingTable, it means either we have bootstrapped or the fidget exchange failed
			// in both case, retry ASAP
			mNextExchangeTime = System.currentTimeMillis();
		}
	}

	/**
	 * Processes a new addBootstrapHost request.
	 *
	 * @param exchange the addBootstrapHost request.
	 */
	public void newFidgetExchangeRequest(FidgetExchangeMessage exchange) {
		Constants.LOGGER.warning("Received a fidget exchange request. This host is not a super peer. Ignore.");
	}

	/**
	 * Processes a new addBootstrapHost response.
	 *
	 * @param reply the addBootstrapHost response.
	 */
	public void newFidgetExchangeReply(FidgetExchangeReplyMessage reply) {
		Constants.LOGGER.warning("Received a fidget exchange request. This host is not a super peer. Ignore.");
	}

	/**
	 * Returns true if this peer has bootstrapped and is ready for a fidget exchange phase
	 *
	 * @return true if this peer has bootstrapped and is ready for a fidget exchange phase
	 */
	public boolean hasBootstrapped() {
		return mHasBootstrapped;
	}
}
