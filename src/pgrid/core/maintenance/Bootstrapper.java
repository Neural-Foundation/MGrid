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

package pgrid.core.maintenance;

import pgrid.Constants;
import pgrid.PGridHost;
import pgrid.Properties;
import pgrid.core.XMLRoutingTable;
import pgrid.core.RoutingTable;
import pgrid.core.LocalRoutingTable;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.MessageManager;
import pgrid.network.protocol.BootstrapMessage;
import pgrid.network.protocol.BootstrapReplyMessage;
import pgrid.network.protocol.FidgetExchangeMessage;
import pgrid.network.protocol.FidgetExchangeReplyMessage;
import pgrid.util.Tokenizer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.*;

/**
 * This class bootstraps with one of the know bootstrap hosts or a designated bootstrap host.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
class Bootstrapper {

	/**
	 * The average. exchange delay.
	 */
	protected static final int AVG_EXCHANGE_DELAY = 1000 * 15; // 1 min.

	/**
	 * The maximum exchange delay.
	 */
	protected static final int MAX_EXCHANGE_DELAY = 1000 * 45; // 10 min.

	/**
	 * The maximum number of fidget exchanges allowed to wait in queue.
	 */
	protected static final int MAX_FIDGET_IN_QUEUE = 10;

	/**
	 * The minimum exchange delay.
	 */
	protected static final int MIN_EXCHANGE_DELAY = 1000; // 10 sec.

	/**
	 * Timeout for a fidget exchange
	 */
	static final long FIDGET_EXCHANGE_TIMEOUT = 1000*45;

	/**
	 * Queue containing fidget exchange initiative
	 */
	protected Vector mFidgetQueue = new Vector(10);

	/**
	 * Lock object used when doing an fidget exchange
	 */
	protected Object mFidgetExchangeLock = new Object();

	/**
	 * True if a fidget exchange is currently underway
	 */
	protected boolean mFidgetExchange = false;

	/**
	 * True if this peer has bootstrapped
	 */
	protected boolean mHasBootstrapped = false;

	/**
	 * The addBootstrapHost hosts.
	 */
	protected Vector mHosts = new Vector();

	/**
	 * The new addBootstrapHost hosts.
	 */
	protected Vector mHostsNew = new Vector();

	/**
	 * True if the local host is a bootstrap host
	 */
	protected boolean mIsBootstrapHost = false;

		/**
	 * Last fidget exchange reqest received. Used to guess if a fidget exchange message is
	 * lost or take too long
	 */
	protected long mLastFidgetExchangeRequest = 0;

	/**
	 * The Maintenance Manager.
	 */
	protected MaintenanceManager mMaintMgr;

	/**
	 * Maximum number of fidget in the fidget list
	 */
	protected int mMaxFidget = 0;

	/**
	 * The Message Manager.
	 */
	protected MessageManager mMsgMgr = MessageManager.sharedInstance();

	/**
	 * The next time for an exchange.
	 */
	protected long mNextExchangeTime = 0;

	/**
	 * The PGridP2P facility.
	 */
	protected PGridP2P mPGridP2P = null;

	/**
	 * The random number generator.
	 */
	protected SecureRandom mRandomizer = new SecureRandom();

	/**
	 * The already used hosts.
	 */
	protected Vector mUsedHosts = new Vector();

	/**
	 * Creates a new router.
	 *
	 * @param p2p the P2P facility.
	 */
	Bootstrapper(PGridP2P p2p, MaintenanceManager maintManager) {
		mPGridP2P = p2p;
		mMaintMgr = maintManager;
		mMaxFidget = mPGridP2P.propertyInteger(Properties.MAX_FIDGETS);

		// create bootstrap hosts from the property file
		String[] hostStr = Tokenizer.tokenize(mPGridP2P.propertyString(pgrid.Properties.BOOTSTRAP_HOSTS), ";");
		for (int i = 0; i < hostStr.length; i++) {
			String[] parts = Tokenizer.tokenize(hostStr[i], ":");
			PGridHost host = null;
			int port = Constants.DEFAULT_PORT;
			if (parts.length > 1) {
				try {
					port = Integer.parseInt(parts[1]);
				} catch (NumberFormatException e) {
					port = Constants.DEFAULT_PORT;
				}
			}
			// get Internet address
			try {
				InetAddress addr;

				// if bootstrap host is "localhost" try to get InetAddress corresponding to the local host
				// and not the loopback address 127.0.0.1
				if (parts[0].equals("localhost")) {
					addr = InetAddress.getLocalHost();
				} else {
					addr = InetAddress.getByName(parts[0]);
				}
				host = PGridHost.getHost(addr, port, false);
			} catch (UnknownHostException e) {
				continue;
			}
			// check if host is the local host
			if (mPGridP2P.isLocalHost(host)) {
				mIsBootstrapHost = true;
				Constants.LOGGER.finer("Localhost host " + host.toHostString() + " is a bootstrap host.");
				continue;
			}
			// add the host if it's a valid host
			if ((host.getAddressString() != null) && (host.getPort() > 0)) {
				Constants.LOGGER.finer("add host " + host.toHostString() + " as bootstrap host.");
				mHosts.add(host);
				// addBootstrapHost(host); INFO this caused to kill the program because hosts are not in the bootstrap list before bootstrap() is called 
			}
		}

		// if the localhost is a bootstrap host and no other bootstrap host are known,
		// consider that this host has already bootstrapped
		if (mIsBootstrapHost && mHosts.isEmpty())
			mHasBootstrapped = true;
	}

	/**
	 * Adds the given host to the list of addBootstrapHost hosts.
	 *
	 * @param host the new addBootstrapHost host.
	 */
	void addBootstrapHost(PGridHost host) {
		if (mPGridP2P.isLocalHost(host) && mPGridP2P.isSuperPeer()) {
			mIsBootstrapHost = true;
			Constants.LOGGER.finer("Localhost host " + host.toHostString() + " is a bootstrap host.");
		} else if (mPGridP2P.isLocalHost(host)){
			if (mPGridP2P.isLocalHost(host)) {
				Constants.LOGGER.warning("Localhost host " + host.toHostString() + " is a bootstrap host. This host should be a super peer!");
			} else {
				Constants.LOGGER.finer("Add host " + host.toHostString() + " as a bootstrap host.");
				mHostsNew.add(host);
			}
		} else {
			Constants.LOGGER.finer("Add host " + host.toHostString() + " as a bootstrap host.");
			mHostsNew.add(host);
		}
	}

	/**
	 * Tries to bootstrap with the known bootstrap hosts.
	 * @throws Exception any exception.
	 */
	void bootstrap() throws Exception {
		Constants.LOGGER.finest("Try to join the network.");
		boolean sent = false;

		// try to bootstrapp with new hosts
		synchronized(mHostsNew) {
			List list = new Vector();
			list.addAll(mHostsNew);
			Collections.shuffle(list);
			for (Iterator it = list.iterator(); it.hasNext() && !sent;) {
				PGridHost host = (PGridHost)it.next();
				mHosts.add(host);
				sent = sendBootstrapMsg(host);
			}
			// remove all hosts which are already in the addBootstrapHost list
			mHostsNew.removeAll(mHosts);

			// if a bootstrap message was successfully sent, return
			if (sent) return;
		}

		// try to addBootstrapHost using the addBootstrapHost list

		synchronized(mHosts) {
			List list = new Vector();
			list.addAll(mHosts);
			Collections.shuffle(list);
			for (Iterator it = list.iterator(); it.hasNext();) {
				if (sendBootstrapMsg((PGridHost)it.next())) {
					sent = true;
					break;
				}
			}
			// if we are the only bootstrap host, loop back bootstrap
			if (mIsBootstrapHost && !sent) {
				sendBootstrapMsg(mPGridP2P.getLocalHost());	
			}
		}

	}

	/**
	 * Exchanges the fidget list with one of the hosts of the fidget list.
	 */
	public void fidgetExchange() throws Exception {
		// return if it's too early for the next exchange
		long time = System.currentTimeMillis();

		// check time out.
		synchronized(mFidgetExchangeLock) {
			if (mFidgetExchange && time-mLastFidgetExchangeRequest > FIDGET_EXCHANGE_TIMEOUT) {
				Constants.LOGGER.finer("Fidget reply timeout. Reset lock.");
				mFidgetExchange = false;
			}
			if (mFidgetExchange) {
				return;
			}

			// if some fidget messages are waiting, try to answer them
			if (!mFidgetQueue.isEmpty()) {
				processFidgetQueue();
			}

			// check if it is time for a new fidget exchange.
			if (System.currentTimeMillis() < mNextExchangeTime) {
				return;
			}

			synchronized(mPGridP2P.getRoutingTable().getFidgetVector()) {
				List list = new Vector();
				list.addAll(mPGridP2P.getRoutingTable().getFidgetVector());
				list.remove(mPGridP2P.getLocalHost());
				Collections.shuffle(list);

				// if all host have been asked once, restart.
				if (mUsedHosts.containsAll(list)) mUsedHosts.clear();

				for (Iterator it = list.iterator(); it.hasNext();) {
					PGridHost host = (PGridHost)it.next();
					if (host.equals(mPGridP2P.getLocalHost()))
						continue;
					// if host was already used for an exchange => try next one
					if (mUsedHosts.contains(host))
						continue;
					if (sendFidgetMsg(host)) {
						mFidgetExchange = true;
						mLastFidgetExchangeRequest = time;
						mUsedHosts.add(host);
						setNextExchangeTime();
						break;
					}
				}
			}
		}
	}

	/**
	 * Returns the used bootstrap hosts.
	 * @return the bootstrap hosts.
	 */
	public Collection getHosts() {
		return mHosts;
	}

	/**
	 * Sends the addBootstrapHost message to the given hosts.
	 * @param host the host used for bootstrapping.
	 * @return <tt>true</tt> if bootstrapping was successful, <tt>false</tt> otherwise.
	 */
	protected boolean sendBootstrapMsg(PGridHost host) {
		if (mMsgMgr.sendMessage(host, new BootstrapMessage(mPGridP2P.getLocalHost()), null)) {
			// message sent successfully => wait for reply
			Constants.LOGGER.finer("Bootstrapping with host " + host.toString() + " ... waiting for reply.");
			// break if no further bootstrapping is required
			return true;
		} else {
			Constants.LOGGER.finer("Bootstrapping with host " + host.toString() + " ... failed.");
		}
		return false;
	}

	/**
	 * Sends the fidget exchange message to the given host.
	 * @param host the host.
	 * @return <tt>true</tt> if sending was successful, <tt>false</tt> otherwise.
	 */
	protected boolean sendFidgetMsg(PGridHost host) {
		FidgetExchangeMessage msg = new FidgetExchangeMessage(mPGridP2P.getLocalHost(), mPGridP2P.getRoutingTable());

		if (mMsgMgr.sendMessage(host, msg, null)) {
			// message sent successfully => wait for reply
			Constants.LOGGER.finer("Fidget list exchange with host " + host.toString() + " ... waiting for reply.");

			return true;
		} else {
			Constants.LOGGER.finer("Fidget list exchange with host " + host.toString() + " ... failed.");
		}
		return false;
	}

	/**
	 * Processes a new addBootstrapHost request.
	 *
	 * @param bootstrap the addBootstrapHost request.
	 */
	public void newBootstrapRequest(BootstrapMessage bootstrap) {

		// if we are not a super peer, ignore request
		if (!mPGridP2P.isSuperPeer()) {
			Constants.LOGGER.warning("Received a bootstrap request. This host is not a super peer. Ignore.");
			return;
		}

		// respond with the local routing table, more precisely with the local fidget list
		BootstrapReplyMessage msg = null;

		Constants.LOGGER.finer("Recieved Bootstrap request from " + bootstrap.getHeader().getHost().toHostString() + ".");

		long currentTime = System.currentTimeMillis();
		long replicationDelay;
		long consructionDelay;
		replicationDelay = Math.max(0, mMaintMgr.getReplicationStartTime() - currentTime);
		consructionDelay = Math.max(0, mMaintMgr.getConstructionStartTime() - currentTime);

		try {
			msg = new BootstrapReplyMessage(mPGridP2P.getLocalHost(), (XMLRoutingTable)mPGridP2P.getRoutingTable().clone(), replicationDelay, consructionDelay);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}

		mMsgMgr.reply(bootstrap.getHeader().getHost(), msg, bootstrap, null, null);
	}

	/**
	 * Processes a new addBootstrapHost response.
	 *
	 * @param bootstrapReply the addBootstrapHost response.
	 */
	public void newBootstrapReply(BootstrapReplyMessage bootstrapReply) {
		mHasBootstrapped = true;
		LocalRoutingTable rt = mPGridP2P.getRoutingTable();
		rt.acquireWriteLock();
		try {
			if (mPGridP2P.isSuperPeer()) {
				// super peer
				if (bootstrapReply.getRoutingTable() != null) {
					// copy received fidget hosts to the list of fidget hosts
					// one of this hosts will be used by the Exchanger to initiate the first Exchange

					rt.unionFidgets(bootstrapReply.getRoutingTable(),mMaxFidget, mRandomizer.nextDouble(), true);
					if (!bootstrapReply.getRoutingTable().getFidgetVector().contains(mPGridP2P.getLocalHost())) {
						if (mPGridP2P.getRoutingTable().getFidgetVector().size() >= mMaxFidget) {
							PGridHost[] fidgets = rt.getFidgets();
							fidgets[mRandomizer.nextInt(mMaxFidget)] = mPGridP2P.getLocalHost();
							rt.setFidgets(fidgets);
						} else {
							rt.addFidget(mPGridP2P.getLocalHost());
						}
					}
					rt.save();

				}
			} else {
				// client
				if (bootstrapReply.getRoutingTable() != null) {
					// copy received fidget hosts to the list of fidget hosts
					// one of this hosts will be used by the Exchanger to initiate the first Exchange
					Collection col = bootstrapReply.getRoutingTable().getFidgetVector();
					col.remove(mPGridP2P.getLocalHost());

					rt.setFidgets(col);
					if (rt.getFidgetVector().size() >= mMaxFidget) {
						mHasBootstrapped = true;
					}

					rt.save();
				} else {
					// if we have no RoutingTable, it means either we have bootstrapped or the fidget exchange failed
					// in both case, retry ASAP
					mNextExchangeTime = System.currentTimeMillis();
				}
			}
		} finally {
			rt.releaseWriteLock();
		}



		// if we have no RoutingTable, it means either we have bootstrapped or the fidget exchange failed
		// in both case, retry ASAP
		mNextExchangeTime = System.currentTimeMillis();
	}


	/**
	 * Processes a new addBootstrapHost request.
	 *
	 * @param exchange the addBootstrapHost request.
	 */
	public void newFidgetExchangeRequest(FidgetExchangeMessage exchange) {

		//if we are not a super peer, ignore request
		if (!mPGridP2P.isSuperPeer()) {
			Constants.LOGGER.warning("Received a fidget exchange request. This host is not a super peer. Ignore.");
			return;
		}

		// respond with the local routing table, more precisely with the local fidget list
		FidgetExchangeReplyMessage msg = null;
		PGridHost host=null;
		long time = System.currentTimeMillis();

		// We want a random subset representative of P-Grid network.
		synchronized(mFidgetExchangeLock) {
			if (mFidgetExchange || !mHasBootstrapped) {
				// This peer is currently exchanging its fidget list. It cannot process this bootstrap
				if (mFidgetQueue.size() < MAX_FIDGET_IN_QUEUE) {
					exchange.setTimeStamp(time);
					mFidgetQueue.add(exchange);
					return;
				}
				host = exchange.getHeader().getHost();
				msg = new FidgetExchangeReplyMessage(exchange.getGUID(), mPGridP2P.getLocalHost(), null);
			}
			else {
				try {
					msg = new FidgetExchangeReplyMessage(exchange.getGUID(), mPGridP2P.getLocalHost(), (XMLRoutingTable)mPGridP2P.getRoutingTable().clone());
					host = exchange.getHeader().getHost();

					if (exchange.getRoutingTable() != null) {
						if (!mPGridP2P.isSuperPeer()) {
							exchange.getRoutingTable().remove(mPGridP2P.getLocalHost());
							Collection col = exchange.getRoutingTable().getFidgetVector();
							col.remove(mPGridP2P.getLocalHost());
							exchange.getRoutingTable().setFidgets(col);
						}
						
						// merge the fidget lists if available
						mPGridP2P.getRoutingTable().unionFidgets(exchange.getRoutingTable(),
								mPGridP2P.propertyInteger(Properties.MAX_FIDGETS), msg.getRandomNumber(), true);
						mPGridP2P.getRoutingTable().save();
					}
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
				}
			}

		}
		mMsgMgr.reply(host, msg, exchange, null, null);
	}

	/**
	 * Processes a new addBootstrapHost response.
	 *
	 * @param reply the addBootstrapHost response.
	 */
	public void newFidgetExchangeReply(FidgetExchangeReplyMessage reply) {
		synchronized(mFidgetExchangeLock) {
			// If we were not expecting a reply, ignore it
			if (mHasBootstrapped && !mFidgetExchange) {
				Constants.LOGGER.fine("Fidget reply from host " + reply.getHeader().getHost().toString() +
						": unexpected reply (timeout, probably).");
				return;
			}

			// reset fidget exchange lock
			mFidgetExchange = false;

			if (reply.getRoutingTable() != null) {
				// copy received fidget hosts to the list of fidget hosts
				// one of this hosts will be used by the Exchanger to initiate the first Exchange

				if (!mPGridP2P.isSuperPeer()) {
					reply.getRoutingTable().remove(mPGridP2P.getLocalHost());
					Collection col = reply.getRoutingTable().getFidgetVector();
					col.remove(mPGridP2P.getLocalHost());
					reply.getRoutingTable().setFidgets(col);
				}

				mPGridP2P.getRoutingTable().unionFidgets(reply.getRoutingTable(),
						mPGridP2P.propertyInteger(Properties.MAX_FIDGETS), reply.getRandomNumber(), false);
				mPGridP2P.getRoutingTable().save();

			} else {
				// if we have no RoutingTable, it means the fidget exchange failed
				// retry ASAP
				mNextExchangeTime = System.currentTimeMillis();
			}

			// if the waiting list is not empty, wake the maintenance manager
			if (!mFidgetQueue.isEmpty()) {
				processFidgetQueue();
			}
		}
	}

	/**
	 * Reply to all fidget exchange waiting in the queue
	 */
	protected void processFidgetQueue() {
		long time = System.currentTimeMillis();
		FidgetExchangeMessage msg;
		FidgetExchangeMessage[] request = null;

		synchronized(mFidgetExchangeLock) {
			request = (FidgetExchangeMessage[])mFidgetQueue.toArray(new FidgetExchangeMessage[mFidgetQueue.size()]);
			mFidgetQueue.clear();

			for (int i = 0; i < request.length; i++) {
				msg = request[i];

				if (time-msg.getTimeStamp() > FIDGET_EXCHANGE_TIMEOUT/2) continue;

				newFidgetExchangeRequest(msg);
			}
		}
	}

	/**
	 * Sets the next time for an exchange.
	 */
	protected void setNextExchangeTime() {
		long currTime = System.currentTimeMillis();
		// check in which phase we are
		long time;
		long timeLeft = mPGridP2P.getMaintenanceManager().getReplicationStartTime() - currTime;
		int fidgets = mPGridP2P.getRoutingTable().getFidgetVector().size();
		int fidgetsMissing = mPGridP2P.propertyInteger(Properties.MAX_FIDGETS) - fidgets;
		if (fidgetsMissing <= 0) {
			time = MAX_EXCHANGE_DELAY;
		} else {
			time = MIN_EXCHANGE_DELAY * fidgets;
			if (time > timeLeft)
				time = AVG_EXCHANGE_DELAY;
			if (time > timeLeft)
				time = MIN_EXCHANGE_DELAY;
		}
		mNextExchangeTime = currTime + time + mRandomizer.nextInt(1000 * 10);
	}

	/**
	 * Returns true iff the local host is a bootstrap host
	 * @return true if the localhost is a bootstrap host
	 */
	public boolean isBootstrapHost() {
		return mIsBootstrapHost;
	}

	/**
	 * Returns true if this peer has bootstrapped and is ready for a fidget exchange phase
	 * @return true if this peer has bootstrapped and is ready for a fidget exchange phase
	 */
	public boolean hasBootstrapped() {
		return mHasBootstrapped;
	}

	/**
	 * Set the has bootstrapped to false.
	 */
	public void resetBootstrapFlag() {
		mHasBootstrapped = false;
	}
}