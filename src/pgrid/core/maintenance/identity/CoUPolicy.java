/**
 * $Id: CoUPolicy.java,v 1.2 2005/11/07 16:56:35 rschmidt Exp $
 *
 * Copyright (c) 2002 The P-Grid Team,
 *                    All Rights Reserved.
 *
 * This file is part of the P-Grid package.
 * P-Grid homepage: http://www.p-grid.org/
 *
 * The P-Grid package is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This package is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file LICENSE.
 * If not you can find the GPL at http://www.gnu.org/copyleft/gpl.html
 */
package pgrid.core.maintenance.identity;

// import test.planetlab.MaintenanceTester;
import p2p.basic.Key;
import pgrid.core.index.IndexManager;
import pgrid.core.search.SearchManager;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.interfaces.basic.PGridP2PFactory;
import pgrid.network.MessageManager;
import pgrid.*;

import java.io.File;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * This class implement the Correct On Use (CoU, Eager) algorithm to do P-Grid maintenance.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 */
public class CoUPolicy implements JoinLeaveProtocol, MaintenancePolicy {

	//FIXME: planetlab
	private static final String GUID_QUERY_RECEIVED_FILE = pgrid.Constants.LOG_DIR + "guidQueryReceived.dat";
	private static File guidQueryReceivedFile;

	/**
	 * The data item manager.
	 */
	private IndexManager mIndexManager = null;

	/**
	 * The P-Grid facility.
	 */
	private PGridP2P mPGridP2P = null;

	/**
	 * The identity manager
	 */
	private IdentityManager mIDMgr = null;

	/**
	 * The search manager.
	 */
	private SearchManager mSearchManager;

	/**
	 * The message manager
	 */
	private MessageManager mMsgMgr = null;
	/**
	 * Name of this protocol
	 */
	public final static String PROTOCOL_NAME = "CoU";

	/**
	 * Number of attemps to get the quorum
	 */
	protected int mAttempt;

	/**
	 * Quorum
	 */
	protected int mQuorum;


	/**
	 * The current mMapping
	 */
	protected IdentityIndexEntry mMapping = null;

	/**
	 * Time out for the mapping query
	 */
	protected final int TIMEOUT = 1000 * 60; // 60s

	/**
	 * Running update
	 */
	protected Hashtable mRunningUpdate;

	/**
	 * Running update thread
	 */
	protected Hashtable mRunningThread;

	// TESTS
	static {
		if (PGridP2P.sharedInstance().isInTestMode()) {
			guidQueryReceivedFile = new File(GUID_QUERY_RECEIVED_FILE);
			guidQueryReceivedFile.delete();
		}
	}
	// TESTS

	/**
	 * This object takes care of the mapping task
	 */
	protected class MappingHandler implements Runnable {

		/**
		 * This mode will query P-Grid and update the host
		 */
		public static final int UPDATE_MODE = 0;

		/**
		 * This mode will query P-Grid dans put the host in a variable that could
		 * be retreived with getRequestedHost.
		 */
		public static final int QUERY_MODE = 1;

		/**
		 * Host to update
		 */
		protected PGridHost mHost;

		/**
		 * Requested host
		 */
		protected PGridHost mRequestedHost;

		/**
		 * The guid query
		 */
		protected Query mQuery = null;

		/**
		 * The current quorum
		 */
		protected int mCurMaxQuorum = 0;

		/**
		 * Accumulator by host mMapping returned
		 */
		protected Hashtable mReplyAcc = new Hashtable();

		/**
		 * True if the update succeeded
		 */
		protected boolean mSucceeded = false;

		/**
		 * True iff the new address is different as the old one
		 */
		protected boolean mAddressHasChange = false;

		/**
		 * The mode to use
		 */
		protected int mMode;

		/**
		 * Constructor
		 *
		 * @param host to update
		 */
		public MappingHandler(PGridHost host, int mode) {
			//todo: port to the new architecture
			/*mHost = host;
			Key key = new PGridKey(Converter.bytesToBinaryString(mHost.getGUID().getBytes(), 0, 2));
			mQuery = new Query(mPGridP2P.getLocalHost(), new GUIDType(), mHost.getGUID().toString(), key);
			mMode = mode;*/
		}

		/**
		 * Returns true if the update has succeeded. One must wait until the thread
		 * dies.
		 *
		 * @return true if update has succeeded
		 */
		public boolean hasSucceeded() {
			return mSucceeded;
		}

		/**
		 * Returns true if the address has change.
		 *
		 * @return true if the address has change
		 */
		public boolean addressHasChanged() {
			return mAddressHasChange;
		}

