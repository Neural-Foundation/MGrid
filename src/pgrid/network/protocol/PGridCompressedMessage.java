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
import pgrid.Constants;
import pgrid.XMLizable;

/**
 * This class represent a partially readen message where the header has been decoded but the
 * message is still compressed in a byte array.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public class PGridCompressedMessage implements PGridMessage {

	/**
	 * Header for this message
	 */
	protected MessageHeader mHeader;

	/**
	 * Data containing the data
	 */
	protected byte[] mData;

	/**
	 * Constructor
	 *
	 * @param header
	 * @param data
	 */
	public PGridCompressedMessage(MessageHeader header, byte[] data) {
		mData = data;
		mHeader = header;
	}

	/**
	 * Returns the message as array of bytes.
	 *
	 * @return the message bytes.
	 */
	public byte[] getBytes() {
		return mData;  //To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Set the message byte representation
	 */
	public void setBytes(byte[] bytes) {
		mData = bytes;
	}

	/**
	 * Returns a descriptor for the type of message.
	 *
	 * @return the message descriptor.
	 */
	public int getDesc() {
		return getHeader().getDesc();  //To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Returns the representation string for a descriptor of a message.
	 *
	 * @return the message descriptor string.
	 */
	public String getDescString() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Returns the message GUID.
	 *
	 * @return the message GUID.
	 */
	public GUID getGUID() {
		return mHeader.getGUID();  //To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Get the message content.
	 *
	 * @return a binary representation of the message
	 */
	public byte[] getData() {
		return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Returns the message header.
	 *
	 * @return the header.
	 */
	public MessageHeader getHeader() {
		return mHeader;  //To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Returns the message length.
	 *
	 * @return the message length.
	 */
	public int getSize() {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Tests if the message is valid.
	 *
	 * @return <code>true</code> if valid, else <code>false</code>.
	 */
	public boolean isValid() {
		return true;  //To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Returns a string represantation of the message.
	 *
	 * @return a string represantation of the message.
	 */
	public String toXMLString() {
		return "\t[Compressed PGrid message]\n";  //To change body of implemented methods use File | Settings | File Templates.
	}


	/**
	 * Create a copy of this message.
	 *
	 * @return a copy of this message.
	 */
	public Object clone() {
		PGridMessage msg = null;
		try {
			msg = (PGridMessage) super.clone();
		} catch (CloneNotSupportedException e) {
			Constants.LOGGER.warning("Message "+this.getDescString()+"is not fully clonable.");
		}

		return msg;
	}

	/**
	 * Set the message header.
	 *
	 * @param header the new message header
	 */
	public void setHeader(MessageHeader header) {
		mHeader = header;
	}

}
