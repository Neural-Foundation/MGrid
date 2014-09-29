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

package p2p.basic;

import java.net.InetAddress;

/**
 * Defines the operation that peers support. Each
 * instance provides addressing information on the
 * represented peer.
 */
public interface Peer {

	/**
	 * Get the guid the distiguishes this peer from others.
	 *
	 * @return the global unique identifer
	 */
	public GUID getGUID();

	/**
	 * Get the range for which the peer is responsible.
	 *
	 * @return the KeyRange
	 */
    public KeyRange getKeyRange();

	/**
	 * Get the peer's address
	 *
	 * @return the Internet address
	 */
	public InetAddress getIP();

	/**
	 * Get the peer's service port
	 *
	 * @return the service port number
	 */
	public int getPort();
}