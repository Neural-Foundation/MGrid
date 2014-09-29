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

package pgrid.network.protocol;

import p2p.basic.GUID;
import p2p.basic.Message;

/**
 * This class represents a interface for all Gridella messages.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public interface PGridMessage extends Message, Cloneable {
	/**
	 * Returns the message as array of bytes.
	 *
	 * @return the message bytes.
	 */
	public byte[] getBytes();

	/**
	 * Returns a desricptor for the type of message.
	 *
	 * @return the message descriptor.
	 */
	public int getDesc();

	/**
	 * Returns the representation string for a descriptor of a message.
	 *
	 * @return the message descriptor string.
	 */
	public String getDescString();

	/**
	 * Returns the message GUID.
	 *
	 * @return the message GUID.
	 */
	public GUID getGUID();

	/**
	 * Returns the message header.
	 *
	 * @return the header.
	 */
	public MessageHeader getHeader();

	/**
	 * Set the message header.
	 *
	 * @param header the new message header
	 */
	public void setHeader(MessageHeader header);

	/**
	 * Returns the message length.
	 *
	 * @return the message length.
	 */
	public int getSize();

	/**
	 * Tests if the message is valid.
	 *
	 * @return <code>true</code> if valid, else <code>false</code>.
	 */
	public boolean isValid();

	/**
	 * Returns a string represantation of the message.
	 *
	 * @return a string represantation of the message.
	 */
	public String toXMLString();

	/**
	 * Create a copy of this message.
	 *
	 * @return a copy of this message.
	 */
	public Object clone();
}