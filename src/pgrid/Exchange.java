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

package pgrid;

import p2p.basic.GUID;
import pgrid.core.XMLRoutingTable;
import pgrid.core.index.CSVIndexTable;
import pgrid.core.index.DBIndexTable;
import pgrid.util.LexicalDefaultHandler;

/**
 * This class represents a P-Grid Exchange.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class Exchange extends LexicalDefaultHandler {

	/**
	 * The list of data items.
	 */
	protected DBIndexTable mDataTable = null;

	/**
	 * The list of data items in CSV.
	 */
	protected CSVIndexTable mCSVDataTable = null;

	/**
	 * The message GUID.
	 */
	protected GUID mGUID = null;

	/**
	 * The creating host.
	 */
	protected PGridHost mHost = null;

	/**
	 * The current common length.
	 */
	protected int mLenCurrent = 0;

	/**
	 * The min storage
	 */
	protected int mMinStorage = 0;

	/**
	 * The random number.
	 */
	protected double mRandomNumber = Double.MIN_VALUE;

	/**
	 * The recursion.
	 */
	protected int mRecursion = 0;

	/**
	 * The replication estimate.
	 */
	protected double mReplicateEstimate = 0;

	/**
	 * The routing table of the creating host.
	 */
	protected XMLRoutingTable mRoutingTable = null;

	/**
	 * Creates a new empty exchange.
	 */
	public Exchange() {
	}

	/**
	 * Creates a new exchange with given values.
	 *
	 * @param guid         the Exchange GUID.
	 * @param recursion    the recursion.
	 * @param lCurrent     the current common length.
	 * @param replicaEst   the replication estimate.
	 * @param routingTable the Routing Table of the creating host.
	 * @param dataTable    the list of data items.
	 */
	public Exchange(GUID guid, int recursion, int lCurrent, int minStorage, double replicaEst, XMLRoutingTable routingTable, DBIndexTable dataTable) {
		mGUID = guid;
		mRecursion = recursion;
		mLenCurrent = lCurrent;
		mMinStorage = minStorage;
		mReplicateEstimate = replicaEst;
		mRoutingTable = routingTable;
		mDataTable = dataTable;
	}

	/**
	 * Creates a new exchange with given values.
	 *
	 * @param guid         the Exchange GUID.
	 * @param recursion    the recursion.
	 * @param lCurrent     the current common length.
	 * @param replicaEst   the replication estimate.
	 * @param routingTable the Routing Table of the creating host.
	 * @param dataTable    the list of data items.
	 */
	public Exchange(GUID guid, int recursion, int lCurrent, int minStorage, double replicaEst, XMLRoutingTable routingTable, CSVIndexTable csvDataTable) {
		mGUID = guid;
		mRecursion = recursion;
		mLenCurrent = lCurrent;
		mMinStorage = minStorage;
		mReplicateEstimate = replicaEst;
		mRoutingTable = routingTable;
		mCSVDataTable = csvDataTable;
	}

	/**
	 * Returns the list of data items.
	 *
	 * @return the list of data items.
	 */
	public DBIndexTable getIndexTable() {
		return mDataTable;
	}

	/**
	 * Sets the list of data items.
	 *
	 * @param indexTable the list of data items.
	 */
	public void setIndexTable(DBIndexTable indexTable) {
		mDataTable = indexTable;
	}
	
	/**
	 * Sets the list of data items.
	 *
	 * @param indexTable the list of data items.
	 */
	public void setIndexTable(CSVIndexTable csvIndexTable) {
		mCSVDataTable = csvIndexTable;
	}

	public CSVIndexTable getMCSVDataTable() {
		return mCSVDataTable;
	}
	
	/**
	 * Returns the Exchange GUID.
	 *
	 * @return the Exchange GUID.
	 */
	public GUID getGUID() {
		return mGUID;
	}

	/**
	 * Set the Exchange GUID.
	 */
	public void setGUID(GUID guid) {
		mGUID = guid;
	}

	/**
	 * Returns the creating host.
	 *
	 * @return the creating host.
	 */
	public PGridHost getHost() {
		return mRoutingTable.getLocalHost();
	}

	/**
	 * Set the creating host.
	 */
	public void setHost(PGridHost host) {
		mHost = host;
	}

	/**
	 * Returns the current common length.
	 *
	 * @return the current common length.
	 */
	public int getLenCurrent() {
		return mLenCurrent;
	}

	/**
	 * Set the current common length.
	 */
	public void setLenCurrent(int clen) {
		mLenCurrent = clen;
	}

	/**
	 * Returns the random number.
	 *
	 * @return the random number.
	 */
	public double getRandomNumber() {
		return mRandomNumber;
	}

	/**
	 * Sets the random number.
	 *
	 * @param randomNumber the random number.
	 */
	public void setRandomNumber(double randomNumber) {
		mRandomNumber = randomNumber;
	}

	/**
	 * Returns the recursion.
	 *
	 * @return the recursion.
	 */
	public int getRecursion() {
		return mRecursion;
	}

	/**
	 * Set the recursion.
	 */
	public void setRecursion(int rec) {
		mRecursion = rec;
	}

	/**
	 * Returns the replication estimate.
	 *
	 * @return the replication estimate.
	 */
	public double getReplicaEstimate() {
		return mReplicateEstimate;
	}

	/**
	 * Set the replication estimate.
	 */
	public void setReplicaEstimate(double estimate) {
		mReplicateEstimate = estimate;
	}

	/**
	 * Returns the Routing Table.
	 *
	 * @return the Routing Table.
	 */
	public XMLRoutingTable getRoutingTable() {
		return mRoutingTable;
	}

	/**
	 * set the Routing Table.
	 */
	public void setRoutingTable(XMLRoutingTable rt) {
		mRoutingTable = rt;
	}

	/**
	 * Return the minimum storage before a split
	 * @return the minimum storage before a split
	 */
	public int getMinStorage() {
		return mMinStorage;
	}

	/**
	 * Set the minimum storage before a split
	 */
	public void setMinStorage(int minStorage) {
		mMinStorage = minStorage;
	}


	
}