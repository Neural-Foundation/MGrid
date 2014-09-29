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
package pgrid.core.maintenance.identity;

/**
 * This interface represent the minimum to implement in order to be responsible for
 * the join - leave protocol of P-Grid
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 */
public interface JoinLeaveProtocol {

	/**
	 * Unique name of the protocol
	 *
	 * @return the name
	 */
	public String getProtocolName();

	/**
	 * This method is called when a peer join the network for the first time
	 */
	public void newlyJoined();

	/**
	 * This method is called when a peer rejoins the network.
	 */
	public void join();

	/**
	 * This method is called when a peer leaves the network.
	 */
	public void leave();
}
