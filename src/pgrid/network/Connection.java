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

package pgrid.network;

import pgrid.PGridHost;
import pgrid.Constants;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * This class represents a connection.
 *
 * acquire and release should be called before and after every write instruction block
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class Connection {

	/**
	 * The connection is accepting.
	 */
	public static final short STATUS_ACCEPTING = 3;

	/**
	 * The connection is closing.
	 */
	public static final short STATUS_CLOSING = 5;

	/**
	 * The connection is active.
	 */
	public static final short STATUS_CONNECTED = 4;

	/**
	 * A connection is established.
	 */
	public static final short STATUS_CONNECTING = 2;

	/**
	 * The connection has caused an error.
	 */
	public static final short STATUS_ERROR = 1;

	/**
	 * The connection is not used.
	 */
	public static final short STATUS_NOT_CONNECTED = 0;

	/**
	 * If compression should be used for this connection.
	 */
	private boolean mCompressionFlag = false;

	/**
	 * The start time of the connection.
	 */
	private long mConnectionStartTime = 0;

	/**
	 * The start time of establishing the connection.
	 */
	private long mConnectingStartTime = 0;

	/**
	 * The number of dropped messages.
	 */
	private int mDroppedCount = 0;

	/**
	 * The connected host.
	 */
	private PGridHost mHost = null;

	/**
	 * The connection id.
	 */
	private pgrid.GUID mGUID = null;

	/**
	 * The last status message.
	 */
	private String mLastStatusMsg = null;

	/**
	 * The used protocol as string.
	 */
	private String mProtocolString = null;
	
	/**
	 * The number of received bytes.
	 */
	private int mReceivedBytes = 0;

	/**
	 * The number of received messages.
	 */
	private int mReceivedCount = 0;

	/**
	 * The number of sent bytes.
	 */
	private int mSentBytes = 0;

	/**
	 * The number of sent messages.
	 */
	private int mSentCount = 0;

	/**
	 * The socket.
	 */
	private Socket mSocket = null;

	/**
	 * The status of the connection.
	 */
	private short mStatus = -1;

	/**
	 * Last IO operation
	 */
	private long mLastIO = System.currentTimeMillis();

	/**
	 * Time to wait between the last read or writen byte and the closure
	 * of this connection.
	 */
	private long mTimeOut = 60*60*1000;

	/**
	 * Creates a new Connection.
	 *
	 * @param host the host.
	 */
	public Connection(PGridHost host) {
		mGUID = new pgrid.GUID();
		mHost = host;

		if (ConnectionManager.sharedInstance().isFirewalled())
			mTimeOut = Constants.CONNECTION_TIMEOUT_FIREWALLED_PEER;
		else
			mTimeOut = Constants.CONNECTION_STD_TIMEOUT;
	}

	/**
	 * Creates a new Connection.
	 *
	 * @param socket the socket.
	 */
	public Connection(Socket socket) {
		mGUID = new pgrid.GUID();
		mSocket = socket;
		try {
			socket.setSoTimeout((int) mTimeOut);
			socket.setTcpNoDelay(false);
			socket.setSoLinger(true, 10);
		} catch (SocketException e) {
		}
	}

	/**
	 * Closes the connection and all streams.
	 */
	public void close() {
		setStatus(Connection.STATUS_NOT_CONNECTED, "Closed");

		if (mSocket != null) {
			try {
				/*
				As the following shutdownInput() and shutdownOutput() are NOT supported for SSL sockets,
				they are commented out.
				TODO find out the side effects of such a change				
				 */
              /*  mSocket.shutdownInput();
				mSocket.shutdownOutput();*/
				mSocket.close();
                /*TODO verify whether all input and output streams associated with this socket are closed
                /* PGridReader PGridWrite has those streams. It looks like they are NOT closed
                */
                mSocket = null;
			} catch (NullPointerException e) {
				// do nothing
			} catch (IOException e) {
				// do nothing
			}
		}

	}

	/**
	 * This method should be called after every read/write.
	 */
	public void resetIOTimer() {
		mLastIO = System.currentTimeMillis();
	}

	/**
	 * Return the last IO timestamp
	 * @return the last IO timestamp
	 */
	public long getLastIOTime() {
		return mLastIO;
	}

	/**
	 * Tests if the connection uses the compressed protocol.
	 *
	 * @return <code>true</code> if compression is used, <code>false</code> otherwise.
	 */
	public boolean isCompressed() {
		return mCompressionFlag;
	}

	/**
	 * Sets if the connection uses the compressed protocol.
	 *
	 * @param flag <code>true</code> if compression is used, <code>false</code> otherwise.
	 */
	public void setCompression(boolean flag) {
		mCompressionFlag = flag;
	}

	/**
	 * Tests if connected or not.
	 *
	 * @return <code>true</code> if connected, <code>false</code> otherwise.
	 */
	public boolean isConnected() {
		if ((mSocket != null) && (mSocket.isConnected()))
			return true;
		else
			return false;
	}

	/**
	 * Tests if closing or not.
	 *
	 * @return <code>true</code> if closing, <code>false</code> otherwise.
	 */
	public boolean isClosing() {
		return (mStatus == STATUS_CLOSING);
	}

	/**
	 * Returns the start time of the connection to the host.
	 *
	 * @return the start time of the connection to the host.
	 */
	public long getConnectionTime() {
		if ((mStatus == STATUS_NOT_CONNECTED) || (mStatus == STATUS_ERROR))
			return 0;
		if ((mStatus == STATUS_ACCEPTING) || (mStatus == STATUS_CONNECTING))
			return System.currentTimeMillis() - mConnectingStartTime;
		return System.currentTimeMillis() - mConnectionStartTime;
	}

	/**
	 * Returns the number of dropped messages.
	 *
	 * @return the number of dropped messages.
	 */
	public int getDroppedCount() {
		return mDroppedCount;
	}

	/**
	 * Increases the number of dropped messages from the host.
	 */
	public void incDroppedCount() {
		mDroppedCount++;
	}

	/**
	 * Returns the host.
	 *
	 * @return the host.
	 */
	public PGridHost getHost() {
		return mHost;
	}

	/**
	 * Sets the host.
	 *
	 * @param host the host.
	 */
	public void setHost(PGridHost host) {
		mHost = host;
		if(host.isBehindFirewall())
			mTimeOut = Constants.CONNECTION_TIMEOUT_FIREWALLED_PEER;
		else
			mTimeOut = Constants.CONNECTION_STD_TIMEOUT;

		try {
			mSocket.setSoTimeout((int) mTimeOut);
			mSocket.setTcpNoDelay(false);
		} catch (SocketException e) {

		}
	}

	/**
	 * Returns the connection id.
	 *
	 * @return the connection id.
	 */
	public pgrid.GUID getGUID() {
		return mGUID;
	}

	/**
	 * Returns the used protocol string.
	 *
	 * @return the protocol string.
	 */
	public String getProtocolString() {
		return mProtocolString;
	}

	/**
	 * Sets the used protocol string.
	 *
	 * @param protocolStr the protocol string.
	 */
	public void setProtocolString(String protocolStr) {
		mProtocolString = protocolStr;
	}

	/**
	 * Returns the number of received bytes.
	 *
	 * @return the number of received bytes.
	 */
	public long getReceivedBytes() {
		return mReceivedBytes;
	}

	/**
	 * Returns the number of received messages.
	 *
	 * @return the number of received messages.
	 */
	public int getReceivedCount() {
		return mReceivedCount;
	}

	/**
	 * Increases the number of received bytes from the host.
	 *
	 * @param bytes the number of received bytes.
	 */
	public void incReceivedBytes(long bytes) {
		mReceivedBytes += bytes;
	}

	/**
	 * Increases the number of received messages from the host.
	 */
	public void incReceivedCount() {
		mReceivedCount++;
	}

	/**
	 * Returns the number of sent bytes.
	 *
	 * @return the number of sent bytes.
	 */
	public long getSentBytes() {
		return mSentBytes;
	}

	/**
	 * Returns the number of sent messages.
	 *
	 * @return the number of sent messages.
	 */
	public int getSentCount() {
		return mSentCount;
	}

	/**
	 * Increases the number of sent bytes from the host.
	 *
	 * @param bytes the number of sent bytes.
	 */
	public void incSentBytes(long bytes) {
		mSentBytes += bytes;
	}

	/**
	 * Increases the number of sent messages to the host.
	 */
	public void incSentCount() {
		mSentCount++;
	}

	/**
	 * Returns the socket.
	 *
	 * @return the socket.
	 */
	public Socket getSocket() {
		return mSocket;
	}

	/**
	 * Sets the socket.
	 *
	 * @param socket the socket.
	 */
	public void setSocket(Socket socket) {
		mSocket = socket;

		try {
			mSocket.setSoTimeout((int) mTimeOut);
			mSocket.setTcpNoDelay(false);
		} catch (SocketException e) {

		}
	}

	/**
	 * Returns the status of the connection.
	 *
	 * @return the status.
	 */
	public short getStatus() {
		return mStatus;
	}

	/**
	 * Returns a string representation of the status.
	 *
	 * @return a string representation of the status.
	 */
	public String getStatusString() {
		if (mLastStatusMsg != null)
			return mLastStatusMsg;
		switch (mStatus) {
			case STATUS_NOT_CONNECTED:
				return "Not connected";
			case STATUS_ERROR:
				return "Error";
			case STATUS_CLOSING:
				return "Closing";
			case STATUS_CONNECTING:
				return "Connecting";
			case STATUS_ACCEPTING:
				return "Accepting";
			case STATUS_CONNECTED:
				return "Connected";
		}
		return "Unknown";
	}

	/**
	 * Sets the status of the connection.
	 *
	 * @param status the status.
	 */
	public void setStatus(short status) {
		setStatus(status, null);
	}

	/**
	 * Sets the status of the connection with a given status message.
	 *
	 * @param status the status.
	 * @param msg    the status message.
	 */
	public void setStatus(short status, String msg) {
		if (mStatus == status)
			return;
		mStatus = status;
		mLastStatusMsg = msg;
		switch (status) {
			case STATUS_NOT_CONNECTED:
			case STATUS_ERROR:
				mConnectionStartTime = 0;
				mReceivedBytes = 0;
				mSentBytes = 0;
				break;
			case STATUS_CONNECTING:
			case STATUS_ACCEPTING:
				mConnectingStartTime = System.currentTimeMillis();
				break;
			case STATUS_CONNECTED:
				resetIOTimer();
				mConnectionStartTime = System.currentTimeMillis();
				break;
		}
	}

	public void setIOTimeOut(long timeout) {
		mTimeOut = timeout;
	}

	public long getIOTimeOut() {
		return mTimeOut;
	}


}