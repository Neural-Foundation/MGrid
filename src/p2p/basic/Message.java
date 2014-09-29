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
 * Defines a message that peers exchange. Most systems will
 * use multiple types of messages in their protocols, and
 * thus several implementations of this interface will be
 * required.
 */
public interface Message {

	/**
	 * Get the message's guid. Useful to determine if a
	 * message has already been observed previously.
	 *
	 * @return the global unique identifier
	 */
	GUID getGUID();

	/**
	 * Get the message content.
	 *
	 * @return a binary representation of the message
	 */
	byte[] getData();
}
