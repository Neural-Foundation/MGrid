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
import pgrid.GUID;
import pgrid.core.index.Signature;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.MessageManager;
import pgrid.network.protocol.ExchangeInvitationMessage;

import java.util.*;
import java.security.SecureRandom;

/**
 * This class frequently initiates exchanges with a host selected randomly from the fidget list.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
class ExchangeInitiator {

	/**
	 * The average. exchange delay.
	 */
	private static final int AVG_EXCHANGE_DELAY = 1000 * 60 *  1; // 1 min.

	/**
	 * The maximum of failed trys to contact a fidget peer before trying to contact a bootstrap peer.
	 */
	private static final int MAX_FAILED_INVITATIONS = 10;

	/**
	 * The minimum exchange delay.
	 */
	private static final int MIN_EXCHANGE_DELAY = 1000 * 10; // 1 min.

	/**
	 * To count the failed trys to contact fidget hosts.
	 */
	private int mFailedInvitations = 0;

	/**
	 * The Message Manager.
	 */
	private MessageManager mMsgMgr = MessageManager.sharedInstance();

	/**
	 * The next time for an exchange.
	 */
	private long mNextExchangeTime = 0;

	/**
	 * The PGridP2P facility.
	 */
	private PGridP2P mPGridP2P = null;

	/**
	 * The random number generator.
	 */
	private SecureRandom mRandomizer = new SecureRandom();

	private boolean isCondition = true;

	/**
	 * Creates a new router.
	 *
	 * @param p2p the P2P facility.
	 */
	ExchangeInitiator(PGridP2P p2p) {
		mPGridP2P = p2p;
	}

	boolean isCondition() {
		return isCondition;
	}

	/**
	 * Invites one of the delivered hosts for an exchange.
	 *
	 * @param hosts     the hosts.
	 * @param recursion the recursion value.
	 * @param lCurrent  the current common length.
	 * @return <tt>true</tt> if a host was invited, <tt>false</tt> otherwise.
	 */
	public boolean randomExchange(Collection hosts, int recursion, int lCurrent) {
		if (hosts == null)
			return false;
		List list = new ArrayList();
		list.addAll(hosts);
		Collections.shuffle(list);
		for (Iterator it = list.iterator(); it.hasNext();) {
			PGridHost host = (PGridHost)it.next();
			if (mPGridP2P.isLocalHost(host))
				continue;
			if (!host.isExchangeTime())
				continue;

			// send invite exchange message
			Signature sign = mPGridP2P.getIndexManager().getPeerIndexTableSignature(host);
			if (sign==null) sign = new Signature();

			ExchangeInvitationMessage msg = new ExchangeInvitationMessage(mPGridP2P.getLocalPath(), sign
					, recursion, lCurrent);
			if (mMsgMgr.sendMessage(host, msg, mPGridP2P.getMaintenanceManager())) {
				Constants.LOGGER.finer("Invited Host " + host.toString() + " for an Exchange.");
				mPGridP2P.getStatistics().ExchangesInitiated++;
				host.invited();
				mFailedInvitations = 0;
				return true;
			} else {
				Constants.LOGGER.finer("Invitation of Host " + host.toString() + " for an Exchange failed.");
				mFailedInvitations++;
			}
			/*try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}*/
		}
		return false;
	}

	/**
	 * Invites a random peer from the fidget list if it is time for an exchange.
	 */
	void inviteHost(int minStorage) throws Exception {
		// return if it's too early for the next exchange
		if (System.currentTimeMillis() < mNextExchangeTime)
			return;

		if (!randomExchange(mPGridP2P.getRoutingTable().getFidgetVector(), 0, 0)) {
			// if no exchange invite message could be sent => bootstrap
			if (mFailedInvitations > MAX_FAILED_INVITATIONS) {
				mFailedInvitations = 0;
				Constants.LOGGER.finer("if no exchange invite message could be sent => bootstrap");
				mPGridP2P.getMaintenanceManager().bootstrap();
			}
		}
		setNextExchangeTime(minStorage);
	}

	/**
	 * Sets the next time for an exchange.
	 */
	private void setNextExchangeTime(int minStorage) {
		long currTime = System.currentTimeMillis();
		long time = AVG_EXCHANGE_DELAY;
		// construction phase
		// as longer the path as longer to sleep
		if (mPGridP2P.getLocalPath().length() > 0)
			time *= mPGridP2P.getLocalPath().length();
		// if i'm overloaded, reduce the sleep accordingly
		if (mPGridP2P == null || mPGridP2P.getIndexManager() == null || mPGridP2P.getIndexManager().getIndexTable() == null) return;
		int storage = mPGridP2P.getIndexManager().getIndexTable().count();
		if (storage > (2 * minStorage))
			time *= (minStorage / storage);
		// failed exchanges
		// time += mRandomizer.nextInt(MIN_EXCHANGE_DELAY) * mFailedExchanges;
		if (time < MIN_EXCHANGE_DELAY)
			time = MIN_EXCHANGE_DELAY;
		mNextExchangeTime = currTime + time + mRandomizer.nextInt(1000 * 10);
	}

	void setReady(boolean cond) {
		isCondition = cond;
	}

}