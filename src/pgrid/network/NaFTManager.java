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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import pgrid.Constants;
import pgrid.PGridHost;
import pgrid.Properties;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.protocol.ACKMessage;
import pgrid.network.protocol.NaFTConnectionRegisterMessage;
import pgrid.network.protocol.NaFTConnectionRegisterReplyMessage;
import pgrid.network.protocol.NaFTConnectionReversalInitMessage;
import pgrid.network.protocol.NaFTConnectionReversalInitReplyMessage;
import pgrid.util.logging.FlushedStreamHandler;
import pgrid.util.logging.LogFormatter;

public class NaFTManager  {

	/**
	 * if true, then a relay is chosen between all super peers. The relay of a host is advertized to the other peers
	 * using the DHT
	 * if false, then the bootstrap host is used as relay
	 */
	public static final boolean DISTRIBUTED_NaFT_RELAY = false;
	
	/**
	 * Current relay host
	 */
	private PGridHost mRelay = null;
	
	/*
	 * List of potential relays
	 */
	private ArrayList<PGridHost> mAvailableRelays = new ArrayList<PGridHost>();
	
	/**
	 * Bootstrap host
	 */
	private PGridHost mBootstrap = null;
	/**
	 * The PGridP2P facility.
	 */
	private PGridP2P mPGridP2P = null;
		
	private final Timer timer = new Timer("NaFTManager Relay connection checker", true);
	
	/**
	 * The PGrid.NaFT logger.
	 */
	public static final Logger LOGGER = Logger.getLogger("PGrid.NaFTManager");
	
	/**
	 * NaFT initialisation 
	 */
	public static final String NaFT_GREETING = "NaFT";
	
	static {
		String logFile = "NaFT.log";
		String LOG_DIR = Constants.LOG_DIR;
		if (LOG_DIR.length() > 0)
			new File(LOG_DIR).mkdirs();
		try {
			FileHandler fHandler = new FileHandler(LOG_DIR + logFile);
			LogFormatter formatter = new LogFormatter();
			formatter.setDateFormat("HH:mm:ss");
			formatter.setFormatPattern(LogFormatter.DEBUG_PATTERN);

			fHandler.setFormatter(formatter);
			LOGGER.addHandler(fHandler);
			StreamHandler eHandler = new FlushedStreamHandler(System.err, formatter);
			eHandler.setLevel(Level.WARNING);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Could not use logging file '" + LOG_DIR + logFile + "'!", e);
		} catch (SecurityException e) {
			LOGGER.log(Level.WARNING, "Could not use logging file '" + LOG_DIR + logFile + "'!", e);
		}
	}
	
	
	/**
	 * Creates a new NAT and Firewall Traversal manager
	 *
	 * @param p2p the P2P facility.
	 */
	public NaFTManager(PGridP2P p2p) {
		mPGridP2P = p2p;
		
		String bootstrapHosts[] = mPGridP2P.propertyString(Properties.BOOTSTRAP_HOSTS).split(";");
		String boot[] = bootstrapHosts[0].split(":");

		
		try {
			if (boot.length == 2)
				mBootstrap = PGridHost.getHost(InetAddress.getByName(boot[0]), Integer.parseInt(boot[1]));
			else if (boot.length == 1)
				mBootstrap = PGridHost.getHost(InetAddress.getByName(boot[0]), 1805);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e){
			e.printStackTrace();
			LOGGER.severe("Bootstrap host property has wonrg format. It should be host1:port1;host2:port2");
		}
	}
	
	private boolean isRelayActive(){
		if (mRelay != null && mRelay.getGUID() != null){
    		Connection conn = ConnectionManager.sharedInstance().getConnection(mRelay);
    		if (conn != null && conn.isConnected()){
    			return true;
    		}
		}
		return false;
	}
	
	public void init(){
		if (mPGridP2P.getLocalHost().isBehindFirewall()){
			
			timer.schedule(new TimerTask() {
	            public void run() {
	            	
	            	if (isRelayActive()) return;
	            	
	            	mRelay = null;
	    			registerWithRelay();
	            }
	        }, 0, 10 * 1000);
			
		}
	}
	
	public boolean isRelay(){
		return (mPGridP2P.getLocalHost().getIP().getHostAddress().equals(mRelay.getIP().getHostAddress()) &&
				mPGridP2P.getLocalHost().getPort() == mRelay.getPort());
	}
	
	
	/*
	public PGridHost getRelay(){
		return mRelay;
	}
	*/
	public void setRelay(PGridHost relay){
		mRelay = relay;
	}
	
	
	public boolean isMySelf(PGridHost relay){
		return (mPGridP2P.getLocalHost().getIP().getHostAddress().equals(relay.getIP().getHostAddress()) &&
				mPGridP2P.getLocalHost().getPort() == relay.getPort());
	}
	
