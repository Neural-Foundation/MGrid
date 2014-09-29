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

import pgrid.core.maintenance.identity.MaintenancePolicy;
import pgrid.PGridHost;
import pgrid.IndexEntry;

/**
 * This class is a place holder. It does nothing for the maintenance or the
 * leave - join protocol.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 */
public class NoMaintenancePolicy implements MaintenancePolicy, pgrid.core.maintenance.identity.JoinLeaveProtocol {

	public static final String PROTOCOL_NAME = "None";

	/**
	 * @see pgrid.core.maintenance.identity.JoinLeaveProtocol#getProtocolName()
	 */
	public String getProtocolName() {
		return PROTOCOL_NAME;
	}

	/**
	 * @see pgrid.core.maintenance.identity.MaintenancePolicy#stale(pgrid.PGridHost)
	 */
	public boolean stale(PGridHost host) {
		return false;
	}

	/**
	 * @see pgrid.core.maintenance.identity.MaintenancePolicy#handleUpdate(pgrid.IndexEntry)
	 */
	public boolean handleUpdate(IndexEntry item) {
		return false;
	}

	/**
	 * @see pgrid.core.maintenance.identity.JoinLeaveProtocol#newlyJoined()
	 */
	public void newlyJoined() {
		// do nothing
	}

	/**
	 * @see pgrid.core.maintenance.identity.JoinLeaveProtocol#join()
	 */
	public void join() {
		// do nothing
	}

	/**
	 * @see pgrid.core.maintenance.identity.JoinLeaveProtocol#leave()
	 */
	public void leave() {
		// do nothing
	}

}
