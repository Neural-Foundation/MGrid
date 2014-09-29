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


package p2p.basic.events;

import p2p.basic.Message;
import p2p.basic.Peer;

/**
 * Defines callback interface to inform listeners of
 * events on the network.
 */
public interface P2PListener {

	/**
	 * Invoked when a new message needs to be delivered to the application.
	 *
	 * @param message the message received
	 * @param origin  the peer from which the message was sent
	 */
	void newMessage(Message message, Peer origin);
}
