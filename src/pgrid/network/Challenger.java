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

// import test.planetlab.MaintenanceTester;
import pgrid.PGridHost;
import pgrid.util.SecurityHelper;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.core.index.IndexManager;
import pgrid.core.maintenance.identity.IdentityManager;
import pgrid.network.protocol.ChallengeMessage;
import pgrid.network.protocol.ChallengeReplyMessage;

/**
 * This class is responsible for the challenge phase before any secured communication.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * todo: This class has to be ported to the new architecture of P-Grid
 */
public class Challenger implements Runnable {

	/**
	 * Time out
	 */
	protected int TIMEOUT = 1000 * 30; // 30s.

	/**
	 * host to challenge
	 */
	protected PGridHost mHost;

	/**
	 * Challenge message
	 */
	protected ChallengeMessage mMsg;

	/**
	 * The Identity Manager.
	 */
	private IdentityManager mIdentMgr = IdentityManager.sharedInstance();

	/**
	 * The date Manager.
	 */
	private IndexManager mDataMgr = null;

	/**
	 * The message manager
	 */
	private MessageManager mMsgMgr = MessageManager.sharedInstance();

	/**
	 * This constructor must be used when we want to challenge
	 * a host.
	 *
	 * @param host to challenge
	 */
	public Challenger(PGridHost host) {
		mDataMgr = PGridP2P.sharedInstance().getIndexManager();
		mHost = host;
	}

	/**
	 * This constructor must be used when we want to response to a challenge
	 * <code>msg</code>.
	 *
	 * @param host to challenge
	 * @param msg  the challenge
	 */
	public Challenger(PGridHost host, ChallengeMessage msg) {
		mDataMgr = PGridP2P.sharedInstance().getIndexManager();
		mHost = host;
		mMsg = msg;
	}

	/**
	 * Start the challenge - response
	 *
	 * @return true if challenged host has succeeded
	 */
	public boolean challengeHost(Connection conn) {
		// todo: do the port
		/*
		String publicKey = mHost.getPublicKey();
		Random rnd = new Random(System.currentTimeMillis());

		long random = rnd.nextLong();


		String challenge = SecurityHelper.encode("" + random, publicKey);

		ChallengeMessage challengeMsg = new ChallengeMessage(challenge);

		mMsgMgr.addWaiter(challengeMsg.getGUID());
		boolean sent = ConnectionManager.sharedInstance().sendPGridMessage(mHost, conn, challengeMsg);

		if (sent) {
			//wait for reply
			ChallengeReplyMessage response = mMsgMgr.getChallengeResponse(challengeMsg.getGUID(), TIMEOUT);
			mMsgMgr.removeResponseWaiter(challengeMsg.getGUID());

			if (response != null && SecurityHelper.decode(response.getResponse(), publicKey).equals("" + random)) {
				return true;
			}
		}
		*/

		return false;
	}

	/**
	 * Response to a challenge
	 *
	 * @return true if challenged host has succeeded
	 */
	public void responseToChallenge() {

		String challenge = mMsg.getChallenge();
		String publicKey = mIdentMgr.getPublicKey();
		String privateKey = mIdentMgr.getPrivateKey();

		// create the response by decoding the challenge with the private key and
		// re-encoding it with the private key
		String response = SecurityHelper.encode(SecurityHelper.decode(challenge, privateKey), privateKey);

		ChallengeReplyMessage responseMsg = new ChallengeReplyMessage(mMsg.getGUID(), response);

		// TESTS
		/* todo BRICKS fix
		if (MaintenanceTester.addressChanged && !MaintenanceTester.mAlreadyChallenged.contains(mMsg.getHeader().getPeer().getGUID().toString())) {
			MaintenanceTester.mAlreadyChallenged.add(mMsg.getHeader().getPeer().getGUID().toString());
			responseMsg = new ChallengeReplyMessage(mMsg.getGUID(), "0");
		}
		*/
		// TESTS

		mMsgMgr.sendMessage(mMsg.getHeader().getHost(), responseMsg, null);
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		responseToChallenge();

	}

}
