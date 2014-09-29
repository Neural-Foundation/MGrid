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

import p2p.index.events.NoSuchTypeException;
import pgrid.network.protocol.PGridMessage;

/**
 * All manager willing to handle message reveived from the network should implement this interface
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public interface RemoteMessageHandler {

	/**
	 * This method is called when a new message arrives. When a message is broadcasted, it is NOT ensure that the local
	 * host is really responsible for this message.
	 *
	 * @param msg	received message
	 * @param broadcasted	true if message was broadcaseted.
	 * @throws IOException 
	 * @throws NoSuchTypeException 
	 */
	public void newRemoteMessage(PGridMessage msg, boolean broadcasted) throws NoSuchTypeException;

	/**
	 * This method is called when the router is unable to route further a message. The routing behavior is left to
	 * the application layer.
	 *  
	 * @param msg
	 */
	public void failedToRoute(PGridMessage msg);
}
