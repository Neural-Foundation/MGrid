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
import pgrid.Properties;
import pgrid.Constants;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.protocol.InitMessage;
import pgrid.network.protocol.InitResponseMessage;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.zip.Deflater;

/**
 * The Communication Acceptor handles incomming connections from remote host.
 * It tries to identitify the used protocol of the remote host, and starts then
 * the corresponding worker.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
class Acceptor implements Runnable {

	/**
	 * The start of a P-Grid greeting.
	 */
	private static final String PGRID_GREETING = "P-GRID";
	
	/**
	 * The Communication Manager.
	 */
	private ConnectionManager mConnMgr = ConnectionManager.sharedInstance();

	/**
	 * The connection.
	 */
	private Connection mConn = null;

	/**
	 * The already reveived greeting.
	 */
	private String mGreeting = null;

	/**
	 * NaFT received greeting
	 */
	private String mNaFTGreeting = null;
	
	/**
	 * The connection.
	 */
	private PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 * Creates a new worker for a delivered socket.
	 *
	 * @param conn the connection to handle.
	 */
	Acceptor(Connection conn) {
		mConn = conn;
	}

	/**
	 * Creates a new worker for a delivered socket.
	 *
	 * @param conn     the connection to handle.
	 * @param greeting the received greeting.
	 */
	Acceptor(Connection conn, String greeting) {
		mConn = conn;
		mGreeting = greeting;
	}

	/**
	 * Starts to decide the used protocol and start the corresponding worker.
	 */
	public void run() {
		try {
			ConnectionReader reader = new ConnectionReader(mConn.getSocket().getInputStream(), mConn);
			ConnectionWriter writer = new ConnectionWriter(mConn.getSocket().getOutputStream());
			boolean firewallStatus;
			
			// read and check greeting
			if (mGreeting == null){
				mGreeting = reader.readGreeting();
			}
			if (mGreeting == null) {			
				mConn.setStatus(Connection.STATUS_ERROR, "Timeout");
				mConnMgr.acceptanceFinished(mConn);
				return;
			}
			if (mGreeting.startsWith(PGRID_GREETING)) {
				// P-Grid
				InitMessage msgInit = new InitMessage(mGreeting);
				if (!msgInit.isValid()) {
					mConn.setStatus(Connection.STATUS_ERROR, "Invalid");
					mConnMgr.acceptanceFinished(mConn);
					return;
				} else {
					// check if remote host is behind a firewall
					if (msgInit.getHeaderField(InitMessage.HEADER_FIREWALLED).toLowerCase().equals("yes")) {
						mConn.setIOTimeOut(Constants.CONNECTION_TIMEOUT_FIREWALLED_PEER);
						firewallStatus = true;
					} else {
						mConn.setIOTimeOut(Constants.CONNECTION_STD_TIMEOUT);
						firewallStatus = false;
					}

				}

				// write response
				InitResponseMessage msgInitResp = new InitResponseMessage(mPGridP2P.getLocalHost().getGUID());
				boolean compression = (msgInit.getHeaderField(InitMessage.HEADER_COMPRESSION).toLowerCase().equals("yes") ? true : false);
				if ((mPGridP2P.propertyInteger(Properties.COMPRESSION_LEVEL) != Deflater.NO_COMPRESSION) && (compression)) {
					msgInitResp.setHeaderField(InitResponseMessage.HEADER_COMPRESSION, "yes");
					mConn.setCompression(true);
				} else {
					msgInitResp.setHeaderField(InitResponseMessage.HEADER_COMPRESSION, "no");
					mConn.setCompression(false);
				}
				msgInitResp.setHeaderField(InitResponseMessage.HEADER_COMPRESSION, "yes");
				// Constants.LOGGER.finest("Init response message:\n" + msgInitResp.toXMLString());
				writer.write(msgInitResp.getBytes());
				mConn.setStatus(Connection.STATUS_CONNECTED);
				mConn.setProtocolString(msgInit.getVersion());
				//PGridHost host = PGridHost.getHost(pgrid.GUID.getGUID(msgInit.getHeaderField(InitMessage.HEADER_GUID)), mConn.getSocket().getInetAddress(), Integer.parseInt(msgInit.getHeaderField(InitMessage.HEADER_PORT)));
				PGridHost host = PGridHost.getHost(pgrid.GUID.getGUID(msgInit.getHeaderField(InitMessage.HEADER_GUID)), InetAddress.getByName(msgInit.getHeaderField(InitMessage.HEADER_INTERNAL_ADDRESS)), Integer.parseInt(msgInit.getHeaderField(InitMessage.HEADER_PORT)));
				host.setFirewalledStatus(firewallStatus);
				host.setExternalIP(mConn.getSocket().getInetAddress());
				mConn.setHost(host);
				//mConn.setPeer(XMLPGridHost.getPeer(pgrid.GUID.getGUID(msgInit.getHeaderField(InitMessage.HEADER_GUID)), mConn.getSocket().getIP(), Integer.parseInt(msgInit.getHeaderField(InitMessage.HEADER_PORT))));
				mConnMgr.acceptanceFinished(mConn);
			
			} else if (mGreeting.startsWith(NaFTManager.NaFT_GREETING)) {
				// This connection is useful for a host to know its external IP address
				Constants.LOGGER.log(Level.CONFIG, "NaFT greeting received from " + mConn.getSocket().getInetAddress().getHostAddress() + ":" + mConn.getSocket().getPort());
				writer.write(mConn.getSocket().getInetAddress().getHostAddress().getBytes());
				writer.write("\n".getBytes());
				mGreeting = null;
				mConn.close();
				return;
			} else {
				mConn.setStatus(Connection.STATUS_ERROR, "Invalid");
				mConnMgr.acceptanceFinished(mConn);
				return;
			}
		} catch (ConnectionClosedException e) {
			mConn.setStatus(Connection.STATUS_ERROR, "Invalid");
			mConnMgr.acceptanceFinished(mConn);
			return;
		} catch (ConnectionTimeoutException e) {
			mConn.setStatus(Connection.STATUS_ERROR, "Timeout");
			mConnMgr.acceptanceFinished(mConn);
			return;
		} catch (IOException e) {
			mConn.setStatus(Connection.STATUS_ERROR);
			mConnMgr.acceptanceFinished(mConn);
			return;
		}
	}

}