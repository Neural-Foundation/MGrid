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

import p2p.basic.Key;
import p2p.basic.KeyRange;
import p2p.basic.P2P;
import p2p.basic.P2PFactory;
import p2p.basic.Peer;
import p2p.index.Query;
import p2p.index.TypeHandler;
import p2p.index.events.NoSuchTypeException;
import p2p.index.events.IndexListener;
import p2p.index.events.SearchListener;
import pgrid.*;
import pgrid.Properties;
import pgrid.core.DBManager;
import pgrid.core.maintenance.DbCsvUtils;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.interfaces.basic.PGridP2PFactory;
import pgrid.interfaces.index.PGridIndexFactory;
import pgrid.network.protocol.IndexModifierCSVMessage;
import pgrid.network.protocol.IndexModifierMessage;
import pgrid.network.protocol.XMLIndexTable;
import pgrid.util.logging.LogFormatter;
import pgrid.util.monitoring.MonitoringManager;
import test.demo.HelloWorld;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import mgrid.core.HBaseManager;

/**
 * This class represents the file manager for all shared and downloaded
 * files.
 * This class implements the <code>Singleton</code> pattern as defined by
 * Gamma et.al. As there could only exist one instance of this class, other
 * clients must use the <code>sharedInstance</code> function to use this class.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class IndexManager {

	/**
	 * The only instance of this class.
	 */
	private static final IndexManager mInstance = new IndexManager();

	/**
	 * The Data Table.
	 */
	private DBIndexTable mDBIndexTable = null;

	/**
	 * The CSV Data Table.
	 */
	private CSVIndexTable mCSVIndexTable = null;

	/**
	 * The recv Distribution CSV Data Table.
	 */
	private CSVIndexTable mDistrCSVIndexTable = null;

	/**
	 * The toSend Distribution CSV Data Table.
	 */
	private CSVIndexTable mToDistrCSVIndexTable = null;

	/**
	 * The Data Table.
	 */
	private DBIndexTable mDBIndexTableSubSet = null;

	/**
	 * The Database manager.
	 */
	private DBManager mDBManager = null;
	
	/**
	  The HBase Database manager.
	*/
	private HBaseManager mHDBManager = null;

	/**
	 * The data distributor.
	 */
	private Distributor mDistributor = null;

	/**
	 * The PGrid P2P instance.
	 */
	private PGridP2P mPGridP2P = null;

	/**
	 * Replicas index table signature
	 */
	private Hashtable mRemotesignature = null;

	/**
	 * Storage factory
	 */
	public PGridIndexFactory mStorageFactory = PGridIndexFactory.sharedInstance();

	/**
	 * The list of listener for a type of data item.
	 */
	private Hashtable mStorageListener = new Hashtable();

	/**
	 * The list of listener for all types of data item.
	 */
	private ArrayList mStorageListenerNoType = new ArrayList();
	

	/**
	 * The PGrid.Distributor logger.
	 */
	static final Logger LOGGER = Logger.getLogger("PGrid.IndexManager");

	static {
		LogFormatter formatter = new LogFormatter();
		formatter.setDateFormat("HH:mm:ss");
		formatter.setFormatPattern(LogFormatter.DATE + ": " + LogFormatter.MESSAGE + LogFormatter.NEW_LINE + LogFormatter.THROWABLE);
		Constants.initChildLogger(LOGGER, formatter, null);
	}

	/**
	 * Constructs the Storage Manager.
	 */
	protected IndexManager() {
	}
	
	/**
	 * Initialize all the data tables
	 */
	private void initDataTables(){
		Constants.LOGGER.info("Initializing all the dataTables");
		
		mCSVIndexTable = new CSVIndexTable(true);
		mDistrCSVIndexTable = new CSVIndexTable("recDistr.csv",true); //FileReceived from Remote Peer during distribution
		mToDistrCSVIndexTable = new CSVIndexTable("toDistr.csv",true);//FileToBeSent to the Remote Peer during distribution
		
		mPGridP2P = PGridP2P.sharedInstance();

		mDBManager = DBManager.sharedInstance();
		mDBManager.init(mPGridP2P.propertyBoolean(Properties.IN_MEMORY_DB));
		
		
		System.out.println("Created a P2P instance. ");
		mHDBManager =  HBaseManager.sharedInstance();	
	

		mDBIndexTable = new DBIndexTable(mPGridP2P.getLocalHost());
		mRemotesignature = mDBIndexTable.getSignatureCache();
		
		if(mDBIndexTable.count() == mCSVIndexTable.count()){
			mCSVIndexTable.setSignature(mDBIndexTable.getSignature());
			Constants.LOGGER.finest("CsvTable and DBTable have same signature"+mDBIndexTable.count());
		}else{
			Constants.LOGGER.finest("CsvTable and DBTable DO-NOT have same signature"+mDBIndexTable.count());
		}

	}
	
	/**
	 * Init and Launch the Distributor Thread
	 */
	private void initDistributor(){
		// start the distributor thread
		mDistributor = new Distributor();
		Thread distributorThread = new Thread(mDistributor, "Distributor");
		distributorThread.setDaemon(true);
		distributorThread.start();
	}

	/**
	 * Constructs the Storage Manager.
	 *
	 * @param file the local data table file name.
	 * @param host the local host.
	 */
	synchronized public void init(String file, PGridHost host) {
		initDataTables();
		initDistributor();
	}

	/**
	 * Returns the only instance of this class.
	 * @return the only instance of this class.
	 */
	public static IndexManager getInstance() {
//		if (mInstance == null) {
//			synchronized(IndexManager.class) {
//				 mInstance = new IndexManager();
//			}
//		}
		return mInstance;
	}

	/**
	 * Compacts the DB and commits.
	 */
	public void compactDB() {
		mDBManager.compactDB();
	}

  /**
   * Create a IndexEntry instance compatible with the Storage implementation.
   *
   * @param type the data item's type
   * @return a IndexEntry instance
   * @throws NoSuchTypeException if the provided Type is unknown.
   */
  public p2p.index.IndexEntry createIndexEntry(String type) throws NoSuchTypeException {
	Type t = mStorageFactory.createType(type);
	TypeHandler handler = mStorageFactory.getTypeHandler(t);

	return handler.createIndexEntry();
  }

	/**
	 * Create a IndexEntry instance compatible with the Storage implementation.
	 *
	 * @param type the data item's type
	 * @param data the encapsulated data
	 * @return a IndexEntry instance
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public p2p.index.IndexEntry createIndexEntry(p2p.index.Type type, Object data) throws NoSuchTypeException {
		TypeHandler handler = mStorageFactory.getTypeHandler(type);

		return handler.createIndexEntry(data);
	}

	/**
	 * Create a IndexEntry instance compatible with the Storage implementation.
	 *
	 * @param type the data item's type
	 * @param data the encapsulated data
	 * @return a IndexEntry instance
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public p2p.index.IndexEntry createIndexEntry(GUID guid, Type type, PGridKey key, PGridHost host, Object data) throws NoSuchTypeException {
		TypeHandler handler = mStorageFactory.getTypeHandler(type);

		return handler.createIndexEntry(guid, key, host, data);
	}

	/**
	 * Generate a Key for a given string
	 *
	 * @param type        the Type of items the query is for.
	 * @param query the object that defines the query.
	 * @return a Key for a given string
	 */
	public Key generateKey(p2p.index.Type type, String query) {
		TypeHandler handler = mStorageFactory.getTypeHandler(type);

		return handler.generateKey(query);
	}

	/**
	 * Create a Query instance compatible with the Storage implementation.
	 *
	 * @param type        the Type of items the query is for.
	 * @param host        the host requesting the query.
	 * @param query the object that defines the query.
	 * @return a Query instance.
	 */
	public p2p.index.Query createQuery(p2p.index.Type type, Peer host, String query) {

		return createQuery(type, null, host, query);
	}

	/**
	 * Create a Query instance compatible with the Storage implementation.
	 *
	 * @param type        	the Type of items the query is for
	 * @param lowerBound	the string object that defines the lower bound of the query
	 * @param higherBound	the string object that defines the higher bound of the query
	 * @return a Query instance
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public p2p.index.Query createQuery(p2p.index.Type type, Peer host, String lowerBound, String higherBound) {
		
		return createQuery(type, null, host, lowerBound, higherBound);
	}
	
	/**
	 * Create a Query instance compatible with the Storage implementation inclusing the original query co-ordinatess
	 *
	 * @param type        	the Type of items the query is for
	 * @param lowerBound	the string object that defines the lower bound of the query
	 * @param higherBound	the string object that defines the higher bound of the query
	 * @return a Query instance
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public p2p.index.Query createQuery(p2p.index.Type type, Peer host, String lowerBound, String higherBound
			, Long origxMin, Long origxMax, Long origyMin, Long origyMax) {

		return createQuery(type, null, host, lowerBound, higherBound, origxMin, origxMax,origyMin, origyMax);
	}

	/**
	 * Create a Query instance compatible with the Storage implementation.
	 *
	 * @param type        the Type of items the query is for.
	 * @param host        the host requesting the query.
	 * @param query the object that defines the query.
	 * @return a Query instance.
	 */
	public p2p.index.Query createQuery(p2p.index.Type type, Collection<p2p.basic.GUID> refGuids, Peer host, String query) {
		TypeHandler handler = mStorageFactory.getTypeHandler(type);

		Query theQuery = new pgrid.Query((PGridHost)host, type, query, null);
	if (refGuids != null)
			((pgrid.Query)theQuery).setQueryReferences(refGuids);
		String searchQuery = handler.submitSearchLowerBoundValue(theQuery);
		if (searchQuery == null)		
			searchQuery = query;
		Key key = handler.generateKey(searchQuery);
		((pgrid.Query)theQuery).setKey(key);
	
		return theQuery;
	}

	/**
	 * Create a Query instance compatible with the Storage implementation.
	 *
	 * @param type        	the Type of items the query is for
	 * @param refGuids		All GUID references for this query.
	 * @param lowerBound	the string object that defines the lower bound of the query
	 * @param higherBound	the string object that defines the higher bound of the query
	 * @return a Query instance
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public p2p.index.Query createQuery(p2p.index.Type type, Collection<p2p.basic.GUID> refGuids, Peer host, String lowerBound, String higherBound) {
		TypeHandler handler = mStorageFactory.getTypeHandler(type);

		RangeQuery rQuery = new pgrid.RangeQuery((PGridHost)host, type, null, lowerBound, higherBound);
		if (refGuids != null)
			rQuery.setQueryReferences(refGuids);

		String searchQueryLB = handler.submitSearchLowerBoundValue(rQuery);
		if (searchQueryLB == null)
			searchQueryLB = lowerBound;
		String searchQueryHB = handler.submitSearchHigherBoundValue(rQuery);
		if (searchQueryHB == null)
			searchQueryHB = higherBound;

		KeyRange key = handler.generateKeyRange(searchQueryLB, searchQueryHB);

		rQuery.setKeyRange(key);

		return rQuery;
	}
	/**
	 * Create a Query instance compatible with the Storage implementation including the original query co-ordinates.
	 *
	 * @param type        	the Type of items the query is for
	 * @param refGuids		All GUID references for this query.
	 * @param lowerBound	the string object that defines the lower bound of the query
	 * @param higherBound	the string object that defines the higher bound of the query
	 * @return a Query instance
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public p2p.index.Query createQuery(p2p.index.Type type, Collection<p2p.basic.GUID> refGuids, Peer host, String lowerBound, String higherBound,
			 Long origxMin, Long origxMax, Long origyMin, Long origyMax	) {
		
		TypeHandler handler = mStorageFactory.getTypeHandler(type);

		RangeQuery rQuery = new pgrid.RangeQuery((PGridHost)host, type, null, lowerBound, higherBound,origxMin,origxMax,origyMin,origyMax);
		if (refGuids != null)
			rQuery.setQueryReferences(refGuids);

		String searchQueryLB = handler.submitSearchLowerBoundValue(rQuery);
		if (searchQueryLB == null)
			searchQueryLB = lowerBound;
		String searchQueryHB = handler.submitSearchHigherBoundValue(rQuery);
		if (searchQueryHB == null)
			searchQueryHB = higherBound;

		KeyRange key = handler.generateKeyRange(searchQueryLB, searchQueryHB);

		rQuery.setKeyRange(key);

		return rQuery;
	}

	/**
	 * Inserts the given data items.
	 * @param items the items to insert.
	 * @param distribute True iff dataitem insertion should be propagated into the network through the distributor mechanism
	 */
	public void insertIndexEntries(Collection items, boolean distribute) {
		
		MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_INDEX_MGR_INSERT_INDEX);
		Constants.LOGGER.finest("COUNT new ITEMS: "+items.size());
		Constants.LOGGER.finest("COUNT Before Insertion: "+getCSVIndexTable().count());
		
		DbCsvUtils.sharedInstance().addToCSVandDB(items, !distribute);
	
		Constants.LOGGER.finest("COUNT After Insertion: "+(getCSVIndexTable().count()+getToDistrCSVIndexTable().count())+"("+getCSVIndexTable().count()+" , "+getToDistrCSVIndexTable().count()+")");
		LOGGER.finest("Insert "+items.size()+" index items.");

		// inform type handlers about new data items
		ArrayList listeners= null;
		Hashtable inform = new Hashtable();
		p2p.index.Type type = null;
		for (Iterator it = items.iterator(); it.hasNext();) {
			p2p.index.IndexEntry entry = (p2p.index.IndexEntry)it.next();

			// get the storage listener list
			if (type == null || !type.equals(entry.getType())) {
				type = entry.getType();
				listeners = getIndexListener(type);
			}

			ArrayList v = (ArrayList)inform.get(listeners);
			if (v == null)
				v = new ArrayList();
			v.add(entry);
			inform.put(listeners, v);
		}
		Enumeration en = inform.keys();
		while (en.hasMoreElements()) {
			listeners = (ArrayList)en.nextElement();
			Collection it = (Collection)inform.get(listeners);
			for(Iterator listener = listeners.iterator(); listener.hasNext();)
				((IndexListener)listener.next()).indexItemsAdded(it);
		}

