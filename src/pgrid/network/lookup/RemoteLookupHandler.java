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

package pgrid.network.lookup;

import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.MessageManager;
import pgrid.network.RemoteMessageHandler;
import pgrid.network.protocol.PeerLookupMessage;
import pgrid.network.protocol.PeerLookupReplyMessage;
import pgrid.network.protocol.PGridMessage;
import pgrid.Constants;
import pgrid.PGridKey;

/**
 * This class processes remote lookup requests.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class RemoteLookupHandler implements RemoteMessageHandler {

	/**
	 * The Message Manager.
	 */
	private MessageManager mMsgMgr = MessageManager.sharedInstance();

	/**
	 * The PGridP2P facility.
	 */
	private PGridP2P mPGridP2P = PGridP2P.sharedInstance();




	/**
	 * Creates a new RemoteSearchHandler.
	 */
	public RemoteLookupHandler() {

	}

	/**
	 * This method is called when a new message arrives.
	 *
	 * @param msg
	 * @param broadcasted
	 */
	public void newRemoteMessage(PGridMessage msg, boolean broadcasted) {
		if (msg instanceof PeerLookupMessage) {

			if (broadcasted &&
					!mPGridP2P.isLocalPeerResponsible(new PGridKey(((PeerLookupMessage)msg).getPath())))
				return;

			// reply the results to the requesting host
			Constants.LOGGER.fine("["+msg.getGUID().toString()+"]: Return results for lookup request.");

			PeerLookupReplyMessage reply = new PeerLookupReplyMessage(msg
					.getGUID(), mPGridP2P.getLocalHost(),
					PeerLookupReplyMessage.TYPE_OK, msg.getHeader().getHops());

			mMsgMgr.reply(((PeerLookupMessage)msg).getInitialHost(), reply, msg, null, null);
		}

	}

	/**
	 * This method is called when the router is unable to route further a message. The routing behavior is left to
	 * the application layer.
	 *
	 * @param msg
	 */
	public void failedToRoute(PGridMessage msg) {
		if (msg instanceof PeerLookupMessage) {
			PeerLookupReplyMessage reply = new PeerLookupReplyMessage(msg
					.getGUID(), mPGridP2P.getLocalHost(),
					PeerLookupReplyMessage.TYPE_BAD_REQUEST, msg.getHeader().getHops());
			mMsgMgr.reply(((PeerLookupMessage)msg).getInitialHost(), reply, msg, null, null);
		}
	}
}