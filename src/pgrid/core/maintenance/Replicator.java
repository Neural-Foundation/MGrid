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
import pgrid.core.index.DBView;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.MessageManager;
import pgrid.network.router.MessageWaiter;
import pgrid.network.protocol.ReplicateMessage;
import pgrid.network.protocol.PGridMessage;
import pgrid.network.protocol.ACKMessage;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import p2p.basic.GUID;

/**
 * This class replicates initially the local data items among some peers of the fidget list.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
class Replicator implements MessageWaiter {

	/**
	 * The average. exchange delay.
	 */
	private static final int AVG_EXCHANGE_DELAY = 1000 * 45; // 1 min.

	/**
	 * The minimum exchange delay.
	 */
	private static final int MIN_EXCHANGE_DELAY = 1000 * 10; // 10 sec.

	/**
	 * The Message Manager.
	 */
	private MessageManager mMsgMgr = MessageManager.sharedInstance();

	/**
	 * The next time for a replication.
	 */
	private long mNextReplicateTime = 0;

	/**
	 * The PGridP2P facility.
	 */
	private PGridP2P mPGridP2P = null;

	/**
	 * The random number generator.
	 */
	private SecureRandom mRandomizer = new SecureRandom();

	/**
	 * Amount of replication requests.
	 */
	private Vector mUsedHosts = new Vector();

	private boolean isCondition = true;

	/**
	 * Creates a new router.
	 *
	 * @param p2p the P2P facility.
	 */
	Replicator(PGridP2P p2p) {
		mPGridP2P = p2p;
	}

	boolean isCondition() {
		return isCondition;
	}

	/**
	 * Replicates the local data store at a random peer of the fidget list.
	 */
	public void replicate() throws Exception {
		// return if it's too early for the next exchange
		if (System.currentTimeMillis() < mNextReplicateTime)
			return;

		synchronized(mPGridP2P.getRoutingTable().getFidgetVector()) {
			List list = new Vector();
			list.addAll(mPGridP2P.getRoutingTable().getFidgetVector());
			Collections.shuffle(list);
			for (Iterator it = list.iterator(); it.hasNext();) {
				PGridHost host = (PGridHost)it.next();
				if (mPGridP2P.isLocalHost(host))
					continue;
				// if host was already used => choose another host
				if (mUsedHosts.contains(host))
					continue;
				if (sendReplicateMsg(host)) {
					mUsedHosts.add(host);
					setNextReplicateTime();
					break;
				}
			}
		}
	}

	/**
	 * Invoked when a new replicate request was received.
	 *
	 * @param host      the requesting host.
	 * @param dataItems the data items to replicate.
	 */
	public void replicateRequest(PGridHost host, Collection dataItems) {
		Constants.LOGGER.finer("Replicate data from host " + host.toString() + " as requested (" + dataItems.size() + " items)");
		mPGridP2P.getIndexManager().getIndexTable().addAll(dataItems);

		// TODO reply with an ACK message
		if (PGridP2P.sharedInstance().isInTestMode()) {
//			mPGridP2P.getIndexManager().writeIndexTable();//expensive statistic
			mPGridP2P.getStatistics().DataItemsManaged = mPGridP2P.getIndexManager().getIndexTable().count();
			mPGridP2P.getStatistics().DataItemsPath = DBView.selection(mPGridP2P.getIndexManager().getIndexTable(), mPGridP2P.getLocalHost().getPath()).count();
		}
	}

	/**
	 * Sends the fidget exchange message to the given host.
	 * @param host the host.
	 * @return <tt>true</tt> if sending was successful, <tt>false</tt> otherwise.
	 */
	private boolean sendReplicateMsg(PGridHost host) {
		Collection dataItems = mPGridP2P.getIndexManager().getIndexTable().getOwnedIndexEntries();
		if (dataItems.size() > 0) {
			ReplicateMessage msg = new ReplicateMessage(dataItems);
			if (mMsgMgr.sendMessage(host, msg, this)) {
				// message sent successfully => wait for reply
				Constants.LOGGER.finer("Request Host " + host.toString() + " to replicate data (" + dataItems.size() + " items) ... wait for reply.");
				// TODO wait for ACK message
				return true;
			} else {
				Constants.LOGGER.finer("Failed to replicate data at host " + host.toString() + ".");
			}
			return false;
		}
		return true;
	}

	/**
	 * Sets the next time for a replication.
	 */
	private void setNextReplicateTime() {
		long currTime = System.currentTimeMillis();
		// check in which phase we are
		long time = AVG_EXCHANGE_DELAY;
		long timeLeft = mPGridP2P.getMaintenanceManager().getConstructionStartTime() - currTime;
		int replicasMissing = Constants.REPLICATION_FACTOR - mUsedHosts.size();
		if (replicasMissing <= 0) {
			time = timeLeft;
		} else if ((replicasMissing * AVG_EXCHANGE_DELAY) > timeLeft) {
			time = MIN_EXCHANGE_DELAY;
		}
		mNextReplicateTime = currTime + time + mRandomizer.nextInt(1000 * 10);
	}

	void setReady(boolean cond) {
		isCondition = cond;
	}

	/**
	 * Handle dirct messages
	 * @param msg
	 * @param guid
	 */
	public void newMessage(PGridMessage msg, GUID guid) {
		if (msg instanceof ACKMessage) {
			if (((ACKMessage)msg).getCode() == ACKMessage.CODE_NOT_SUPERPEER)
				mPGridP2P.getMaintenanceManager().removeRemotePeer(msg.getHeader().getHost());
		}
	}
}