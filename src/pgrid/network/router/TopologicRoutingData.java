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
package pgrid.network.router;

/**
 * Class description goes here
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public class TopologicRoutingData {
	/**
	 * look for the smallest peer greater or equal to the given path
	 */
	public static final int RIGHT_MOST = 0;
	/**
	 * look for the greatest peer smaller or equal to the given path
	 */
	public static final int LEFT_MOST = 1;
	/**
	 * look for a peer responsible for the path
	 */
	public static final int ANY = 2;

	protected String mPath;
	protected int mMode;

	public TopologicRoutingData(String path, int mode) {
		mMode = mode;
		mPath = path;
	}

	public String getPath() {
		return mPath;
	}

	public void setPath(String path) {
		this.mPath = path;
	}

	public int getMode() {
		return mMode;
	}

	public void setMode(int mode) {
		this.mMode = mode;
	}

}