//		// try to distribute data items which not belong to the local peer

		MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_INDEX_MGR_INSERT_INDEX);
		
		/** For Replication the local entries should be propogated to the replicas **/
		Collection<IndexEntry> repItems = new ArrayList<IndexEntry>();
		for (Object obj : items) {
			if(PGridP2P.sharedInstance().isLocalPeerResponsible(((IndexEntry)obj).getKey()) && PGridP2P.sharedInstance().getRoutingTable().getReplicas().length != 0){
				repItems.add((IndexEntry)obj);
			}
		}
	//	DbCsvUtils.sharedInstance().addToCSV(getToDistrCSVIndexTable(),repItems);
		/** For Replication the local entries should be propogated to the replicas **/
		

		
	}

	/**
	 * Inserts the given data items in BULK.
	 * All the items will be inserted localy. Once bulk insert is done call for disributing  the records into the network
	 * @param items the items to insert.
	 * @param distribute True iff dataitem insertion should be propagated into the network through the distributor mechanism
	 */
	public void insertIndexEntriesInBulk(Collection items, boolean distribute) {
		
		MonitoringManager.sharedInstance().startTimer(pgrid.util.monitoring.Constants.AT_INDEX_MGR_INSERT_INDEX);
		
		//Inserting into DB

		DbCsvUtils.sharedInstance().addToCSVandDB(items, !distribute);

		MonitoringManager.sharedInstance().stopTimer(pgrid.util.monitoring.Constants.AT_INDEX_MGR_INSERT_INDEX);
		
	}
	
	public void distributeDataAfterBulkInsertion(){
		mDistributor.distributeData();
	}

	/**
	 * Distributes the local indexEntries into the network
	 * @param items
	 */
	public void distributeLocalIndexEntries(Collection items){
		mDistributor.insert(items);
	}
	/**
	 * Update data items.
	 *
	 * @param items the data item to update.
	 */
	public void updateIndexEntries(Collection items) {
		if (PGridP2P.sharedInstance().isInTestMode()) {
			mPGridP2P.getStatistics().DataItemsManaged = mPGridP2P.getIndexManager().getIndexTable().count();
			mPGridP2P.getStatistics().DataItemsPath = DBView.selection(mPGridP2P.getIndexManager().getIndexTable(),mPGridP2P.getLocalHost().getPath()).count();
		}

		LOGGER.finer("Try to update "+items.size()+" data items.");

		// inform type handlers about new data items
		Hashtable inform = new Hashtable();
		ArrayList remote = new ArrayList();
		boolean notResponsable;
		pgrid.IndexEntry itemTemp;

		ArrayList listeners= null;
		p2p.index.Type type = null;
		for (Iterator it = items.iterator(); it.hasNext();) {
			p2p.index.IndexEntry entry = (p2p.index.IndexEntry)it.next();

			// get the storage listener list
			if (type == null || !type.equals(entry.getType())) {
				type = entry.getType();
				listeners = getIndexListener(type);
			}

			ArrayList v = (ArrayList)inform.get(listeners);
			if (v == null)
				v = new ArrayList();
			v.add(entry);
			inform.put(listeners, v);
			LOGGER.finest("Updating index entry with key:"+ entry.getKey()+".");
			if (mPGridP2P.isLocalPeerResponsible(entry.getKey())) {
				// Check if the local host is still responsible for the new key
				itemTemp = (pgrid.IndexEntry)((IndexEntry) entry).clone();
				itemTemp.setKey(getTypeHandler(itemTemp.getType()).generateKey(itemTemp.getData()));
				notResponsable = !mPGridP2P.isLocalPeerResponsible(itemTemp.getKey());

				if (notResponsable) {
					// Those data items will be remove from the local host and send
					// to their responsable host
					remote.add(itemTemp);
					mDBIndexTable.removeIndexEntry((IndexEntry) entry);
					LOGGER.finest("Local peer not responsible anymore of the updated data entry. Old key:"+
							entry.getKey()+" new key:"+itemTemp.getKey());
				}
				else {
					mDBIndexTable.updateIndexEntry((IndexEntry) entry);
					LOGGER.finest("Local peer still responsible of the updated data entry. key: "+itemTemp.getKey());
				}
			}
		}
		// If some data item have change there responsible host, insert them
		if (!remote.isEmpty()) {
			insertIndexEntries(remote, true);
		}

		Enumeration en = inform.keys();
		while (en.hasMoreElements()) {
			listeners = (ArrayList)en.nextElement();
			Collection it = (Collection)inform.get(listeners);
			for(Iterator listener = listeners.iterator(); listener.hasNext();)
				((IndexListener)listener.next()).indexItemsUpdated(it);
		}

		// try to distribute data items which not belong to the local peer
		mDistributor.update(items);

	}

	/**
	 * Remove the data items with given IDs.
	 *
	 * @param dataItems the ids of the data items to be removed
	 * @param propagate
	 */
	public void deleteIndexEntries(Collection dataItems, boolean propagate) {
		// this is a work around to use the same facility for deleting a data item then for update or insert
		if (PGridP2P.sharedInstance().isInTestMode()) {
			mPGridP2P.getStatistics().DataItemsManaged = mPGridP2P.getIndexManager().getIndexTable().count();
			mPGridP2P.getStatistics().DataItemsPath = DBView.selection(mPGridP2P.getIndexManager().getIndexTable(),mPGridP2P.getLocalHost().getPath()).count();
		}

		LOGGER.finest("Try to delete "+dataItems.size()+" data items.");

		// inform type handlers about new data items
		ArrayList local = new ArrayList();
		Hashtable inform = new Hashtable();
		ArrayList listeners = null;
		p2p.index.Type type = null;
		for (Iterator it = dataItems.iterator(); it.hasNext();) {
			p2p.index.IndexEntry entry = (p2p.index.IndexEntry)it.next();

			// get the storage listener list
			if (type == null || !type.equals(entry.getType())) {
				type = entry.getType();
				listeners = getIndexListener(type);
			}

			ArrayList v = (ArrayList)inform.get(listeners);
			if (v == null)
				v = new ArrayList();
			v.add(entry);
			inform.put(listeners, v);
			if (mPGridP2P.isLocalPeerResponsible(entry.getKey())) {
				local.add(entry);
			}
		}
		// update local data
		if (!local.isEmpty())
			mDBIndexTable.removeAll(local, mPGridP2P.getLocalPeer());

		Enumeration en = inform.keys();
		while (en.hasMoreElements()) {
			listeners = (ArrayList)en.nextElement();
			Collection it = (Collection)inform.get(listeners);
			for(Iterator listener = listeners.iterator(); listener.hasNext();)
				((IndexListener)listener.next()).indexItemsRemoved(it);
		}

		// try to distribute data items which not belong to the local peer
		if (propagate)
			mDistributor.delete(dataItems);
	}

	/**
	 * Remove the data items with given Ds.
	 *
	 * @param propagate
	 */
	public void deleteAllLocalIndexEntries(boolean propagate) {
		mDBIndexTable.removeAllOwnedIndexEntries();
	}



	/**
	 * Register a listener of events related to data items.
	 * Such listeners are notified when operations on items
	 * on the the current node are requested.
	 *
	 * @param listener the listener to register
	 * @param type     the type of data items that the listener is interested in
	 */
	public void addIndexListener(IndexListener listener, p2p.index.Type type) {
		synchronized(mStorageListener) {
			ArrayList listeners = (ArrayList) mStorageListener.get(type);
			if (listeners == null) {
				listeners = new ArrayList();
				mStorageListener.put(type, listeners);
			}
			listeners.add(listener);
		}
	}

	/**
	 * Register a listener of events related to data items.
	 * Such listeners are notified when operations on items
	 * of any type on the the current node are requested.
	 *
	 * @param listener the listener to register
	 */
	public void addIndexListener(IndexListener listener) {
		mStorageListenerNoType.add(listener);
	}

	/**
	 * Removed a registered listener.
	 *
	 * @param listener the listener to register
	 * @param type	 the type of data items that the listener is interested in
	 */
	public void removeIndexListener(IndexListener listener, Type type) {
		synchronized(mStorageListener) {
			ArrayList listeners = (ArrayList) mStorageListener.get(type);
			if (listeners != null) {
				listeners.remove(listener);
				if (listeners.isEmpty()) mStorageListener.remove(type);
			}
		}
	}

	/**
	 * Returns a Type Handler instance for a given Type.
	 *
	 * @param type an application-specific type to encapsulate
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public ArrayList getIndexListener(p2p.index.Type type) throws NoSuchTypeException {
		ArrayList listener = new ArrayList();

		if (!mStorageListenerNoType.isEmpty())
			listener.addAll(mStorageListenerNoType);

		synchronized(mStorageListener) {
			ArrayList l = (ArrayList) mStorageListener.get(type);
			if (l != null) listener.addAll(l);
		}

		return listener;
	}

	/**
	 * Returns the signature for the index table.
	 *
	 * @return the signature.
	 */
	public Signature getIndexTableSignature() {
		return mDBIndexTable.getSignature();
	}

	/**
	 * Returns the local PGridP2P Data Table.
	 *
	 * @return the Data Table.
	 */
	public DBIndexTable getIndexTable() {
		return mDBIndexTable;
	}
	
	public CSVIndexTable getCSVIndexTable(){
		return mCSVIndexTable;
	}

	/**
	 * Sets the data table according to a view.
	 *
	 * @param table the new data table.
	 */
