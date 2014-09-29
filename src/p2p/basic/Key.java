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



/**
 * Is used to address items and peers in the
 * distributed indexing structure used by
 * the peer-to-peer network.
 */
public interface Key {

	/**
	 * Returns the value of the key.
	 *
	 * @return returns a byte array that represents the key.
	 */
	public byte[] getBytes();

	/**
	 * Returns the size of the key.
	 * @return the size.
	 */ 
	public int size();

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString();

}

