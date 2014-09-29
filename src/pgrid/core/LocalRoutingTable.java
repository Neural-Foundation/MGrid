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

package pgrid.core;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import p2p.basic.GUID;
import pgrid.Constants;
import pgrid.PGridHost;
import pgrid.Properties;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.ConnectionManager;
import pgrid.network.NaFTManager;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.logging.Level;

/**
 * This class represents the Routing Table of the P-Grid facility. It includes
 * the fidget hosts, the hosts for each level of a path, and the replicas.
 * 
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class LocalRoutingTable extends XMLRoutingTable {
	
	/**
	 * The file to store the routing table.
	 */
	private File mFile = null;

	/**
	 * The P-Grid facility.
	 */
	private PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 * If set, no more saves are allowed.
	 */
	private boolean mShutdownFlag = false;

	/**
	 * True iif the local peer had no ID
	 */
	private boolean mNewIdentity = false;

	/**
	 * True if the local peer has changed its IP
	 */
	private boolean mModifiedIP = false;

	/**
	 * Local peer proxy
	 */
	private PGridHost mProxy = null;

	private int mGetIpAttempts = 10;
	private long mWaitingTime = 2000 * 1; // By default, wait 1 second 

	/**
	 * Create the routing table with the given file. If the given file exists,
	 * it is read in, otherwise it is created.
	 * 
	 * @param routeFile
	 *            the file to store the routing table.
	 * @param port
	 *            the current listening port.
	 */
	public LocalRoutingTable(String routeFile, int port) {
		super();

		mFile = new File(routeFile);
		// create all dirs if they are not already created
		createHierachy();

		// if no file exists, store the initial values
		if ((!mFile.exists()) || (mFile.length() <= 0)) {


			String bootstrapHosts[] = mPGridP2P.propertyString(Properties.BOOTSTRAP_HOSTS).split(";");
			boolean gotIP = false;

			for (int j = 0; j < mGetIpAttempts; j++) {
			
				for (int i = 0; i < bootstrapHosts.length; i++) {
					
					String boot[] = null;
					try {
						boot = bootstrapHosts[i].split(":");
					} catch (ArrayIndexOutOfBoundsException e) {
						Constants.LOGGER.log(Level.SEVERE, "ArrayIndexOutOfBoundsException:" + e.getMessage()
								+ ".\nThe bootstrap host config entry should look like: '1.2.3.4:1805'");
						mPGridP2P.gracefulShutdown("The bootstrap host config entry should look like: '1.2.3.4:1805'. Exiting P-Grid.");
						return;
					}
					Constants.LOGGER.info("Connecting to bootstrap host: " + boot[0] + ":" + boot[1]);
					boolean isBootStrapHost = false;
	
					if (port == Integer.parseInt(boot[1])) {
						try {
							NIC: for (Enumeration e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
								NetworkInterface nic = (NetworkInterface) e.nextElement();
								for (Enumeration n = nic.getInetAddresses(); n.hasMoreElements();) {
									InetAddress inet = (InetAddress) n.nextElement();
									Constants.LOGGER.config("Current host(NIC): " + inet.getHostAddress()+"("+inet.getHostName()+")" + ":" + port);
									if (boot[0].equals(inet.getHostAddress()) || boot[0].equals(inet.getHostName())) {
										isBootStrapHost = true;
										break NIC;
									}
	
								}
							}
						} catch (SocketException e) {
							Constants.LOGGER.log(Level.SEVERE, "No IP address could be found: " + e.getMessage());
						}
					}
	
					if (isBootStrapHost) {
						Constants.LOGGER.config("Has the same IP as the bootstrap host ... ");
						try {
							mLocalHost = PGridHost.getHost(InetAddress.getByName(boot[0]), port, true);
							mLocalHost.setExternalIP(InetAddress.getByName(boot[0]));
							gotIP = true;
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
					} else {
						
						try {
	
							Constants.LOGGER.info("Connecting to " + boot[0] + ":" + boot[1] + " to know my external IP ...");
							Socket s = new Socket();
							Constants.LOGGER.info("Connecting ...");
							s.connect(new InetSocketAddress(boot[0], Integer.parseInt(boot[1])), 5000);
							Constants.LOGGER.info(" done!");
	
							DataOutputStream out = new DataOutputStream(s.getOutputStream());
	
							BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
							out.writeBytes(NaFTManager.NaFT_GREETING + "\n\n");
							out.flush();
	
							mLocalHost = PGridHost.getHost(s.getLocalAddress(), Integer.parseInt(boot[1]), true);
				
							mLocalHost.setExternalIP(InetAddress.getByName(in.readLine()));		
							out.close();
							in.close();
							s.close();
							gotIP = true;
						} catch (ConnectException e){
							Constants.LOGGER.warning("Bootstrap host " +bootstrapHosts[i] + " is not reachable.");
						} catch (UnknownHostException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
						
					}
	
					if (gotIP) {
						
						Constants.LOGGER.info("mLocalHost = " + mLocalHost.toHostString() + " (External IP: " + mLocalHost.getExternalIP().getHostAddress() +")");
						
						if (!mLocalHost.getIP().equals(mLocalHost.getExternalIP())){
							// Internal IP is different from external IP -> host is behind a
							// firewall or a NAT
							String internal = "N/A";
							String external = "N/A";
							if (mLocalHost.getIP() != null) internal = mLocalHost.getIP().getHostAddress();
							if (mLocalHost.getExternalIP() != null) external = mLocalHost.getExternalIP().getHostAddress();
							NaFTManager.LOGGER.info("This peer is behing a NAT/FW.\n" +
									"Internal IP = " + internal + "\n" +
									"External IP = " + external + "\n");
							
							mLocalHost.setFirewalledStatus(true);
							mPGridP2P.setProperty(Properties.BEHIND_FIREWALL, ""+true);
							
							// FIXME: commented out for testing purpose. Please uncomment it before production
							//setSuperPeerFlag(false);
							
							ConnectionManager.sharedInstance().setFirewallStatus(true);
						}	
					
						mNewIdentity = true;
						mLocalHost.setLocalHostFlag();
						
						
						String path  = mPGridP2P.propertyString(Properties.PEER_PATH);
						if (path.equalsIgnoreCase("%")) {
							mLocalHost.setPath("");
						} else {
							mLocalHost.setPath(path);
						}
						if (mPGridP2P.isSuperPeer())
							addFidget(mLocalHost);
						save();
						return;
					}
				} // end for 
				
				try {
					Thread.sleep(mWaitingTime);
				} catch (InterruptedException e) {}
			
			} // end for

			Constants.LOGGER.severe("Please ensure that a bootstrap host is available and accessible.");
			mPGridP2P.gracefulShutdown("No IP address could be found. Exiting P-Grid.");
			return;
			

		}
		
		
		// file exists => read routing table
		try {
			Constants.LOGGER.config("reading P-Grid Routing Table from '" + routeFile + "' ...");
			BufferedReader in = new BufferedReader(new FileReader(mFile));
			char[] content = new char[(int) mFile.length()];
			in.read(content, 0, content.length);
			in.close();

			SAXParserFactory spf = SAXParserFactory.newInstance();
			XMLReader parser = spf.newSAXParser().getXMLReader();
			// XMLReader parser = XMLReaderFactory.createXMLReader();
			parser.setContentHandler(this);
			parser.parse(new InputSource(new StringReader(new String(content))));
			mLocalHost.setLocalHostFlag();

			
			// Try to find whether or not the local IP has changed
			InetAddress addr = mLocalHost.getIP();
			String name = null;
			if (addr != null)
				name = addr.getHostName();

			
			try {
				mLocalHost.setIP(InetAddress.getLocalHost());
				
			} catch (UnknownHostException e) {
				Constants.LOGGER.log(Level.SEVERE, null, e);
				mPGridP2P.gracefulShutdown(e.getMessage());
				return;
			}
			

			if (port != mLocalHost.getPort() || addr == null || !addr.equals(mLocalHost.getExternalIP()))
				mModifiedIP = true;

			mLocalHost.setPort(port);
			mLocalHost.setExternalIP(getLocalHost().getIP());
			
			// make sure that if we are not a super peer, we will not have
			// routing information.
			if (!mPGridP2P.isSuperPeer()) {
				getLocalHost().setPath("");
				acquireWriteLock();
				try {
					mLevels.clear();
					mReplicas.clear();
				} finally {
					releaseWriteLock();
				}
			}

			save();
		} catch (SAXException e) {
			Constants.LOGGER.log(Level.SEVERE, null, e);
			mPGridP2P.gracefulShutdown(e.getMessage());
			return;
		} catch (FileNotFoundException e) {
			Constants.LOGGER.log(Level.SEVERE, null, e);
			mPGridP2P.gracefulShutdown(e.getMessage());
			return;
		} catch (IOException e) {
			Constants.LOGGER.log(Level.SEVERE, null, e);
			mPGridP2P.gracefulShutdown(e.getMessage());
			return;
		} catch (ParserConfigurationException e) {
			e.printStackTrace(); // To change body of catch statement use
			// File | Settings | File Templates.
		}
	}
	
	private void createHierachy() {
		if (!mFile.getParentFile().exists() && !mFile.getParentFile().mkdirs()) {
			Constants.LOGGER.config("Cannot create subfolder: " + mFile.getParentFile());
			mPGridP2P.gracefulShutdown("Cannot create subfolder: " + mFile.getParentFile());
			return;
		}
	}

	/**
	 * Saves the routing table to the defined file.
	 */
	public void save() {
		/*
		if (mShutdownFlag){
			System.out.println("Saving of routing table aborted because of shutdown.");
			return;
		}
		*/
			
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(mFile));
			String content = super.toXMLString("", Constants.LINE_SEPERATOR);
			out.write(content);
			out.close();
		} catch (FileNotFoundException e) {
			Constants.LOGGER.log(Level.WARNING, null, e);
		} catch (IOException e) {
			Constants.LOGGER.log(Level.WARNING, null, e);
		}
	}

	/**
	 * Adds a new host at the delivered level.
	 * 
	 * @param level
	 *            the level of the path.
	 * @param host
	 *            the host.
	 */
	public void addLevel(int level, PGridHost host) {
		if (!host.equals(mLocalHost))
			super.addLevel(level, host);
	}

	/**
	 * Adds new hosts at the the delivered level.
	 * 
	 * @param level
	 *            the level of the path.
	 * @param hosts
	 *            the hosts.
	 */
	public void addLevel(int level, Collection hosts) {
		hosts.remove(mLocalHost);
		super.addLevel(level, hosts);
	}

	/**
	 * Adds the delivered host to the replicas.
	 * 
	 * @param host
	 *            the new host.
	 */
	public void addReplica(PGridHost host) {
		if (!host.equals(mLocalHost))
			super.addReplica(host);

	}

	/**
	 * Adds the delivered hosts to the replicas.
	 * 
	 * @param hosts
	 *            the new hosts.
	 */
	public void addReplicas(Collection hosts) {
		hosts.remove(mLocalHost);
		super.addReplicas(hosts);
	}

	/**
	 * Sets the whole level with the new delivered hosts.
	 * 
	 * @param level
	 *            the level to set.
	 * @param hosts
	 *            the new hosts.
	 * @throws IllegalArgumentException
	 *             if an illegal level is given.
	 */
	public void setLevel(int level, Collection hosts) throws IllegalArgumentException {
		hosts.remove(mLocalHost);
		super.setLevel(level, hosts);
	}

	/**
	 * Sets the whole level with the new delivered hosts.
	 * 
	 * @param level
	 *            the level to set.
	 * @param hosts
	 *            the new hosts.
	 */
	public void setLevel(int level, PGridHost[] hosts) {
		try {
			acquireWriteLock();
			setLevels(level);
			if (level >= mLevels.size()) {
				Constants.LOGGER.log(Level.WARNING, "Illegal Argument in LocalRoutingTable.setLevels() for level "
						+ level, new Throwable());
			} else {
				((Collection) mLevels.get(level)).clear();
				for (int i = 0; i < hosts.length; i++)
					addLevel(level, hosts[i]);
			}
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Sets the replicas with the given hosts.
	 * 
	 * @param hosts
	 *            the new hosts.
	 */
	public void setReplicas(Collection hosts) {
		hosts.remove(mLocalHost);
		super.setReplicas(hosts);
	}

	/**
	 * Sets the replicas with the given hosts.
	 * 
	 * @param hosts
	 *            the new hosts.
	 */
	public void setReplicas(PGridHost[] hosts) {
		try {
			acquireWriteLock();
			mReplicas.clear();
			for (int i = 0; i < hosts.length; i++)
				addReplica(hosts[i]);

		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Performs a union of the delivered and this Routing Table.
	 * 
	 * @param routingTable
	 *            a Routing Table.
	 */
	public void union(RoutingTable routingTable) {
		unionFidgets(routingTable);
	}

	/**
	 * Performs a union of the hosts at the delivered level of the delivered and
	 * this Routing Table.
	 * 
	 * @param level
	 *            the level.
	 * @param routingTable
	 *            a Routing Table.
	 */
	public void unionLevel(int level, RoutingTable routingTable) {
		if (routingTable != null) {
			setLevel(level, union(getLevelVector(level), routingTable.getLevelVector(level)));
		}
	}

	/**
	 * If P-Grid is shutdown the routing table is saved to a file.
	 */
	public synchronized void shutdown() {
		save();
		mShutdownFlag = true;
	}

	/**
	 * True iff the local peer has not inserted its information (id, ip, E, ts,
	 * D(id, ip, E, ts)) into P-Grid.
	 * 
	 * @return true if local peer is unknown for P-Grid
	 */
	public boolean isNewIdentity() {
		return mNewIdentity;
	}

	/**
	 * True iff the new IP or port has changed since the last time this peer was
	 * run.
	 * 
	 * @return if this peer has a new IP or port
	 */
	public boolean isModifiedIp() {
		return mModifiedIP;
	}

	/**
	 * Return a reference to the host with a given guid
	 * 
	 * @param guid
	 *            the guid
	 * @return the host
	 */
	public PGridHost getHost(GUID guid) {
		return (PGridHost) mHosts.get(guid.toString());
	}

	public Object clone() throws CloneNotSupportedException {
		Object clone;
		acquireReadLock();
		try {
			clone = super.clone();
		} finally {
			releaseReadLock();
		}
		return clone;
	}

	/**
	 * Returns this peer proxy if any.
	 * 
	 * @return this peer proxy if any.
	 */
	public PGridHost getProxy() {
		if (mProxy == null || !getFidgetVector().contains(mProxy)) {
			Collection hosts = getFidgetVector();
			if (hosts.isEmpty())
				return null;
			List list = new LinkedList(hosts);
			Collections.shuffle(list);
			mProxy = (PGridHost) list.get(0);
		}

		return mProxy;
	}
}
