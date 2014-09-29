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
 * Global Unique Identifier is used to distinguish objects
 * in a distributed environment. Implementations must
 * define the hashCode and equals methods to that effect.
 */
public interface GUID {

	/**
	 * Returns the value of the unique ID.
	 *
	 * @return returns a byte array the represents the unique ID.
	 */
	public byte[] getBytes();

  /**
   * Returns the string representation of the unique ID.
   *
   * @return returns a string that represents the unique ID.
   */
  public String toString();

}