//	public void setIndexTable(DBView table) {
//		mDBIndexTable.setIndexTable(table);
//	}

	public void setIndexTable(String path) {
		mDBIndexTable.setIndexTable(path);
	}

	/**
	 * Returns a Type Handler instance for a given Type.
	 *
	 * @param type an application-specific type to encapsulate
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public TypeHandler getTypeHandler(p2p.index.Type type) throws NoSuchTypeException {
		return mStorageFactory.getTypeHandler(type);
	}

	/**
	 * Invoked when a new insert request was received by another host.
	 * @param indexModifier
	 */
	public void remoteIndexModification(IndexModifierMessage indexModifier) {
		Constants.LOGGER.finest("IndexModifier message received");
		mDistributor.remoteDistribution(indexModifier);
	}
	/**
	 * Invoked when a new insert request was received by another host.
	 * @param indexModifierCSV
	 */
	public void remoteIndexModification(IndexModifierCSVMessage indexModifierCSV) {
		Constants.LOGGER.finest("IndexModifierCSV message received");
		mDistributor.remoteDistribution(indexModifierCSV);
	}

	/**
	 * Search the local data table for matching items.
	 * Callback is notified for new result.
	 *
	 * @param query    the query used to specify the search
	 * @param listener an object to notify whith the result set
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 * @throws IOException 
	 */
	public void matchLocalItems(p2p.index.Query query, SearchListener listener) throws NoSuchTypeException{
		TypeHandler handler = getTypeHandler(query.getType());
		handler.handleLocalSearch(query, listener);
	}

	/**
	 * Shutdowns the P-Grid facility.
	 */
	synchronized public void shutdown() {
		getIndexTable().saveSignatureCache(mRemotesignature);		

		if (PGridP2P.sharedInstance().isInTestMode()) {
			writeIndexTable();
		}
		mDBIndexTable.shutdown();
		mDBManager.shutdown();
		mDistributor.shutdown();
	}

	/**
	 * Saves the shared files to the specified file.
	 * Used for testing only.
	 */
	public void writeIndexTable() {
		// TESTS used for testing only
		XMLIndexTable xmlIndexTable = new XMLIndexTable(mDBIndexTable);
		try {
			FileWriter file = new FileWriter(Constants.DATA_DIR+"IndexTable.xml");
			BufferedWriter out = new BufferedWriter(file);
			String content = xmlIndexTable.toXMLString("", Constants.LINE_SEPERATOR);
			out.write(content);
			out.close();
		} catch (FileNotFoundException e) {
			Constants.LOGGER.log(Level.WARNING, null, e);
		} catch (IOException e) {
			Constants.LOGGER.log(Level.WARNING, null, e);
		}
	}

	/**
	 * Manually propagate all local data items.
	 */
	public void propagateAllLocalIndexes() {
		Collection di = mDBIndexTable.getOwnedIndexEntries();
		if (!di.isEmpty()) mDistributor.insert(di);
	}

	/**
	 * Saves the shared files to the specified file.
	 *  This method is used only for testing purpose.
	 */
	synchronized public void saveIndexTable() {
		//if (PGridP2P.sharedInstance().isInTestMode())
		//	mDataTable.save();
	}

	/**
	 * Sets the Data Table.
	 *  This method is used only for testing purpose.
	 *
	 * @param indexTable a Data Table.
	 */
	public void setIndexTable(IndexTable indexTable) {
		/*if (PGridP2P.sharedInstance().isInTestMode()) {
			mDataTable.clear();
			mDataTable.addAll(indexTable.getDataItems());
		}*/
	}

	/**
	 * Sets the local path. This method is used only for testing purpose.
	 *
	 * @param path the local path.
	 */
	public synchronized void setLocalPath(String path) {
		/*if (PGridP2P.sharedInstance().isInTestMode()) {
			if (mDataTable != null)
				mDataTable.setCommonPrefix(path);
		}*/
	}

	public Signature getPeerIndexTableSignature(PGridHost host) {
		if (mRemotesignature == null) return null;
		return (Signature) mRemotesignature.get(host);
	}

	public void setPeerIndexTableSignature(PGridHost host, Signature sign) {
		mRemotesignature.put(host, sign);
	}

	/**
	 * Returns the prediction subset. This set is used to estimate data distribution in the exchanger. This method is
	 * not synchronized.
	 *
	 * @return the prediction subset.
	 */
	public DBIndexTable getPredictionSubset() {

		if (mDBIndexTableSubSet == null || !mDBIndexTableSubSet.getSignature().equals(mCSVIndexTable.getSignature())) {
			if (mDBIndexTableSubSet != null){
				mDBIndexTableSubSet.delete();
				mDBIndexTableSubSet=null;
			}

			mDBIndexTableSubSet = mCSVIndexTable.randomSubSet(Constants.PREDICTION_SUBSET_SIZE);
			mDBIndexTableSubSet.setSignature(mCSVIndexTable.getSignature());
		}
		return mDBIndexTableSubSet;
	}

	private CSVIndexTable mCSVIndexTableSubSet = null;
	/**
	 * Returns the prediction subset. This set is used to estimate data distribution in the exchanger. This method is
	 * not synchronized.
	 *
	 * @return the prediction subset.
	 */
	public CSVIndexTable getPredictionSubsetCSV() {
		mCSVIndexTableSubSet = mCSVIndexTable.randomSubSetCSV(Constants.PREDICTION_SUBSET_SIZE);
		mCSVIndexTableSubSet.setSignature(mCSVIndexTable.getSignature());
		return mCSVIndexTableSubSet;
	}


	/**
	 * Returns the csv file DISTR CSV FILE which is received from the network.
	 * @return
	 */
	public CSVIndexTable getDistrCSVIndexTable() {
		return mDistrCSVIndexTable;
	}

	/**
	 * Returns the csv file which contains entries that SHOULD BE DISTRIBITUED INTO THE NETWORK
	 * @return
	 */
	public CSVIndexTable getToDistrCSVIndexTable() {
		return mToDistrCSVIndexTable;
	}
	
	/**
	 * Returns an handle to the Distributor
	 * @return
	 */
	public void startDistributor(){
		mDistributor.startDistributor();
	}

	/**
	 * Returns an handle to the Distributor
	 * @return
	 */
	public void stopDistributor(){
		mDistributor.stopDistributor();
	}

}