	public PGridHost getRelay(PGridHost host){
		if (DISTRIBUTED_NaFT_RELAY) {
			// FIXME add corresponding code
			// perform query for the host, or get cached relay
			return null;
		} else {
			return mBootstrap;
		}
	}
	
	/**
	 * Register to an external relay if the host is behind a nat
	 */
	private void registerWithRelay(){
		if (mPGridP2P.getLocalHost().isBehindFirewall()){
			
			
			if (mRelay == null){
				if (mPGridP2P.getLocalHost().getExternalIP() == null){
					LOGGER.info("Could not determine the external IP. Trying again later.");
					return;
				}
				
				NaFTConnectionRegisterMessage msg = new NaFTConnectionRegisterMessage();
				PGridHost relay;
				if (mAvailableRelays.isEmpty()){
					// Contact the bootstrap host to have a list of potential relays
					relay = mBootstrap;
				} else {
					// Contact one of the potential relay
					Collections.shuffle(mAvailableRelays);
					relay = mAvailableRelays.get(0);
				}
				
				LOGGER.info("Host is behind a NAT. Registering to relay: " + relay.toHostString());
				mPGridP2P.send(relay, msg);
				
			} else {
				LOGGER.severe("Relay is not null. This should never happen.");
			}
		}
	}
	
	public void newNaFTConnectionRegisterMessage(NaFTConnectionRegisterMessage message){
		
		LOGGER.fine("NaFTConnectionRegisterMessage received.\n" 
				+ "Internal IP: " + message.getHost().getExternalIP().getHostAddress() + "\n"
				+ "Internal Port: " + message.getHost().getPort() + "\n"
				+ "External IP: " + message.getHeader().getHost().getIP().getHostAddress() + "\n"
		);
		
		NaFTConnectionRegisterReplyMessage reply = new NaFTConnectionRegisterReplyMessage();
		ArrayList<PGridHost> relays = new ArrayList<PGridHost>();
		
		if (DISTRIBUTED_NaFT_RELAY){
			PGridHost[][] hosts = mPGridP2P.getRoutingTable().getLevels();
			for (int i=0; i<hosts.length; i++ ){
				for (int j=0; j<hosts[i].length; j++){
					relays.add((PGridHost)hosts[i][j]);
				}
			}
		} else {
			relays.add(mPGridP2P.getLocalHost());
		}
		reply.setRelayHosts(relays);
		mPGridP2P.send(message.getHeader().getHost(),reply);
		
	}
	
	public void newNaFTConnectionRegisterReplyMessage(NaFTConnectionRegisterReplyMessage message){
		LOGGER.fine("NaFTConnectionRegisterReplyMessage received from " + message.getHeader().getHost()); 
		
			
		if (!mAvailableRelays.isEmpty()) {
			// Got this message from a relay (and not from bootstrap)
			mRelay = message.getHeader().getHost();
			LOGGER.fine("Using " + mRelay + " as relay.");
		}
		
		for (PGridHost host : message.getRelayHosts()){
			if (!mAvailableRelays.contains(host)) mAvailableRelays.add(host);
		}
		
	}
	
	public void newNaFTConnectionReversalInitMessage(NaFTConnectionReversalInitMessage message){
		
		LOGGER.fine("NaFTConnectionReversalInitMessage received from " 
				+ message.getHeader().getHost() + " with a destination host: " 
				+ message.getDestinationHost());
		
		if (!mPGridP2P.isSuperPeer()) {
			mPGridP2P.send(message.getHeader().getHost(), new ACKMessage(message.getGUID(), ACKMessage.CODE_NOT_SUPERPEER));
		} else if (mPGridP2P.getLocalHost().isBehindFirewall()){
			mPGridP2P.send(message.getHeader().getHost(), new ACKMessage(message.getGUID(), ACKMessage.CODE_BEHIND_FIREWALL));
		} else {
			
			
			// The current peer is the destination host
			if (message.getDestinationHost().getIP().getHostAddress().equals(mPGridP2P.getLocalPeer().getIP().getHostAddress())
					&& message.getDestinationHost().getPort() == mPGridP2P.getLocalPeer().getPort()){
				
				// open a connection with the requesting host.
				NaFTConnectionReversalInitReplyMessage reply = new NaFTConnectionReversalInitReplyMessage();
				reply.getHeader().addReference(message.getGUID());
				LOGGER.fine("MaintenanceManager.newNaFTConnectionReversalInitMessage(): Destination reached ! Opening a reverse connection ...");
				mPGridP2P.send(message.getHost(), reply);
				
			} 
			// Else forward message to destination host
			else {
				LOGGER.fine("MaintenanceManager.newNaFTConnectionReversalInitMessage(): forwading NaFT connection reversal request");
				mPGridP2P.send(message.getDestinationHost(), message);
			}			
		}
	}
}