		/**
		 * Get requested host.
		 *
		 * @return requested host
		 */
		public PGridHost getRequestedHost() {
			return (PGridHost)mMapping.getPeer();
		}

		/**
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			//todo: port to the new architecture
			/*
						Thread s;
						long beforeTime = 0, afterTime = 0;

						IdentityManager.LOGGER.finer("Starting a new mapping thread for '" + mHost.toHostString() + "'.");

						// register to get the query reply
						mMsgMgr.addQueryReplyListener(this);

						for (int i = 0; i < mAttempt; i++) {
							IdentityManager.LOGGER.finest("Attempt number " + (i + 1) + " for '" + mHost.toHostString() + "'.");
							// if the quorum has been reached, break
							synchronized (mQuery) {
								if (mCurMaxQuorum >= mQuorum) {
									IdentityManager.LOGGER.finest("Attemps number " + (i + 1) + " has been aborted. Quorum already reached for host '" + mHost.toHostString() + "'.");
									break;
								}
							}

							// start a searcher
							//FIXME: decomment REPLICATION

							s = new Thread(new Searcher(mQuery, 1Constants.REPLICATION_FACTOR));
							s.setDaemon(true);
							s.start();
							//mSearchManager.search(mQuery);
							IdentityManager.LOGGER.finest("Searcher " + (i + 1) + " has been started for '" + mHost.toHostString() + "'.");
							// wait for the result
							synchronized (mQuery) {
								try {
									beforeTime = System.currentTimeMillis();
									mQuery.wait(TIMEOUT);
									afterTime = System.currentTimeMillis();
								} catch (InterruptedException inter) {
									// do nothing
								}
							}

							if (s.isAlive()) s.interrupt();

							// check if a time out occured.
							if ((afterTime - beforeTime) >= TIMEOUT) {
								IdentityManager.LOGGER.finest("Time out occured for attempt " + (i + 1) + " for '" + mHost.toHostString() + "'.");
								continue;
							}

						}

						synchronized (mQuery) {
							// check if the quorum has been reached
							if (mCurMaxQuorum >= mQuorum) {
								// the corrum has been reached and the host updated.
								mSucceeded = true;
							} else {
								mSucceeded = false;
							}
							mQuery = null;
						}

						mMsgMgr.removeQueryReplyListener(this);
						*/

		}

