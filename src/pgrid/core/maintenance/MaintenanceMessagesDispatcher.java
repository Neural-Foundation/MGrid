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

import pgrid.network.RemoteMessageHandler;
import pgrid.network.protocol.*;
import pgrid.network.router.MessageWaiter;
import pgrid.interfaces.basic.PGridP2P;
import p2p.basic.GUID;

/**
 * Class description goes here
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public class MaintenanceMessagesDispatcher implements MessageWaiter, RemoteMessageHandler {

	protected PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 *
	 * @param msg
	 * @param guid
	 */
	public void newMessage(PGridMessage msg, GUID guid) {
		dispatch(msg);
	}

	/**
	 * This method is called when a new message arrives.
	 *
	 * @param msg
	 * @param broadcasted
	 */
	public void newRemoteMessage(PGridMessage msg, boolean broadcasted) {
		dispatch(msg);
	}

	/**
	 * This method is called when the router is unable to route further a message. The routing behavior is left to
	 * the application layer.
	 *
	 * @param msg
	 */
	public void failedToRoute(PGridMessage msg) {
		// that's life, do nothing
	}

	private void dispatch(PGridMessage msg) {
		if (msg instanceof BootstrapMessage) {
			mPGridP2P.getMaintenanceManager().newBootstrapRequest((BootstrapMessage)msg);
		} else if (msg instanceof BootstrapReplyMessage) {
			mPGridP2P.getMaintenanceManager().newBootstrapReply((BootstrapReplyMessage)msg);
		} else if (msg instanceof FidgetExchangeMessage) {
			mPGridP2P.getMaintenanceManager().newFidgetExchangeRequest((FidgetExchangeMessage)msg);
		} else if (msg instanceof FidgetExchangeReplyMessage) {
			mPGridP2P.getMaintenanceManager().newFidgetExchangeReply((FidgetExchangeReplyMessage)msg);
		} else if (msg instanceof ExchangeMessage) {
			mPGridP2P.getMaintenanceManager().newExchangeRequest((ExchangeMessage)msg);
		} else if (msg instanceof ExchangeReplyMessage) {
			mPGridP2P.getMaintenanceManager().newExchangeReply((ExchangeReplyMessage)msg);
		} else if (msg instanceof ExchangeInvitationMessage) {
			mPGridP2P.getMaintenanceManager().newExchangeInvitation((ExchangeInvitationMessage)msg);
		} else if (msg instanceof SearchPathMessage) {
			mPGridP2P.getMaintenanceManager().newSearchPath(msg.getHeader().getHost(), (SearchPathMessage)msg);
		}  else if (msg instanceof ReplicateMessage) {
			mPGridP2P.getMaintenanceManager().newReplicateRequest(msg.getHeader().getHost(), (ReplicateMessage) msg);
		} else if (msg instanceof ExchangeIndexEntriesMessage) {
			mPGridP2P.getMaintenanceManager().newExchangeIndexEntriesMessage((ExchangeIndexEntriesMessage)msg);
		} else if (msg instanceof ExchangeCSVIndexEntriesMessage) {
			mPGridP2P.getMaintenanceManager().newExchangeCSVIndexEntriesMessage((ExchangeCSVIndexEntriesMessage)msg);
		}
	}


}
