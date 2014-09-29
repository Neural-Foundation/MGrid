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

import org.xml.sax.helpers.DefaultHandler;
import pgrid.Constants;
import pgrid.PGridHost;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.util.Utils;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * This class represents the Routing Table of the P-Grid facility.
 * It includes the fidget hosts, the hosts for each level of a path, and the replicas.
 *
 * @author @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */
public class RoutingTable extends DefaultHandler implements Cloneable {

	/**
	 * Hashtable of all hosts in the routing table by GUID
	 */
	protected Hashtable mHosts = new Hashtable();

	/**
	 * The list of fidget hosts.
	 */
	protected Collection mFidgets = Collections.synchronizedCollection(new TreeSet());

	/**
	 * The list of references.
	 */
	protected Vector mLevels = new Vector();

	/**
	 * The local host.
	 */
	protected PGridHost mLocalHost = null;

	/**
	 * Lock
	 */
	protected ReadWriteLock mRTLock = new ReentrantReadWriteLock();

	/**
	 * The list of replica hosts.
	 */
	protected Collection mReplicas = Collections.synchronizedCollection(new TreeSet());

	/**
	 * Random generator
	 */
	protected Random mRnd = new Random();

	/**
	 * Create a new empty routing table.
	 */
	public RoutingTable() {
		// do nothing
	}

	public void acquireReadLock() {
		mRTLock.readLock().lock();
	}

	public void releaseReadLock() {
		mRTLock.readLock().unlock();
	}

	public void acquireWriteLock() {
		mRTLock.writeLock().lock();
	}

	public void releaseWriteLock() {
		mRTLock.writeLock().unlock();
	}

	/**
	 * Returns a random subset of the given collection of the given size.
	 *
	 * @param col   the collection.
	 * @param count the size of the random subset
	 * @return the random subset.
	 */
	private Collection randomSelect(Collection col, int count, double seed, boolean reverse) {
		Vector list;
		acquireWriteLock();
		try {
			if (col == null)
				throw new NullPointerException();

			if (col.size() <= count)
				return col;

			Random rnd = new Random((long)(1000*seed));

			list = new Vector(col);

			Collections.shuffle(list, rnd);

			if (reverse) Collections.reverse(list);

		} finally{
			releaseWriteLock();
		}
		return list.subList(0, count);
	}


	/**
	 * Refreshes the routing table by building the union with the given routing table.
	 *
	 * @param routingTable the routing table used to refresh this routing table.
	 * @param commonLen    the common length of the peer's path.
	 * @param lLen         the local path length.
	 * @param rLen         the remote path length.
	 * @param maxFidgets   the amount of fidgets to manage.
	 * @param maxRef       the amount of references to manage per level.
	 * @param firstPart	   the fidget list is divided into two parts. If firstPart is true, the first fidget list is kept
	 * @param random	   the random seed
	 */
	public void refresh(RoutingTable routingTable, int commonLen, int lLen, int rLen, int maxFidgets, int maxRef,
									 boolean firstPart,double random) {
		acquireWriteLock();
		try {
			if (routingTable == null) {
				throw new NullPointerException();
			}

			// 1:, 2:, 4:
			unionFidgets(routingTable, maxFidgets, random, firstPart);

			if (commonLen > 0) {
				// 5:
				for (int i = 0; i < commonLen; i++) {
					try {
						// 6:
						Collection commonRef = union(getLevelVector(i), routingTable.getLevelVector(i));
						// 7: At the most REF_MAX routing references are randomly and independently chosen from commonRef in order to
						//    ensure that the network is uniformly connected, and routing references are random.
						if (i == lLen-1 && i == rLen-1)
							setLevel(i, commonRef);
						else
							setLevel(i, randomSelect(commonRef, maxRef, random, firstPart));
					} catch (IllegalArgumentException e) {
						// do nothing
					}
				}
				// 10:
				if ((lLen > commonLen) && (rLen > commonLen)) {
					// 11: Peers add mutual entries in their routing tables for level commonLen + 1
					addLevel(commonLen, routingTable.getLocalHost());
				}
			}
			//NEW: check all levels and replicas if their path has changed
			for (int i = 0; i < getLevelCount();  i++) {
				PGridHost[] refs = getLevel(i);
				for (int j = 0; j < refs.length; j++) {
					if (refs[j].getRevision() == 0)
						continue;
					String commonPrefix = Utils.commonPrefix(mLocalHost.getPath(), refs[j].getPath());
					int cl = commonPrefix.length();
					// if the host has another path than it should have
					if ((cl != i) || (refs[j].getPath().length() == i)) {
						lLen = mLocalHost.getPath().length() - cl;
						rLen = refs[j].getPath().length() - cl;
						// peers have incompatible paths
						if ((lLen > 0) && (rLen > 0)) {
							addLevel(cl, refs[j]);
						} else if ((lLen == 0) && (rLen == 0)) {
							addReplica(refs[j]);
						} else {
							removeLevel(refs[j]);
						}
					}
				}
			}
			PGridHost[] refs = getReplicas();
			for (int i = 0; i < refs.length; i++) {
				if (refs[i].getRevision() == 0)
					continue;
				if (!mLocalHost.getPath().equals(refs[i].getPath())) {
					String commonPrefix = Utils.commonPrefix(mLocalHost.getPath(), refs[i].getPath());
					int cl = commonPrefix.length();
					lLen = mLocalHost.getPath().length() - cl;
					rLen = refs[i].getPath().length() - cl;
					// peers have incompatible paths
					if ((lLen > 0) && (rLen > 0)) {
						addLevel(cl, refs[i]);
					} else {
						removeReplica(refs[i]);
					}
				}
			}

		} finally {
			releaseWriteLock();
		}

	}

