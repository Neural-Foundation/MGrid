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

import pgrid.util.LexicalDefaultHandler;
import pgrid.XMLizable;
import pgrid.Constants;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.PGridMessageMapping;
import p2p.basic.GUID;

import java.io.UnsupportedEncodingException;

/**
 * This class is a helper class for everyone willing to create a new message for P-Grid. Each message should be cloneable
 * therefor if a message use mutable object, it should implement clone method and clone those field.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public abstract class PGridMessageImp extends LexicalDefaultHandler implements PGridMessage, XMLizable {

	/**
	 * The message header.
	 */
	private MessageHeader mHeader = null;

	/**
	 * Returns the XML message name for this message
	 *
	 * @return the XML message name for this message
	 */
	abstract protected String getXMLMessageName();

	/**
	 * Default constructor
	 */
	public PGridMessageImp() {
		mHeader = new MessageHeader(Constants.PGRID_PROTOCOL_VERSION, -1, PGridP2P.sharedInstance().getLocalHost(), getDesc());
	}

	/**
	 * Default constructor
	 */
	public PGridMessageImp(GUID guid) {
		mHeader = new MessageHeader(Constants.PGRID_PROTOCOL_VERSION, -1, PGridP2P.sharedInstance().getLocalHost(),
				guid, getDesc(), null);
	}

	/**
	 * Default constructor
	 */
	public PGridMessageImp(MessageHeader header) {
		mHeader = header;
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
	 * Returns the exchange message as array of bytes.
	 *
	 * @return the message bytes.
	 */
	public byte[] getBytes() {
		byte[] bytes=null;

		try {
			bytes = toXMLString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
		return bytes;
	}

	/**
	 * Returns a desricptor for the type of message.
	 *
	 * @return the message descriptor.
	 */
	public int getDesc() {
		int desc = PGridMessageMapping.sharedInstance().getType(getXMLMessageName());
		if (desc < 0)
			Constants.LOGGER.warning("Message "+getXMLMessageName()+" is not registrated correctly in the message mapping XML file.");

		return desc;
	}

	/**
	 * Returns the representation string for a descriptor of a message.
	 *
	 * @return the message descriptor string.
	 */
	public String getDescString() {
		return PGridMessageMapping.sharedInstance().getDescription(getXMLMessageName());
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
	 * Returns the message header.
	 *
	 * @return the header.
	 */
	public MessageHeader getHeader() {
		return mHeader;  //To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Set the message header.
	 */
	public void setHeader(MessageHeader header) {
		mHeader = header;
	}

	/**
	 * Tests if the message is valid.
	 *
	 * @return <code>true</code> if valid, else <code>false</code>.
	 */
	public boolean isValid() {
		if (mHeader == null) {
			return false;
		} else {
			if (!mHeader.isValid()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns the message length.
	 *
	 * @return the message length.
	 */
	public int getSize() {
		return toXMLString().length();
	}

	/**
	 * Create a copy of this message.
	 *
	 * @return a copy of this message.
	 */
	public Object clone() {
		PGridMessageImp msg = null;
		try {
			msg = (PGridMessageImp) super.clone();
			msg.mHeader = (MessageHeader) mHeader.clone();
		} catch (CloneNotSupportedException e) {
			Constants.LOGGER.warning("Message "+this.getDescString()+" is not fully clonable.");
		}

		return msg;
	}

}
