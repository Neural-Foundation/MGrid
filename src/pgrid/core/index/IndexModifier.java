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
package pgrid.core.index;

import pgrid.*;
import pgrid.core.maintenance.DbCsvUtils;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.MessageManager;
import pgrid.network.router.RouterListener;
import pgrid.network.router.Router;
import pgrid.network.protocol.IndexModifierCSVMessage;
import pgrid.network.protocol.IndexModifierMessage;

import java.util.*;
import java.util.logging.Logger;

/**
 * This class processes data modifier requests.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
class IndexModifier implements RouterListener {

	/**
	 * Represente a delete operation
	 */
	public static final short DELETE = 0;

	/**
	 * Represente an insert operation
	 */
	public static final short INSERT = 1;

	/**
	 * Represente an update operation
	 */
	public static final short UPDATE = 2;


	/**
	 * The PGrid.Distributor logger.
	 */
	private static final Logger LOGGER = Distributor.LOGGER;

	/**
	 * The list of attempts.
	 */
	private Hashtable mAttempts = new Hashtable();


	/**
	 * The list of request
	 */
	private Hashtable mRequests = new Hashtable();


	/**
	 * The distributor.
	 */
	private Distributor mDistributor = null;

	/**
	 * The Message Manager.
	 */
	private MessageManager mMsgMgr = MessageManager.sharedInstance();

	/**
	 * The P-Grid instance.
	 */
	private PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 * Creates the insert handler.
	 *
	 * @param distributor the distributor.
	 */
	IndexModifier(Distributor distributor) {
		mDistributor = distributor;
	}

	/**
	 * Invoked when data items were not distributed successfully.
	 *
	 * @param guid  the GUID of the distribution request.
	 */
	public void distributionFailed(p2p.basic.GUID guid) {
		LOGGER.finest("Data operation request failed.");
		DistributionAttempt attempt = (DistributionAttempt)mAttempts.remove(guid);
		DistributionRequestInt request = (DistributionRequestInt)mRequests.remove(guid);

		if (!attempt.isLocal()) {
			// store data items because they could not be transmitted to a responsible host
			switch (request.getRequest()) {
			case INSERT:
//				mPGridP2P.getIndexManager().getIndexTable().addAll(attempt.getItems());
				DbCsvUtils.sharedInstance().addToCSV(mPGridP2P.getIndexManager().getToDistrCSVIndexTable(), attempt.getItems());
				Constants.LOGGER.finest("Distributed Insert Fail. "+attempt.getItems().size());

				break;
			case UPDATE:
				break;
			case DELETE:
				break;
			default:
				LOGGER.fine("Unknown request type '"+request.getRequest()+".");
			break;
			}

			if (PGridP2P.sharedInstance().isInTestMode()) {
				mPGridP2P.getStatistics().DataItemsManaged = mPGridP2P.getIndexManager().getIndexTable().count();
//				mPGridP2P.getStatistics().DataItemsPath = DBView.selection(mPGridP2P.getIndexManager().getIndexTable(),mPGridP2P.getLocalHost().getPath()).count();
			}
			// TODO retry to insert
		}
	}

	/**
	 * Invoked when data items were distributed successfully.
	 *
	 * @param guid  the GUID of the distribution request.
	 */
	public void distributionSuccess(p2p.basic.GUID guid) {
		LOGGER.finest("Data operation request succeeded.");


		DistributionAttempt attempt = (DistributionAttempt)mAttempts.remove(guid);
		DistributionRequestInt request = (DistributionRequestInt)mRequests.remove(guid);

		if (attempt.isLocal()) {
			// remove data items because they are now stored at a remote host
			switch (request.getRequest()) {
			case INSERT:
				Constants.LOGGER.finest("Distributed Insert Successful. "+attempt.getItems().size());
//				mPGridP2P.getIndexManager().getIndexTable().removeAll(attempt.getItems());
				break;
			case UPDATE:
				break;
			case DELETE:
				break;
			default:
				LOGGER.fine("Unknown request type '"+request.getRequest()+".");
			break;
			}



			if (PGridP2P.sharedInstance().isInTestMode()) {
				int count = mPGridP2P.getIndexManager().getIndexTable().count();
//				int countPath = DBView.selection(mPGridP2P.getIndexManager().getIndexTable(),mPGridP2P.getLocalHost().getPath()).count();

				mPGridP2P.getStatistics().DataItemsManaged = count;
//				mPGridP2P.getStatistics().DataItemsPath = countPath;
			}
		}
	}

	/**
	 * Perform the processing of the given items in the network.
	 * @param request the request.
	 */
	void process(DistributionRequest request) {
		if (mPGridP2P.isSuperPeer()) {
			LOGGER.finest("processing " + request.getItems().size() + " items.");
			if (PGridP2P.sharedInstance().isInTestMode())
				mPGridP2P.getStatistics().UpdatesLocalProcessed++;

			// sort the data items according to their corresponding routing table level
			ArrayList[] levels = mDistributor.sortByLevel(request.getItems());

			// send insert message for each level
			processPerLevel(request, levels, true, GUID.getGUID());

			// send insert message to all replicas
			processAtReplicas(request, levels[levels.length-1], null);
		} else {
			PGridKey key = new PGridKey("");
			// if we are not a super peer, we should forward the message to a randomly chosen super peer.
			LOGGER.finest("Forward distribution message to a super peer.");
			IndexModifierMessage msg = new IndexModifierMessage(GUID.getGUID(), key, request.getRequest(), request.getItems());
			mMsgMgr.route(key, msg, this, null);
		}
	}

	/**
	 * Perform the processing of the items at all replicas excluding the already informed replicas.
	 * @param items the items to be processed.
	 * @param guid the used message id.
	 */
	private void processAtReplicas(DistributionRequestInt request, ArrayList items, GUID guid) {
		if ((items == null) || (items.size() == 0))
			return;
		IndexModifierMessage msg;

		LOGGER.finest("Perform the processing of " + items.size() + " data items in the replicat subnetwork.");

		// send data modifier message to all replicas
		msg = new IndexModifierMessage(guid, new PGridKey(mPGridP2P.getLocalPath()), request.getRequest(), items);

		mMsgMgr.sendToReplicas(msg, null);

	}

	/**
	 * Perform the processing of the data items ordered by level at a responsible host.
	 * @param levels the data items ordered by level.
	 * @param refGUID
	 */
	private void processPerLevel(DistributionRequestInt request, ArrayList[] levels, boolean local, GUID refGUID) {

		for (int i = 0; i < levels.length-1; i++) {
			ArrayList level = levels[i];
			// if no data items for this level exist => next level
			if ((level == null) || (level.size() == 0))
				continue;
			PGridKey key = null;
			IndexModifierMessage msg = null;

			// create common key
			key = new PGridKey(mDistributor.commonKeyForLevel(i));

			LOGGER.finest("Perform the processing of " + level.size() + " data items with prefix key " + key + " at host at level " + i);
			msg = new IndexModifierMessage(refGUID, key, request.getRequest(), level);
			mAttempts.put(msg.getGUID(), new DistributionAttempt(level, local));
			// save a reference
			mRequests.put(msg.getGUID(), request);

			mMsgMgr.route(key, msg, this, null);
		}
	}

	/**
	 * Invoked when a new insert request was received by another host.
	 * @param request the insert request.
	 */
	void remoteProcess(RemoteDistributionRequest request) {
		IndexModifierMessage msg = (IndexModifierMessage)request.getMessage();
		LOGGER.fine("processing remote " + msg.getDataItems().size() + " items.");
		if (PGridP2P.sharedInstance().isInTestMode())
			mPGridP2P.getStatistics().UpdatesRemoteProcessed++;

		boolean replicaBroadcast = false;
		if (msg.getHeader().getRouteHeader().getStrategy().equals(Router.REPLICA_STRATEGY))
			replicaBroadcast = true;

		LOGGER.fine("process new received request.");

		// sort the data items according to their corresponding routing table level
		ArrayList[] levels = mDistributor.sortByLevel(msg.getDataItems());

		// send insert message for each level
		if (!replicaBroadcast)
			processPerLevel(request, levels, false, (GUID) msg.getHeader().getReferences().iterator().next());

		// add items for the local peer to the data table
		ArrayList localItems = levels[levels.length-1];
		if ((localItems == null) || (localItems.size() == 0))
			return;

		switch (request.getRequest()) {
		case INSERT:
			LOGGER.finer("Insert "+localItems.size()+" index items into the index table.");
//			mPGridP2P.getIndexManager().getIndexTable().addAll(localItems);

			DbCsvUtils.sharedInstance().addToCSV(mPGridP2P.getIndexManager().getDistrCSVIndexTable(), localItems);

			break;
		case UPDATE:
			ArrayList remote = new ArrayList();
			boolean notResponsable;
			pgrid.IndexEntry itemTemp;
			IndexEntry item;

			for (Iterator it = localItems.iterator(); it.hasNext();) {
				item = (IndexEntry)it.next();

				// Check if the local host is still responsible for the new key
				itemTemp = (pgrid.IndexEntry)(item).clone();
				itemTemp.setKey(PGridP2P.sharedInstance().getIndexManager().getTypeHandler(itemTemp.getType()).generateKey((String)itemTemp.getData()));
				notResponsable = !mPGridP2P.isLocalPeerResponsible(itemTemp.getKey());


				if (notResponsable) {
					// Those data items will be remove from the local host and send
					// to their responsable host
					remote.add(itemTemp);
					mPGridP2P.getIndexManager().getIndexTable().removeIndexEntry(item);
					LOGGER.finest("Local peer isn't responsible anymore for the updated data item. Old key:"+
							item.getKey()+" new key:"+itemTemp.getKey()+(replicaBroadcast?"(Replicas subnetwork)":"")+".");
				}
				else {
					mPGridP2P.getIndexManager().getIndexTable().updateIndexEntry(item);
					LOGGER.finest("Local peer still responsible of the updated data item. key: "+itemTemp.getKey()+(replicaBroadcast?"(Replicas subnetwork)":"")+".");
				}
			}

			// If some data item have change there responsible host, insert them
			if ((!remote.isEmpty()) && (!replicaBroadcast)) {
				LOGGER.finest("Re-insert the updated data items.");
				mPGridP2P.getIndexManager().insertIndexEntries(remote, true);
			}
			break;
		case DELETE:
			mPGridP2P.getIndexManager().getIndexTable().removeAll(localItems, msg.getHeader().getHost());
			break;
		default:
			LOGGER.fine("Unknown request type '"+request.getRequest()+".");
		break;
		}

		if (PGridP2P.sharedInstance().isInTestMode()) {
			mPGridP2P.getStatistics().DataItemsManaged = mPGridP2P.getIndexManager().getIndexTable().count();
			mPGridP2P.getStatistics().DataItemsPath = DBView.selection(mPGridP2P.getIndexManager().getIndexTable(),mPGridP2P.getLocalHost().getPath()).count();
		}

		// prepare to send insert request to all replicas
		if (!replicaBroadcast)
			processAtReplicas(request, localItems, (GUID)msg.getGUID());

		msg.getIndexTable().delete(); //We are done with using the indexTable, so we are freeing resources

	}

	/**
	 * Invoked when a routing failed.
	 *
	 * @param guid the GUID of the original query
	 */
	public void routingFailed(p2p.basic.GUID guid) {
		distributionFailed(guid);
	}

	/**
	 * Invoked when a routing finished.
	 *
	 * @param guid the GUID of the original query
	 */
	public void routingFinished(p2p.basic.GUID guid) {
		distributionSuccess(guid);
	}

	/**
	 * Invoked when a routing started (reached a responsible peer).
	 *
	 * @param guid	the GUID of the original query
	 * @param message the explanation message.
	 */
	public void routingStarted(p2p.basic.GUID guid, String message) {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
