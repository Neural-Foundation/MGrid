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
package pgrid.core.maintenance.loadbalancing;

/**
 * This interface is a listener to the dynamic load balancing componant
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public interface ReplicateBalancerListener {

	/**
	 * This class is called when the local host change its path
	 * @param oldPath		the old path of this peer
	 * @param path			the new path of this peer
	 * @param minStorage	MinStorage constant of the replica
	 */
	public void localPathChanged(String oldPath, String path, int minStorage);

	/**
	 * This method is called when the cloning process failed.
	 */
	public void cloningFailed();
}
