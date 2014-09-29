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

package pgrid.core.index;

import p2p.basic.GUID;

/**
 * Defines the callback interface to notify of distribution results.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public interface DistributionListener {

	/**
	 * Invoked when data items were not distributed successfully.
	 *
	 * @param guid  the GUID of the distribution request.
	 */
	public void distributionFailed(GUID guid);

	/**
	 * Invoked when data items were distributed successfully.
	 *
	 * @param guid  the GUID of the distribution request.
	 */
	public void distributionSuccess(GUID guid);
}