		public void newQueryReply(QueryReply queryReply) {
			// check if we are responsable for this query
			if (mQuery == null || !queryReply.getGUID().equals(mQuery.getGUID())) return;
			int current;
			Iterator iterator = null;

			if (queryReply.getResultSet() != null)
				iterator = queryReply.getResultSet().iterator();

			synchronized (mQuery) {
				IdentityManager.LOGGER.finer("New mapping received for host '" + mHost.toHostString() + "'.");


				if (queryReply.getType() == QueryReply.TYPE_NOT_FOUND || iterator == null) {
					// the query has hit the right peer, but the peer did not have the
					// mapping.

					//                  FIXME: planetlab:
					mQuery.notifyAll();
					//                  FIXME: planetlab:

				} else {
					// go through all result and add them to the list of result
					// for quorum verification.
					while (iterator.hasNext()) {
						IndexEntry element = (IndexEntry)iterator.next();

						// process only identity data items
						if (!(element instanceof IdentityIndexEntry)) continue;

						Integer acc = (Integer)mReplyAcc.get(element.getPeer().toString());
						if (acc == null) {
							acc = new Integer(0);
						}
						current = acc.intValue() + 1;

						// this could only happen if the validity check has failed.
						if (current == 0) continue;

						// keep a pointer on the most frequent reply
						if (mCurMaxQuorum < current) {
							// check validity
							if ((mMapping != null && mMapping.equals(element)) || checkUpdateValidity((IdentityIndexEntry)element, mHost.getPublicKey())) {
								mCurMaxQuorum = current;
								mMapping = (IdentityIndexEntry)element;
							} else {
								// this is not a valid mMapping reset its counter
								current = -1;
							}
						}

						// save acc
						acc = new Integer(current);
						mReplyAcc.put(element.getPeer().toString(), acc);
					}
				}


				// check if the quorum has been reached
				if (mCurMaxQuorum >= mQuorum) {
					IdentityManager.LOGGER.finer("Quorum for mapping '" + mHost.toHostString() + "' has been reached.");
					// the quorum has been reached. Correct routing table
					// update host
					//                  FIXME: planetlab
					/* todo BRICKS fix
					try {
						java.io.FileWriter writer = new java.io.FileWriter(guidQueryReceivedFile, true);
						writer.write(String.valueOf(System.currentTimeMillis()) + " " +
								queryReply.getGUID() + " " +
								MaintenanceTester.mProbability[MaintenanceTester.mIndex] + "\n");
						writer.close();

					} catch (IOException e) {
						e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
					}
					*/
					//                  FIXME: planetlab

					if (mMode == UPDATE_MODE) {
						if (mMapping != null && !((Host)mMapping.getPeer()).equals(mHost)) {
							//synchronized(mHost) {
							mHost.setIP(mMapping.getPeer().getIP());
							mHost.setPort(mMapping.getPeer().getPort());

							// keep a copy in the cache
							// TODO Renault, fix me: PGridHost.cacheHost(mHost);
							//}
							mAddressHasChange = true;
							mPGridP2P.getRoutingTable().save();
						} else {
							mAddressHasChange = false;
						}
					} else if (mMode == QUERY_MODE) {
						// save host information
						mRequestedHost = (PGridHost)mMapping.getPeer();
					}
					IdentityManager.LOGGER.finer("New address: '" + mHost.toHostString() + "'.");
					// wake up the mapping thread
					mQuery.notifyAll();
				} else {
					IdentityManager.LOGGER.finer("Quorum for mapping '" + mHost.toHostString() + "' is currently not reached.");
				}
			}
		}

	}

	/**
	 * Default constructor
	 */
	public CoUPolicy() {
		mPGridP2P = PGridP2P.sharedInstance();
		mIndexManager = mPGridP2P.getIndexManager();
		mMsgMgr = MessageManager.sharedInstance();
		mIDMgr = IdentityManager.sharedInstance();
		mSearchManager = SearchManager.sharedInstance();
		mAttempt = mPGridP2P.propertyInteger(Properties.IDENTITY_CONNECTION_ATTEMPS);
		mQuorum = mPGridP2P.propertyInteger(Properties.IDENTITY_MIN_QUORUM);
		mRunningUpdate = new Hashtable();
		mRunningThread = new Hashtable();
	}

	/**
	 * This method make use of the public key available in the identity manager.
	 * Make sure it is available.
	 *
	 * @see pgrid.core.maintenance.identity.JoinLeaveProtocol#newlyJoined()
	 */
	public void newlyJoined() {
		Constants.LOGGER.config("Insert local peer into P-Grid");

		//check if the public key is available
		if (mIDMgr.getPublicKey() == null) {
			mIDMgr.loadPublicKey();
		}

		// create the key
		Key key = PGridP2PFactory.sharedInstance().generateKey(mPGridP2P.getLocalHost().getGUID().toString());

		//create the data item
		XMLIdentityIndexEntry item = new XMLIdentityIndexEntry(mIDMgr.getType(), mPGridP2P.getLocalHost(), key, mIDMgr.getPublicKey(), System.currentTimeMillis(), mPGridP2P.getLocalHost().toString());

		// add the mMapping in local. It will be put in P-Grid like the other items
		Vector toAdd = new Vector();
		toAdd.add(item);
		mIndexManager.getIndexTable().addAll(toAdd);
	}

	/**
	 * @see pgrid.core.maintenance.identity.JoinLeaveProtocol#join()
	 */
	public void join() {
		if (PGridP2P.sharedInstance().getRoutingTable().isModifiedIp()) {
			Constants.LOGGER.config("Update ID-IP '" + mPGridP2P.getLocalHost().toHostString() + "' of P-Grid");

			// load the private key
			if (mIDMgr.getPrivateKey() == null) {
				mIDMgr.loadPrivateKey();
			}

			// create the key
			Key key = PGridP2PFactory.sharedInstance().generateKey(mPGridP2P.getLocalHost().getGUID().toString());
			XMLIdentityIndexEntry item = new XMLIdentityIndexEntry(mIDMgr.getType(), mPGridP2P.getLocalHost(), key, "", System.currentTimeMillis(), mPGridP2P.getLocalHost().toString());

			IdentityMappingUpdater idUpdater = new IdentityMappingUpdater(item);

			if (!idUpdater.remoteUpdate()) {
				Constants.LOGGER.config("Identification faild, ID-IP did not reach the minimum quorum. Exit");
				System.exit(-1);
			}
		} else {
			Constants.LOGGER.config("ID-IP '" + mPGridP2P.getLocalHost().toHostString() + "' up to date.");
		}
	}

	/**
	 * @see pgrid.core.maintenance.identity.JoinLeaveProtocol#leave()
	 */
	public void leave() {
		// in this algorithm, nothing is done here
	}

	/**
	 * @see pgrid.core.maintenance.identity.JoinLeaveProtocol#getProtocolName()
	 */
	public String getProtocolName() {
		return PROTOCOL_NAME;
	}


	/**
	 * Check if the update could be processed. The signature is decrypted and checked.
	 *
	 * @param toUpdate
	 * @return true if the signature is correct
	 */
	protected boolean checkUpdateValidity(IdentityIndexEntry toUpdate, String publicKey) {
		//FIXME: perform cryptography stuff
		return true;
	}

	/**
	 * Looks for the IP corresponding for ad ID
	 *
	 * @param host to modify
	 * @param join true if the thread must join the mapping thread
	 * @return true if the host address has been modified.
	 */
	protected boolean queryMapping(PGridHost host, boolean join) {
		MappingHandler mappingHandler;
		mappingHandler = (MappingHandler)mRunningUpdate.get(host);
		Thread mappingThread = (Thread)mRunningThread.get(host);

		// if the host has become offline, abort the connection
		if (host.getState() == PGridHost.HOST_OFFLINE) return false;

		// if the host has become OK, Return the succeeded status
		if (host.getState() == PGridHost.HOST_OK) return mappingHandler.hasSucceeded();

		// if no update is running or the update is outdated, create a new update
		if (mappingThread == null || mappingHandler == null) {
			host.setState(PGridHost.HOST_UPDATING);
			mappingHandler = new MappingHandler(host, MappingHandler.UPDATE_MODE);
			mappingThread = new Thread(mappingHandler);
			mRunningThread.put(host, mappingThread);
			mRunningUpdate.put(host, mappingHandler);
			mappingThread.setDaemon(true);
			mappingThread.start();
			host.incMappingAttemps();
		}

		if (join) {
			try {
				mappingThread.join();
			} catch (InterruptedException i) {
				// do nothing
			}
		}

		mRunningThread.remove(host);
		//mRunningUpdate.remove(host);
		// if the mapping give the same result as the address already had,
		// consider this host as offline otherwise, set it to ok
		if (!mappingHandler.addressHasChanged() && host.getMappingAttemps() >= mAttempt) {
			host.setState(PGridHost.HOST_OFFLINE);
		} else if (mappingHandler.hasSucceeded())
			host.setState(PGridHost.HOST_OK);
		else
			host.setState(PGridHost.HOST_STALE);

		return mappingHandler.hasSucceeded();
	}

	/**
	 * In eager strategy (CoU), each failure in routing table
	 * should be corrected immediatelly
	 *
	 * @see pgrid.core.maintenance.identity.MaintenancePolicy#stale(pgrid.PGridHost)
	 */
	public boolean stale(PGridHost host) {
		if (host.getState() != PGridHost.HOST_UPDATING)
			host.setState(PGridHost.HOST_STALE);

		// if the bootstrap host is unreachable, nothing can be done.
		if (host.getGUID() == null) {
			host.setState(PGridHost.HOST_OFFLINE);
			return false;
		}

		// if the percentage is higher then the minimum, correct this host.
		return queryMapping(host, true);
	}

	/**
	 * @see pgrid.core.maintenance.identity.MaintenancePolicy#handleUpdate(pgrid.IndexEntry)
	 */
	public boolean handleUpdate(IndexEntry item) {
		if (item == null || !(item instanceof XMLIdentityIndexEntry)) return false;

		XMLIdentityIndexEntry newData = (XMLIdentityIndexEntry)item;
		XMLIdentityIndexEntry dataItem = (XMLIdentityIndexEntry)mIDMgr.getItem(newData.getPeer().getGUID());

		// check if the mMapping can be update
		if (dataItem == null || !checkUpdateValidity(newData, dataItem.getPublicKey())) return false;

		IdentityManager.LOGGER.finer("Old address: " + ((PGridHost)dataItem.getPeer()).toHostString() + " new address: " + ((PGridHost)newData.getPeer()).toHostString() + ".");

		synchronized (dataItem) {
			PGridHost newHost = (PGridHost)newData.getPeer();
			PGridHost toUpgrade = mPGridP2P.getRoutingTable().getHost(newHost.getGUID());

			// upgrade the host used by P-GRID
			toUpgrade.setPort(newHost.getPort());
			toUpgrade.setSpeed(newHost.getSpeed());
			toUpgrade.setIP(newHost.getIP());

			// upgrade identity data item mMapping
			dataItem.setData(newData.getData().toString());
			dataItem.setPeer(((PGridHost)newData.getPeer()));
			dataItem.setKey(newData.getKey());
			dataItem.setSignature(newData.getSignature());

			// set the data table stat to unsaved.
			mIndexManager.getIndexTable().touch();
		}

		return true;
	}

}
