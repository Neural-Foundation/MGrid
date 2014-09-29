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

public class NaFTMessagesDispatcher implements MessageWaiter, RemoteMessageHandler {

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

		if (msg instanceof NaFTConnectionReversalInitMessage) {
			mPGridP2P.getNaFTManager().newNaFTConnectionReversalInitMessage((NaFTConnectionReversalInitMessage)msg);
		} else if (msg instanceof NaFTConnectionRegisterMessage) {
			mPGridP2P.getNaFTManager().newNaFTConnectionRegisterMessage((NaFTConnectionRegisterMessage)msg);
		} else if (msg instanceof NaFTConnectionRegisterReplyMessage) {
			mPGridP2P.getNaFTManager().newNaFTConnectionRegisterReplyMessage((NaFTConnectionRegisterReplyMessage)msg);
		} else if (msg instanceof GetFile) {
			mPGridP2P.getDownloadManager().newGetFileMessage((GetFile)msg);
		} else if (msg instanceof GetFileReply) {
			mPGridP2P.getDownloadManager().newGetFileReplyMessage((GetFileReply)msg);
		}

	}


}