	/**
	 * Adds the delivered host to the list of Fidget hosts.
	 *
	 * @param host the host.
	 */
	public void addFidget(PGridHost host) {
		acquireWriteLock();
		try {
			if (host == null) {
				throw new NullPointerException();
			}
			mHosts.put(host.getGUID().toString(), host);
			if (!mFidgets.contains(host))
				mFidgets.add(host);

		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Adds a new host at the delivered level.
	 *
	 * @param level the level of the path.
	 * @param host  the host.
	 */
	public void addLevel(int level, PGridHost host) {
		acquireWriteLock();
		try {
			if (host == null) {
				throw new NullPointerException();
			}
			remove(host);
			mHosts.put(host.getGUID().toString(), host);
			setLevels(level);
			Collection lev = (Collection)mLevels.get(level);
			lev.add(host);
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Adds new hosts at the the delivered level.
	 *
	 * @param level the level of the path.
	 * @param hosts the hosts.
	 */
	public void addLevel(int level, Collection hosts) {
		acquireWriteLock();
		try {
			if (hosts == null) {

				throw new NullPointerException();
			}
			setLevels(level);
			setLevel(level, union(getLevelVector(level), hosts));
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Adds the delivered host to the replicas.
	 *
	 * @param host the new host.
	 */
	public void addReplica(PGridHost host) {
		acquireWriteLock();
		try {
			if (host == null) {
				throw new NullPointerException();
			}
			removeLevel(host);
			mHosts.put(host.getGUID().toString(), host);
			mReplicas.add(host);
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Adds the delivered hosts to the replicas.
	 *
	 * @param hosts the new hosts.
	 */
	public void addReplicas(Collection hosts) {
		acquireWriteLock();
		try {
			if (hosts == null) {
				throw new NullPointerException();
			}
			setReplicas(union(getReplicaVector(), hosts));
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Removes all known fidgets, level hosts and replicas.
	 */
	public void clear() {
		acquireWriteLock();
		try{
			mFidgets.clear();
			mLevels.clear();
			mReplicas.clear();
		} finally {
			releaseWriteLock();
		}
	}
	/*
	public void clearAndDeleteRoutingTableFile(){
		this.clear();
		File rt = new File (Constants.DATA_DIR + PGridP2P.sharedInstance().propertyString(pgrid.Properties.ROUTING_TABLE));
		if (rt != null && rt.exists()){
			rt.delete();
			Constants.LOGGER.info("Routing Table cleared and file deleted: " + Constants.DATA_DIR + pgrid.Properties.ROUTING_TABLE);
		}
	}
	*/

	/**
	 * Removes all known replicas.
	 */
	public void clearReplicas() {
		acquireWriteLock();
		try {
			mReplicas.clear();
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Returns an array of all fidget hosts.
	 *
	 * @return an array of all fidget hosts.
	 */
	public PGridHost[] getFidgets() {
		acquireReadLock();

		PGridHost[] hosts;
		try {
			if (mFidgets.size() > 0) {
				hosts = new PGridHost[mFidgets.size()];
				System.arraycopy(mFidgets.toArray(), 0, hosts, 0, hosts.length);
			} else {
				hosts = new PGridHost[0];
			}
		} finally {
			releaseReadLock();
		}
		return hosts;
	}

	/**
	 * Returns a list of all fidget hosts.
	 *
	 * @return a list of all fidget hosts.
	 */
	public Collection getFidgetVector() {
		acquireReadLock();
		Collection fidget;
		try {
			fidget = new TreeSet(mFidgets);
		} finally {
			releaseReadLock();
		}
		return fidget;
	}

	/**
	 * Returns an array of all references.
	 *
	 * @return an array of all references.
	 */
	public PGridHost[][] getLevels() {
		acquireReadLock();
		PGridHost[][] refs = null;
		try {
			if (mLevels.size() == 0) {
				refs = new PGridHost[0][0];
			} else {
				refs = new PGridHost[mLevels.size()][];
				for (int i = 0; i < refs.length; i++)
					refs[i] = getLevel(i);
			}
		} finally {
			releaseReadLock();
		}
		return refs;
	}

	/**
	 * Returns the amount of levels of this Routing Table.
	 *
	 * @return the amount of levels.
	 */
	public int getLevelCount() {
		acquireReadLock();
		int s;
		try {
			s = mLevels.size();
		} finally {
			releaseReadLock();
		}
		return s;
	}

	/**
	 * Returns an array of the delivered level.
	 *
	 * @param level the level to return.
	 * @return an array of the level.
	 */
	public PGridHost[] getLevel(int level) {
		PGridHost[] hosts;
		acquireReadLock();
		try {
			if (level >= mLevels.size()) {
		//		Constants.LOGGER.log(Level.WARNING, "Illegal Argument in RoutingTable.getLevel() for level " + level, new Throwable());
				hosts = new PGridHost[0];
			} else if (((Collection)mLevels.get(level)).size() > 0) {
				hosts = new PGridHost[((Collection)mLevels.get(level)).size()];

				System.arraycopy(((Collection)mLevels.get(level)).toArray(), 0, hosts, 0, hosts.length);
			} else {
				hosts = new PGridHost[0];
			}

		} finally {
			releaseReadLock();
		}
		return hosts;
	}

	/**
	 * Returns a list of the delivered level.
	 *
	 * @param level the level to return.
	 * @return a list of the level.
	 * @throws IllegalArgumentException if an illegal level is given.
	 */
	public Collection getLevelVector(int level) {
		Collection hosts;
		acquireReadLock();
		try {
			if (level >= mLevels.size()) {
		//		Constants.LOGGER.log(Level.WARNING, "Illegal Argument in RoutingTable.getLevelVector() for level " + level, new Throwable());
				hosts = new Vector();                                
			}
			else hosts = new TreeSet((Collection)mLevels.get(level));
		} finally {
			releaseReadLock();
		}
		return hosts;
	}

	/**
	 * Returns the local host.
	 *
	 * @return the local host.
	 */
	public PGridHost getLocalHost() {
		acquireReadLock();
		PGridHost host;
		try {
			host = mLocalHost;
		} finally {
			releaseReadLock();
		}
		return host;
	}

	/**
	 * Sets the local host.
	 *
	 * @param host the local host.
	 */
	public void setLocalHost(PGridHost host) {
		acquireWriteLock();
		try {

			if (host == null) {
				throw new NullPointerException();
			}
			mLocalHost = host;
			if (host.getGUID() != null)
				mHosts.put(host.getGUID().toString(), host);


		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Returns an array of all replicas.
	 *
	 * @return an array of all replicas.
	 */
	public PGridHost[] getReplicas() {
		acquireReadLock();
		PGridHost[] hosts;
		try {
			if (mReplicas.size() > 0) {
				hosts = new PGridHost[mReplicas.size()];
				System.arraycopy(mReplicas.toArray(), 0, hosts, 0, hosts.length);
			} else {
				hosts = new PGridHost[0];
			}
		} finally {
			releaseReadLock();
		}
		return hosts;
	}

	/**
	 * Returns a list of all known references.
	 *
	 * @return a list of all known references.
	 */
	public List getAllReferences() {
		int size;
		ArrayList array = new ArrayList();

		acquireReadLock();
		try {
			size = mLevels.size();
			array = new ArrayList();
			Collection tmp = null;

			for (int i = 0; i < size; i++) {
				tmp = (Collection)mLevels.get(i);

				if (tmp != null)
					array.addAll(tmp);
			}
		} finally {
			releaseReadLock();
		}
		return array;
	}

	/**
	 * Returns a list of all replicas.
	 *
	 * @return a list of all replicas.
	 */
	public Collection<PGridHost> getReplicaVector() {
		Collection<PGridHost> hosts;

		acquireReadLock();
		try {
			hosts = new TreeSet<PGridHost>(mReplicas);
		} finally {
			releaseReadLock();
		}
		return hosts;
	}

	/**
	 * Removes the delivered host from the list of leveled hosts and replicas.
	 *
	 * @param host the host to remove.
	 */
	public void remove(PGridHost host) {
		acquireWriteLock();
		try {
			if (host == null) {
				throw new NullPointerException();
			}
			removeLevel(host);
			removeReplica(host);
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Removes a level.
	 *
	 * @param i the level index.
	 */
	public void removeLevel(int i) {
		acquireWriteLock();
		try {
			if (i != (mLevels.size() - 1)) {
				throw new IllegalArgumentException();
			}
			mLevels.remove(i);
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Removes the delivered host from the list of leveled hosts.
	 *
	 * @param host the host to remove.
	 */
	public void removeLevel(PGridHost host) {
		acquireWriteLock();
		try {
			if (host == null) {
				throw new NullPointerException();
			}
			for (int i = 0; i < mLevels.size(); i++) {
				Collection hosts = (Collection)mLevels.get(i);
				hosts.remove(host);
			}
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Removes the delivered host from the list of replicas.
	 *
	 * @param host the host to remove.
	 */
	public void removeReplica(PGridHost host) {
		acquireWriteLock();
		try {

			if (host == null) {
				throw new NullPointerException();
			}
			mReplicas.remove(host);
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Sets the fidget hosts with the given hosts.
	 *
	 * @param hosts the new hosts.
	 */
	public void setFidgets(Collection hosts) {
		acquireWriteLock();
		try {
			Collection tmpFidgets;
			if (hosts == null) {
				throw new NullPointerException();
			}
			tmpFidgets = Collections.synchronizedCollection(new LinkedHashSet(hosts));
			for (Iterator iter = hosts.iterator(); iter.hasNext();) {
				PGridHost element = (PGridHost)iter.next();
				mHosts.put(element.getGUID().toString(), element);
			}
			mFidgets = tmpFidgets;
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Sets the fidget hosts with the given hosts.
	 *
	 * @param hosts the new hosts.
	 */
	public void setFidgets(PGridHost[] hosts) {
		acquireWriteLock();
		try {
			if (hosts == null) {
				throw new NullPointerException();
			}
			mFidgets.clear();
			for (int i = 0; i < hosts.length; i++) {
				mHosts.put(hosts[i].getGUID().toString(), hosts[i]);
				addFidget(hosts[i]);
			}
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Sets the whole level with the new delivered hosts.
	 *
	 * @param level the level to set.
	 * @param hosts the new hosts.
	 * @throws IllegalArgumentException if an illegal level is given.
	 */
	public void setLevel(int level, Collection hosts) throws IllegalArgumentException {
		acquireWriteLock();
		try {
			if (hosts == null) {
				throw new NullPointerException();
			}
			for (Iterator it = hosts.iterator(); it.hasNext();) {
				PGridHost host = (PGridHost)it.next();
				mHosts.put(host.getGUID().toString(), host);
				remove(host);
			}
			setLevels(level);
			Collection coll = (Collection)mLevels.get(level);
			coll.clear();
			coll.addAll(hosts);
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Sets the amount of levels.
	 *
	 * @param levels the amount of levels.
	 */
	public void setLevels(int levels) {
		acquireWriteLock();
		try {
			if (levels < -1) {
				Constants.LOGGER.log(Level.WARNING, "Illegal Argument in RoutingTable.setLevels() for level " + levels, new Throwable());
			} else if (levels == -1) {
				mLevels.clear();
			} else if (levels >= mLocalHost.getPath().length()) {
				Constants.LOGGER.log(Level.WARNING, "Illegal Argument in RoutingTable.setLevels() for level " + levels + ". Current path: "+mLocalHost.getPath(), new Throwable());
			} else {
			for (int i = mLevels.size() - 1; i < levels; i++)
				mLevels.add(new TreeSet());
			}
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Sets the whole level with the new delivered hosts.
	 *
	 * @param level the level to set.
	 * @param hosts the new hosts.
	 */
	public void setLevel(int level, PGridHost[] hosts) {

		acquireWriteLock();
		try {

			if (hosts == null) {
				throw new NullPointerException();
			}
			setLevels(level);
			((Collection)mLevels.get(level)).clear();
			for (int i = 0; i < hosts.length; i++) {
				addLevel(level, hosts[i]);
				mHosts.put(hosts[i].getGUID().toString(), hosts[i]);
			}
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Sets the replicas with the given hosts.
	 *
	 * @param hosts the new hosts.
	 */
	public void setReplicas(Collection hosts) {
		acquireWriteLock();
		try {

			if (hosts == null) {
				throw new NullPointerException();
			}
			for (Iterator it = hosts.iterator(); it.hasNext();) {
				PGridHost host = (PGridHost)it.next();
				mHosts.put(host.getGUID().toString(), host);
				removeLevel(host);
			}
			mReplicas = Collections.synchronizedCollection(new TreeSet(hosts));
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Sets the replicas with the given hosts.
	 *
	 * @param hosts the new hosts.
	 */
	public void setReplicas(PGridHost[] hosts) {
		acquireWriteLock();
		try {

			if (hosts == null) {
				throw new NullPointerException();
			}
			mReplicas.clear();
			for (int i = 0; i < hosts.length; i++) {
				mHosts.put(hosts[i].getGUID().toString(), hosts[i]);
				addReplica(hosts[i]);
			}
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Performs a union of the delivered and this Routing Table.
	 *
	 * @param routingTable a Routing Table.
	 */
	public void union(RoutingTable routingTable) {
		acquireWriteLock();
		try {

			if (routingTable == null)
				throw new NullPointerException();
			unionFidgets(routingTable);
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Performs a union of the Fidget hosts of the delivered and this Routing Table.
	 *
	 * @param routingTable a Routing Table.
	 */
	public void unionFidgets(RoutingTable routingTable) {

		acquireWriteLock();
		try {

			if (routingTable == null)
				throw new NullPointerException();
			setFidgets(union(mFidgets, routingTable.mFidgets));
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Performs a union of the hosts at the delivered level of the delivered and this Routing Table.
	 *
	 * @param level        the level.
	 * @param routingTable a Routing Table.
	 */
	public void unionLevel(int level, RoutingTable routingTable) {
		acquireWriteLock();
		try {


			if (routingTable == null)
				throw new NullPointerException();
			setLevel(level, union(getLevelVector(level), routingTable.getLevelVector(level)));
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Performs a union of the Fidget hosts of the delivered and this Routing Table.
	 *
	 * @param routingTable  a Routing Table.
	 * @param maxFidgets	Maximum number of fidget to keep
	 * @param seed			Seed used to shuffle fidgets
	 * @param reverse		When splitted, which part of the fidget list to take
	 */
	public void unionFidgets(RoutingTable routingTable,
							 int maxFidgets, double seed, boolean reverse) {
		acquireWriteLock();
		try {


			if (routingTable == null)
				throw new NullPointerException();
			// 1:
			Collection commonFidgets;
			if (reverse){
				commonFidgets = union(mFidgets, routingTable.mFidgets);
			} else {
				commonFidgets = union(routingTable.mFidgets, mFidgets);
			}
			// 2:
			setFidgets(randomSelect(commonFidgets, maxFidgets, seed, reverse));
		} finally {
			releaseWriteLock();
		}
	}

	/**
	 * Returns the union of the two delivered lists of hosts.
	 *
	 * @param refs1 the first list of hosts.
	 * @param refs2 the second list of hosts.
	 * @return the union of the two lists.
	 */
	protected Collection union(Collection refs1, Collection refs2) {
		Collection set;
	    acquireWriteLock();
		try {


			if ((refs1 == null) || (refs2 == null)) {
				throw new NullPointerException();
			}

			if (refs1.isEmpty())
				set = Collections.synchronizedCollection(new LinkedHashSet());
			else
				set = Collections.synchronizedCollection(new LinkedHashSet(refs1));

			set.addAll(refs2);
		} finally {
			releaseWriteLock();
		}
		return set;
	}

	public Object clone() throws CloneNotSupportedException {
		Object clone;
		acquireReadLock();
		try {
			clone = super.clone();
			((RoutingTable)clone).mFidgets = Collections.synchronizedCollection(new TreeSet(mFidgets));
			((RoutingTable)clone).mHosts = (Hashtable) mHosts.clone();
			((RoutingTable)clone).mLevels = (Vector) mLevels.clone();
			((RoutingTable)clone).mReplicas = Collections.synchronizedCollection(new TreeSet(mReplicas));
			((RoutingTable)clone).mRTLock = new ReentrantReadWriteLock();
		} finally{
			releaseReadLock();
		}
		return clone;
	}

	/**
	 * Returns true if this routing table has at least 1 empty level
	 * @return true if this routing table has at least 1 empty level
	 */
	public boolean hasEmptyLevels() {
		int levels = getLevelCount();
		PGridHost[][] hosts = getLevels();
		
		for (int i=0;i<levels;i++) {
			if (hosts[i].length == 0) return true;
		}
		return false;
	}

}
