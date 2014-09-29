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

import pgrid.Properties;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.protocol.InitMessage;
import pgrid.network.protocol.InitResponseMessage;
import static pgrid.network.PGridSSL.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.zip.Deflater;

/**
 * The Communication Connector establishes a connections to a remote host.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
class Connector implements Runnable {

	/**
	 * Timout to wait for a connection.
	 */
	private static final int CONNECT_TIMEOUT = 60 * 3; // ~ 3m.

	/**
	 * Timout to wait for a message to receive.
	 */
	private static int SO_TIMEOUT = 1000*10; // ~ 20 sec.

	/**
	 * The Communication Manager.
	 */
	private ConnectionManager mConnMgr = ConnectionManager.sharedInstance();

	/**
	 * The connection.
	 */
	private Connection mConn = null;

	/**
	 * The address of the host to connect.
	 */
	private InetAddress mInetAddr = null;

	/**
	 * The PGridP2P facility.
	 */
	private PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 * The port of the host to connect.
	 */
	private int mPort = -1;

	/**
	 * The Communication Reader.
	 */
	private ConnectionReader mReader = null;

	/**
	 * The socket to the host.
	 */
	private Socket mSocket = null;

	/**
	 * The Communication Writer.
	 */
	private ConnectionWriter mWriter = null;

	/**
	 * Creates a new worker to establish the connection.
	 *
	 * @param conn the connection.
	 */
	public Connector(Connection conn) {
		mConn = conn;
		mInetAddr = conn.getHost().getIP();
		mPort = conn.getHost().getPort();
	}

	/**
	 * Connect with Gridella protocol.
	 *
	 * @return the GUID of the connected host.
	 */
	private pgrid.GUID handshakePGrid() {
		try {
			// send greeting
			InitMessage msgInit = new InitMessage(mPGridP2P.getLocalHost().getGUID(),mPGridP2P.getLocalHost().getIP(), mPGridP2P.getLocalHost().getPort(), mConnMgr.isFirewalled());
			if (mPGridP2P.propertyInteger(Properties.COMPRESSION_LEVEL) != Deflater.NO_COMPRESSION)
				msgInit.setHeaderField(InitMessage.HEADER_COMPRESSION, "yes");
			else
				msgInit.setHeaderField(InitMessage.HEADER_COMPRESSION, "no");
			// Constants.LOGGER.finest("Init message:\n" + msgInit.toXMLString());
			mWriter.write(msgInit.getBytes());

			// receive response
			String response = mReader.readGreeting();
			if (response == null) {
				mConn.setStatus(Connection.STATUS_ERROR, "Timeout in handshake (no remote reply)");
				mConnMgr.connectingFinished(mConn, null);
				return null;
			}
			InitResponseMessage msg = new InitResponseMessage(response);
			if (!msg.isValid()) {
				mConn.setStatus(Connection.STATUS_ERROR, "Invalid");
				mConnMgr.connectingFinished(mConn, null);
				return null;
			}
			mConn.setCompression((msgInit.getHeaderField(InitMessage.HEADER_COMPRESSION).toLowerCase().equals("yes") ? true : false));
			pgrid.GUID returnGUID = pgrid.GUID.getGUID(msg.getHeaderField(InitResponseMessage.HEADER_GUID));
			return returnGUID;
		} catch (IllegalArgumentException e) {
			return null;
		} catch (ConnectionClosedException e) {
			mConn.setStatus(Connection.STATUS_ERROR, "Refused");
			mConnMgr.connectingFinished(mConn, null);
			return null;
		} catch (ConnectionTimeoutException e) {
			mConn.setStatus(Connection.STATUS_ERROR, "Timeout in handshake");
			mConnMgr.connectingFinished(mConn, null);
			return null;
		} catch (IOException e) {
			mConn.setStatus(Connection.STATUS_ERROR);
			mConnMgr.connectingFinished(mConn, null);
			return null;
		}
	}

	/**
	 * Opens a socket to the remote host delivered by its name and port.
	 *
	 * @return <code>true</code> if the socket was created successful.
	 */
	private boolean openSocket() {
		if (mPort < 0) {
			mConn.setStatus(Connection.STATUS_ERROR, "Not available");
			mConnMgr.connectingFinished(mConn, null);
			return false;
		}

		Thread timeoutThread = new Thread() {
			public void run() {
				try {
					Thread.sleep(10*1000);
				} catch (InterruptedException e) {
					return;
				}
				if ((mSocket == null) && (mConn.getStatus() != Connection.STATUS_ERROR)) {
					mConn.setStatus(Connection.STATUS_ERROR, "Timeout in open socket");
					mConnMgr.connectingFinished(mConn, null);
				}
			}
		};
		timeoutThread.setDaemon(true);
		timeoutThread.start();
		try {
			if (mInetAddr == null) {
				mConn.getHost().resolve();
				mInetAddr = mConn.getHost().getIP();
				if (mInetAddr == null) {
					mConn.setStatus(Connection.STATUS_ERROR, "Not available");
					mConnMgr.connectingFinished(mConn, null);
					return false;
				}
			}
            mSocket = PGridSocket.sharedInstance().getSocket(mInetAddr,mPort);
            mSocket.setSoLinger(true, CONNECT_TIMEOUT);
			mSocket.setTcpNoDelay(false);
			timeoutThread.interrupt();
			if (mConn.getStatus() == Connection.STATUS_ERROR) return false;

		} catch (SocketException e) {
			if (timeoutThread.isAlive()) {
				timeoutThread.interrupt();
				mConn.setStatus(Connection.STATUS_ERROR, "Not available");
				mConnMgr.connectingFinished(mConn, null);
			}
			return false;
		} catch (IOException e) {
			if (timeoutThread.isAlive()) {
				timeoutThread.interrupt();
				mConn.setStatus(Connection.STATUS_ERROR, "Not available");
				mConnMgr.connectingFinished(mConn, null);
			}
			return false;
		}
		mConn.setSocket(mSocket);
		return true;
	}

	/**
	 * Starts the worker. The handshake between the hosts is done, and the
	 * decided protocol is stored for the remote host.
	 */
	public void run() {
		if (!openSocket())
			return;
		try {
			mReader = new ConnectionReader(mSocket.getInputStream(), mConn);
			mWriter = new ConnectionWriter(mSocket.getOutputStream());
		} catch (IOException e) {
			mConn.setStatus(Connection.STATUS_ERROR);
			mConnMgr.connectingFinished(mConn, null);
			return;
		}

		pgrid.GUID guid;
		if ((guid = handshakePGrid()) == null)
			return;
		mConn.setStatus(Connection.STATUS_CONNECTED);
		mConnMgr.connectingFinished(mConn, guid);
	}

}