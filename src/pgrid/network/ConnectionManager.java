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

import p2p.basic.GUID;
import pgrid.Constants;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.PGridHost;
import pgrid.Properties;
import pgrid.core.maintenance.identity.IdentityManager;
import pgrid.network.protocol.PGridMessage;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

/**
 * The Communication Manager adminstrates all connection to other hosts.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class ConnectionManager {
	/**
	 * Offline period of a host
	 */
	//private static final int OFFLINE_PERIOD = 1000*20; // 20s for testes
	//private static final int OFFLINE_PERIOD = 1000 * 60 * 2; // 5m.

	/**
	 * Timout to wait for a connection.
	 */
	private static final int CONNECT_TIMEOUT = 1000 * 20; // ~ 1m.

	/**
	 * Timout to wait for a connection.
	 */
	private static final int CONNECTING_TIMEOUT = 1000 * 20; // ~ 20.

	/**
	 * The reference to the only instance of this class (Singleton
	 * pattern). This differs from the C++ standard implementation by Gamma
	 * et.al. since Java ensures the order of static initialization at runtime.
	 *
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	private static final ConnectionManager SHARED_INSTANCE = new ConnectionManager();

	/**
	 * List of connecting connections.
	 */
	private Hashtable mConnectings = new Hashtable();

	/**
	 * List of waiters on connections.
	 */
	private Hashtable mConnectionLock = new Hashtable();

	/**
	 * The Message Manager.
	 */
	private MessageManager mMsgMgr = null;

	/**
	 * Hashtable of all PGridP2P connections, indexed by the GUID.
	 */
	private Hashtable mConnections = new Hashtable();

	/**
	 * true if this peer is behind a firewall
	 */
	private boolean mFirewalled = false;

		/**
	 * Hashtable of all PGridP2P connections, indexed by the GUID.
	 */
	private Listener mListener = new Listener();

	/**
	 * Hashtable of all Writers, by Host GUID.
	 */
	private Hashtable mWriters = new Hashtable();

	/**
	 * Hashtable of all timestamp of offline host, by Host GUID.
	 */
	//private Hashtable mOfflineHostTimestamps = new Hashtable();

	/**
	 * The Identity Manager.
	 */
	private IdentityManager mIdentMgr = null;

	/**
	 * True if connections must be challenged before acceptance
	 */
	private boolean mSecuredConnection = false;

	/**
	 * Number of connection attemps.
	 */
	protected int mAttemps;

	/**
	 * The constructor must be protected to ensure that only subclasses can
	 * call it and that only one instance can ever get created. A client that
	 * tries to instantiate PGridP2P directly will get an error at compile-time.
	 */
	protected ConnectionManager() {
	}

	/**
	 * This creates the only instance of this class. This differs from the C++ standard implementation by Gamma et.al.
	 * since Java ensures the order of static initialization at runtime.
	 *
	 * @return the shared instance of this class.
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	public static ConnectionManager sharedInstance() {
		return SHARED_INSTANCE;
	}

	/**
	 * Processes an incoming connection accepted by the CommListener.
	 *
	 * @param socket the socket.
	 */
	void accept(Socket socket) {		
		Connection conn = new Connection(socket);

		conn.setStatus(Connection.STATUS_ACCEPTING);
		Thread t = new Thread(new Acceptor(conn), "Acceptor - " + conn.getGUID());
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Processes an incoming connection.
	 *
	 * @param socket   the socket.
	 * @param greeting the already received greeting.
	 */
	public void accept(Socket socket, String greeting) {
		Connection conn = new Connection(socket);

		conn.setStatus(Connection.STATUS_ACCEPTING);
		Thread t = new Thread(new Acceptor(conn, greeting), "Acceptor - " + conn.getGUID());
		t.setDaemon(true);
		t.start();
	}

	/**
	 * The acceptance of an incoming connection is finished.
	 *
	 * @param conn connection.
	 */
	public void acceptanceFinished(Connection conn) {
		Object connectionLock = null;

		// if host is null, an error has happend in the greating phase.
		if (conn.getHost() == null) {
			if (PGridP2P.sharedInstance().isInDebugMode()) {
				Constants.LOGGER.finest("Incomming connection with host " +
						conn.getSocket().getInetAddress().getCanonicalHostName() +
						" failed with status: "+conn.getStatusString());
			}

			return;
		}

		// process the acceptance
		synchronized (mConnectionLock) {
			connectionLock = mConnectionLock.get(conn.getHost());
			// give the coin to only one thread
			if (connectionLock == null) {
				connectionLock = new Object();
				mConnectionLock.put(conn.getHost(), connectionLock);
			}
		}
		synchronized (connectionLock) {
			if (conn.getStatus() == Connection.STATUS_CONNECTED) {
				// check if a connection already exists
				Connection oldConn = (Connection)mConnections.get(conn.getHost().getGUID());
				if (oldConn != null) {
					// a connection already exists => disconnect new one and return
					Constants.LOGGER.finer("Additional connection (" + oldConn.getStatusString() + ") to host " + oldConn.getHost().toHostString() + " will be closed later on.");
				}
				mConnections.put(conn.getHost().getGUID(), conn);

				mWriters.put(conn.getHost().getGUID(), new PGridWriter(conn));
				PGridReader pr = new PGridReader(conn, mMsgMgr);
				Thread t = new Thread(pr, "Reader for '" + conn.getHost().toHostString() + "' - " + conn.getGUID());
				t.setDaemon(true);
				t.start();

				if (PGridP2P.sharedInstance().isInDebugMode())
					Constants.LOGGER.finest("Incomming connection ("+conn.getGUID()+") with host " + (conn.getHost() == null? "\"unknown\"": conn.getHost().toHostString()) + " established.");
			}
			else if (PGridP2P.sharedInstance().isInDebugMode()) {
				Constants.LOGGER.finest("Incomming connection with host " + (conn.getHost() == null? "\"unknown\"": conn.getHost().toHostString()) + " failed with status: "+conn.getStatusString());
			}
		}
	}

	/**
	 * Connects the host with the given protocol.
	 *
	 * @param host the host.
	 * @return the connection.
	 */
	public Connection connect(PGridHost host) {
		
		Constants.LOGGER.finest("Trying to get a connection for host " + host.getGUID());
		String connections = "";
		for (Enumeration e = mConnections.keys() ; e.hasMoreElements() ;) {
			GUID g = (GUID)e.nextElement();
			connections += g.toString();
			try {
				if(host!=null && host.getGUID() != null){
					if (g.toString().equals(host.getGUID().toString())){
						connections += "  <---- FOUND !!";
					}
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				System.out.println("host(error) :"+host.toHostString());
				return null;
			}
	        connections += "\n";
	         
	     }
		Constants.LOGGER.finest("Current available connections (" + mConnections.size() + ") :\n" + connections);
		
		Connection conn;
		Object waiter = null;
		Object connectingWaiter = null;
		boolean stop = false;

		// try to find existing connection, if a PGrid connection is requested and the host is no bootstrap host
		// create a queue if needed
		synchronized (mConnectionLock) {
			connectingWaiter = mConnectionLock.get(host);
			// give the coin to only one thread
			if (connectingWaiter == null) {
				connectingWaiter = new Object();
				mConnectionLock.put(host, connectingWaiter);
			}
		}
		synchronized (connectingWaiter) {
			// if the thread does not have the coin, wait
			while (!stop) {
				if ((host != null) && (host.getGUID() != null)) {
					conn = (Connection)mConnections.get(host.getGUID());

					if (conn != null) {
						return conn;
					}
				}

				waiter = mConnectings.get(host);
				if (waiter != null) {
					try {
						connectingWaiter.wait(CONNECTING_TIMEOUT);
					} catch (InterruptedException e) {
						// do nothing here
					}
					if (host.getState() != PGridHost.HOST_OK) {
						return null;
					}
				} else {
					//give the coin to only one thread
					waiter = new Object();
					mConnectings.put(host, waiter);
					stop = true;
				}

			}

		}

		if (PGridP2P.sharedInstance().isInDebugMode()) {
			Constants.LOGGER.finest("Try to establish a new connection for host: " + host.toHostString());
		}

		// establish new connection
		conn = new Connection(host);
		conn.setStatus(Connection.STATUS_CONNECTING);
		Thread t = new Thread(new Connector(conn), "Connect to '" + host.toHostString() + "' - " + conn.hashCode());
		t.setDaemon(true);
		t.start();

		// wait for established connection

		synchronized (waiter) {
			while(conn.getStatus() == Connection.STATUS_CONNECTING) {
				try {
					waiter.wait(CONNECT_TIMEOUT);
				} catch (InterruptedException e) {
				}
			}
		}
		synchronized(connectingWaiter) {
			mConnectings.remove(host);
			connectingWaiter.notifyAll();
		}
		Connection newConn = null;

		/*if (host.getGUID() != null)
					newConn = (Connection)mConnections.get(host.getGUID());*/

		return (newConn == null ? conn : newConn);
	}

	/**
	 * Establishing a connection has finished.
	 *
	 * @param conn the connection.
	 * @param guid an GUID containing further informations.
	 */
	void connectingFinished(Connection conn, pgrid.GUID guid) {
		boolean challengeSucceeded = true;
		boolean bootstrap = false;
		PGridHost host = conn.getHost();
		Thread readerThread = null;

		// if the host uses a temp. GUID (because it was not know before) => set the correct guid
		// INFO (Roman): I changed it to guid only because the GUID is temp. for bootstrap requests
		if (host.isGUIDTmp()) {
			host.setGUID(guid);
		}

		// connection has been established
		if (conn.getStatus() == Connection.STATUS_CONNECTED) {
			if (host.getGUID() == null) {
				bootstrap = true;
				host.setGUID(guid);
			}


			mWriters.put(host.getGUID(), new PGridWriter(conn));
			PGridReader pr = new PGridReader(conn, mMsgMgr);
			readerThread = new Thread(pr, "Reader for '" + host.toHostString() + "' - " + conn.getGUID());
			readerThread.setDaemon(true);
			readerThread.start();
			if (PGridP2P.sharedInstance().isInDebugMode())
				Constants.LOGGER.finer("Connection "+conn.getGUID()+" with host '" + host.toHostString() + "' established with code: "+conn.getStatusString()+".");
			if (mSecuredConnection && !bootstrap) {
				Constants.LOGGER.fine("Challenging host " + host.toHostString() + "...");
				if (!mIdentMgr.challengeHost(host, conn)) {
					host.setState(PGridHost.HOST_STALE);
					Constants.LOGGER.fine("Challenge failed for host '" + host.toHostString() + "'!");
					conn.close();
					conn.setStatus(Connection.STATUS_ERROR);
					challengeSucceeded = false;
					mWriters.remove(host.getGUID());
				} else {
					host.resetOfflineTime();
					Constants.LOGGER.fine("Challenge succeeded for host '" + host.toHostString() + "'!");
				}
			}
		} // connection has not been established

		 // inform waiting thread that the connection is established
		Object connectionLock = null;
		synchronized (mConnectionLock) {
			connectionLock = mConnectionLock.get(host);
			// give the coin to only one thread
			if (connectionLock == null) {
				connectionLock = new Object();
				mConnectionLock.put(host, connectionLock);
			}
		}
		synchronized (connectionLock) {
			if (conn.getStatus() == Connection.STATUS_CONNECTED) {
				mConnections.put(host.getGUID(), conn);
				host.resetMappingAttemps();
			}

			Object t = mConnectings.get(host);

			if (t != null) {
				synchronized (t) {
					t.notifyAll();
				}
			}
		}
	}

	/**
	 * Initializes the Connection Manager.
	 *
	 * @param startListener <tt>true</tt> if the connection listener should be started, <tt>false</tt> otherwise.
	 */
	public void init(boolean startListener) {
		mMsgMgr = MessageManager.sharedInstance();
		mSecuredConnection = PGridP2P.sharedInstance().propertyBoolean(Properties.IDENTITY_CHALLENGE);

		if (mSecuredConnection)
			mIdentMgr = IdentityManager.sharedInstance();

		mAttemps = PGridP2P.sharedInstance().propertyInteger(Properties.IDENTITY_CONNECTION_ATTEMPS);
		
		if (startListener) {
			
			if (mListener.isListenning()) {
				Constants.LOGGER.fine("P-Grid Listener is already active. No need to start a new thread.");
			} else {
				Thread t = new Thread(mListener, "P-Grid Listener");
				t.setDaemon(true);
				t.start();
			}
		}
	}

	/**
	 * Try to reconnect a failed connection by updating the host IPort.
	 *
	 * @param host the host to connect.
	 * @return the established connection or null.
	 */
	private Connection reconnect(PGridHost host) {
		Constants.LOGGER.fine("try to reconnect '" + host.toHostString() + "' ...");

		// establish new connection
		Connection conn = new Connection(host);
		conn.setStatus(Connection.STATUS_CONNECTING);
		Connector myConnector = new Connector(conn);
		//myConnector.run();
		Thread t = new Thread(new Connector(conn), "Reconnect to '" + host.toHostString() + "' - " + conn.getGUID());
		t.setDaemon(true);
		t.start();

		synchronized (t) {
			try {
				t.join();
			} catch (InterruptedException e) {
			}
		}

		return conn;
	}

	/**
	 * The socket of the delivered connection was closed by the remote host.
	 *
	 * @param conn the connection.
	 */
	//FIXME: add a bool to notify connection
	public void socketClosed(Connection conn) {
		Object connectionLock = null;

		synchronized (mConnectionLock) {
			connectionLock = mConnectionLock.get(conn.getHost());
			// give the coin to only one thread
			if (connectionLock == null) {
				connectionLock = new Object();
				mConnectionLock.put(conn.getHost(), connectionLock);
			}
		}
		synchronized (connectionLock) {
			if (mConnections.containsValue(conn))
				mConnections.remove(conn.getHost().getGUID());

		}
		conn.close();

	}

	/**
	 * The socket of the delivered connection was closed by the remote host.
	 *
	 * @param conn the connection.
	 */
	//FIXME: add a bool to notify connection
	public void socketClosed(Connection conn, boolean lingerSOTimeout) {
		Object connectionLock = null;

		if (!lingerSOTimeout) {
			if (conn.getSocket() != null) {
				try {
					conn.getSocket().setSoLinger(false, 0);
				} catch (SocketException e) {
					// do nothing
				}
			}
		}

		socketClosed(conn);
	}


	/**
	 * Sends the delivered message to the delivered host. This method is synchroneous.
	 *
	 * @param host the receiving host.
	 * @param msg  the message to send.
	 * @return <code>true</code> if the message was sent sucessfull, <code>false</code> otherwise.
	 */
	public boolean sendPGridMessage(PGridHost host, PGridMessage msg) {
		// if the host is offline, don't try to send this message
		synchronized (host) {
			if (host.getState() == PGridHost.HOST_OFFLINE) {
				return false;
			}
		}
		Connection conn = connect(host);
		if (conn != null && conn.getStatus() == Connection.STATUS_CONNECTED) {
			PGridWriter writer = (PGridWriter)mWriters.get(host.getGUID());
			if (writer != null) {
				if (writer.sendMsg(msg)) {
					if (PGridP2P.sharedInstance().isInDebugMode()) {
						Constants.LOGGER.finest("Message : " + msg.getGUID()+" sent successfully.");
					}
					return true;
				}
			} else if (PGridP2P.sharedInstance().isInDebugMode()) {
				Constants.LOGGER.finest("No writer for host: " + host.toHostString()+".");
			}
		}

		if (PGridP2P.sharedInstance().isInDebugMode()) {
			Constants.LOGGER.finest("PGrid " + msg.getDescString() + " Message failed to be sent to " + host.toHostString() + (conn != null?" [Connection status: "+conn.getStatusString()+"].":"[Connection is null]."));
		}
		
		/**
		 * NaFT handling
		 */
		
		// Try to send message through a relay
		PGridHost relay = PGridP2P.sharedInstance().getNaFTManager().getRelay(host);
		
		if (relay == null || PGridP2P.sharedInstance().getNaFTManager().isMySelf(relay)) return false;
		
		NaFTManager.LOGGER.finest("Trying to send the message for " + host.toHostString() + " via relay host " + relay.toHostString());
		msg.getHeader().setSourceHost(msg.getHeader().getHost());
		msg.getHeader().setDestinationHost(host);
		
		conn = connect(relay);
		if (conn != null && conn.getStatus() == Connection.STATUS_CONNECTED) {
			PGridWriter writer = (PGridWriter)mWriters.get(relay.getGUID());
			if (writer != null) {
				if (writer.sendMsg(msg)) {
					if (PGridP2P.sharedInstance().isInDebugMode()) {
						NaFTManager.LOGGER.finest("Message : " + msg.getGUID()+" sent successfully.");
					}
					return true;
				}
			} else if (PGridP2P.sharedInstance().isInDebugMode()) {
				NaFTManager.LOGGER.finest("No writer for relay host: " + relay.toHostString()+".");
			}
		}
			
		return false;
	}

	/**
	 * Sends the delivered message to the delivered host through a specific connection.
	 *
	 * @param host the receiving host.
	 * @param conn the connection to the host.
	 * @param msg  the message to send.
	 * @return <code>true</code> if the message was sent sucessfull, <code>false</code> otherwise.
	 */
	boolean sendPGridMessage(PGridHost host, Connection conn, PGridMessage msg) {
		if (conn.getStatus() == Connection.STATUS_CONNECTED) {
			PGridWriter writer = (PGridWriter)mWriters.get(host.getGUID());
			if (writer != null) {
				return writer.sendMsg(msg);
			}
		}
		return false;
	}

	/**
	 * Returns the connection for the given host and protocol.
	 *
	 * @param host the host.
	 * @return the connection.
	 */
	public Connection getConnection(PGridHost host) {
		Connection conn = null;
		// try to find existing connection
		if (host.getGUID() != null) {
			conn = (Connection)mConnections.get(host.getGUID());
		}
		return conn;
	}

	/**
	 * Returns all connections
	 *
	 * @return all connections.
	 */
	public Collection getConnections() {
		return mConnections.values();
	}

	/**
	 * This method is called by the PGridReader when a timeout occurs. A timeout trigger the disconnection between this
	 * peer and the remote one.
	 * @param conn
	 */
	public void connectionTimeout(Connection conn) {
		long time = System.currentTimeMillis();

		if (mConnections.containsValue(conn))
			mConnections.remove(conn.getHost().getGUID());

		// check if it is time to close the connection
		if ((time - conn.getLastIOTime()) >= conn.getIOTimeOut() && conn.getStatus() == Connection.STATUS_CONNECTED) {
			conn.setStatus(Connection.STATUS_CLOSING, "Timeout");

			conn.resetIOTimer();
		}
		// if timeout, wait an extra 5s and close connection
		else if ((time - conn.getLastIOTime()) > 5*1000 && conn.getStatus() != Connection.STATUS_CONNECTED){
			conn.setStatus(Connection.STATUS_ERROR, "Closed");
			socketClosed(conn);
		}
	}

	/**
	 * Stop listening
	 */
	public void stopListening() {
		mListener.stopListening();
	}

	/**
	 * Restart listening
	 */
	public void restartListening() {
		mListener.restartListening();
	}

	/**
	 * Set the firewall status
	 * @param status the firewall status
	 */
	public void setFirewallStatus(boolean status) {
		mFirewalled = status;
	}

	/**
	 * Returns true if this peer is behind a firewall
	 * @return true if this peer is behind a firewall
	 */
	public boolean isFirewalled() {
		return mFirewalled;
	}


}