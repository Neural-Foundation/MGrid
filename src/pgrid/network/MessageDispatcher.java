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

package pgrid.network;

import java.io.IOException;

import pgrid.network.protocol.*;
import pgrid.network.router.MessageWaiter;
import p2p.basic.GUID;
import p2p.index.events.NoSuchTypeException;

/**
 * The listener interface for receiving notification about new incoming
 * messages. Everytime a new message is received the
 * <code>statusChanged()</code> method would be used to inform all listeners.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public interface MessageDispatcher {

	/**
	 * Called by the router when a new message appears.
	 *
	 * @param msg	the new message
	 * @throws IOException 
	 * @throws NoSuchTypeException 
	 */
	public void dispatchMessage(PGridMessage msg) throws NoSuchTypeException;

	/**
	 * Registers a waiter for a given message GUID.
	 *
	 * @param guid the message guid.
	 * @param waiter the message waiter.
	 */
	public void registerWaiter(GUID guid, MessageWaiter waiter);

	/**
	 * Registers a message handler for a given message type (desc). 
	 *
	 * @param type the message type.
	 * @param handler the message handler.
	 */
	public void registerMessageRemoteHandler(int type, RemoteMessageHandler handler);

}