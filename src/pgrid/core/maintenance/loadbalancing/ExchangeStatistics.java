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

package pgrid.core.maintenance.loadbalancing;

import pgrid.Constants;
import pgrid.interfaces.basic.PGridP2P;

import java.util.Vector;

/**
 * This class stores statistical information about performed exchanges used for load balancing.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
class ExchangeStatistics {

	/**
	 * The comp. path count.
	 */
	private double mCompPath[] = new double[0];

	/**
	 * The level count.
	 */
	private int mCount[] = new int[0];

	/**
	 * The same path count.
	 */
	private double mSamePath[] = new double[0];

	/**
	 * The comp. path count.
	 */
	private Vector<Double> mOldCompPath = new Vector<Double>();

	/**
	 * The level count.
	 */
	private Vector<Integer> mOldCount = new Vector<Integer>();

	/**
	 * The same path count.
	 */
	private Vector<Double> mOldSamePath = new Vector<Double>();


	/**
	 * Creates a new statistics object.
	 */
	ExchangeStatistics() {
	}

	/**
	 * Returns the count for a level.
	 *
	 * @param level the level.
	 * @return the count.
	 */
	double compPath(int level) {
		ensureLength(level - 1);
		return mCompPath[level - 1];
	}

	/**
	 * Returns the count for a level.
	 *
	 * @param level the level.
	 * @return the count.
	 */
	int count(int level) {
		ensureLength(level - 1);
		return mCount[level - 1];
	}

	/**
	 * Ensures, that the arrays are long enough.
	 *
	 * @param level the minimal length of the arrays.
	 */
	private void ensureLength(int level) {
		if (mCompPath.length <= level) {
			double[] tmp = new double[level + 1];
			System.arraycopy(mCompPath, 0, tmp, 0, mCompPath.length);
			mCompPath = tmp;
			mOldCompPath.addElement(0.0);
		}
		if (mCount.length <= level) {
			int[] tmp = new int[level + 1];
			System.arraycopy(mCount, 0, tmp, 0, mCount.length);
			mCount = tmp;
			mOldCount.addElement(0);
		}
		if (mSamePath.length <= level) {
			double[] tmp = new double[level + 1];
			System.arraycopy(mSamePath, 0, tmp, 0, mSamePath.length);
			mSamePath = tmp;
			mOldSamePath.addElement(0.0);
		}
	}

	/**
	 * Increases the count for comp. path at a level.
	 *
	 * @param level   the level.
	 * @param pathLen the path length.
	 */
	void incCompPath(int level, int pathLen) {
		ensureLength(level - 1);
		mCompPath[level - 1] += (1.0 / Math.pow(2.0, pathLen - (level - 1) - 1));
		if (PGridP2P.sharedInstance().isInDebugMode())
			Constants.LOGGER.finest("Prob for level "+(level-1)+" at comp path = "+mCompPath[level - 1]+".");
	}

	/**
	 * Increases the count for a level.
	 *
	 * @param level the level.
	 */
	void incCount(int level) {
		ensureLength(level - 1);
		mCount[level - 1]++;
	}

	/**
	 * Increases the count for same path at a level.
	 *
	 * @param level   the level.
	 * @param pathLen the path length.
	 */
	void incSamePath(int level, int pathLen) {
		ensureLength(level - 1);
		mSamePath[level - 1] += (1.0 / Math.pow(2.0, pathLen - (level - 1) - 1));
		if (PGridP2P.sharedInstance().isInDebugMode())
			Constants.LOGGER.finest("Prob for level "+(level-1)+" at same path = "+mSamePath[level - 1]+".");
	}

	/**
	 * Resets the statistics.
	 */
	void reset() {
		mCompPath = new double[0];
		mCount = new int[0];
		mSamePath = new double[0];

		mOldCompPath.clear();
		mOldCount.clear();
		mOldSamePath.clear();
	}

	/**
	 * Save statisics for a particular level.
	 */
	void checkpoint(int level) {
		if (level > mOldCompPath.size()) {
			mOldCompPath.setSize(level);
			mOldCount.setSize(level);
			mOldSamePath.setSize(level);
		}
		mOldCompPath.insertElementAt(mCompPath[level], level);
		mOldCount.insertElementAt(mCount[level], level);
		mOldSamePath.insertElementAt(mSamePath[level], level);
	}

	/**
	 * Substract checkpoint value to the statistics
	 * @param level
	 */
	void substractCheckpoint(int level) {
		if (mOldCompPath.elementAt(level) != null)
			mCompPath[level] = mCompPath[level]-mOldCompPath.elementAt(level);
		if (mOldCount.elementAt(level) != null)
			mCount[level] = mCount[level]-mOldCount.elementAt(level);
		if (mOldSamePath.elementAt(level) != null)
			mSamePath[level] = mSamePath[level]-mOldSamePath.elementAt(level);
	}

	/**
	 * Returns the count for a level.
	 *
	 * @param level the level.
	 * @return the count.
	 */
	double samePath(int level) {
		ensureLength(level - 1);
		return mSamePath[level - 1];
	}

}
