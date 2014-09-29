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

/**
 * This class provides useful functions used by the exchange algorithmus.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
class ExchangeAlgUtils {

	/**
	 * Creates a new utils class for the exchange algorithm.
	 *
	 */
	ExchangeAlgUtils() {
	}

	/**
	 * Compute M1 value for exchange algorithm.
	 *
	 * @param ld1 local data count.
	 * @param ld2 remote data count.
	 * @param est the replica estimation.
	 */
	static double computeM1(double ld1, double ld2, double est) {
		double M = 0;
		if (ld1 * ld2 == 0) {
			if ((ld1 == 0) && (ld2 != 0)) {
				M = 0.00657 + 187.73 / Math.pow(ld2, 4) - 63.765 / Math.pow(ld2, 3) + 13.255 / Math.pow(ld2, 2) - 0.2837 / ld2;
			}
			if ((ld2 == 0) && (ld1 != 0)) {
				M = 0.00657 + 187.73 / Math.pow(ld1, 4) - 63.765 / Math.pow(ld1, 3) + 13.255 / Math.pow(ld1, 2) - 0.2837 / ld1;
			}
			if ((ld1 == 0) && (ld2 == 0)) {
				M = 1;
			}
		} else {
			double ld12 = ld1 + ld2;
			double ratio = 0;
			if (ld1 < ld2) {
				ratio = ld1 / (ld1 + ld2);
			} else {
				ratio = ld2 / (ld1 + ld2);
				double tmp = ld1;
				ld1 = ld2;
				ld2 = tmp;
			}
			if ((ratio * est) < Constants.REPLICATION_FACTOR)
				ratio = Constants.REPLICATION_FACTOR / est;
			if (Constants.REPLICATION_FACTOR == 0)
				ratio = 1.0 / est;
			if (ratio < 0.05)
				ratio = 0.05;

			// naive approach
			/*
			try {
				M = 1 / (1 / ratio - 1);
			} catch (Exception e) {
				M = 0;
			}
			*/

			// our approach
			ld1 = ratio * ld1;
			ld2 = ld12 - ld1;

			if (ratio > (1 - Math.log(2))) {
				M = 1;
			} else {
				M = (136.94287 * Math.pow(ld1, 7) +
						372.74879 * Math.pow(ld1, 6) * ld2 +
						Math.pow(ld1, 3) * (-2441.16 - 1.51555 * ld2) * Math.pow(ld2, 3) + ld1 * (-6.66 - 0.23771 * ld2) * Math.pow(ld2, 5) + 0.00657 * Math.pow(ld2, 7) +
						Math.pow(ld1, 2) * Math.pow(ld2, 4) * (133.96 + 11.69077 * ld2) +
						Math.pow(ld1, 4) * Math.pow(ld2, 2) * (11266.16 + 59.77595 * ld2) +
						Math.pow(ld1, 5) * ld2 * (-24116.06 + 309.03247 * ld2)) / Math.pow(ld1 + ld2, 7);
			}
		}
		return M;
	}

	/**
	 * Compute M1 value for exchange algorithm.
	 *
	 * @param ld1 local data count.
	 * @param ld2 remote data count.
	 * @param est replication estimator.
	 */
	static double computeM2(double ld1, double ld2, double est) {
		double M = 0;
		if (ld1 * ld2 == 0) {
			if ((ld1 == 0) && (ld2 != 0)) {
				M = -1 + 2.5833 / Math.pow(ld2, 4) + 0.8316 / Math.pow(ld2, 3) + 1.695 / Math.pow(ld2, 2) + 2.586 / ld2;
			}
			if ((ld2 == 0) && (ld1 != 0)) {
				M = -1 + 2.5833 / Math.pow(ld1, 4) - 0.8316 / Math.pow(ld1, 3) + 1.695 / Math.pow(ld1, 2) - 2.586 / ld1;
			}
			if ((ld1 == 0) && (ld2 == 0)) {
				M = 1;
			}
		} else {
			double ld12 = ld1 + ld2;
			double ratio = 0;
			if (ld1 < ld2) {
				ratio = ld1 / (ld1 + ld2);
			} else {
				ratio = ld2 / (ld1 + ld2);
				double tmp = ld1;
				ld1 = ld2;
				ld2 = tmp;
			}
			if ((ratio * est) < Constants.REPLICATION_FACTOR) {
				if (Constants.REPLICATION_FACTOR == 0) ratio = Math.min(0.5, 1.0 / est);
				else ratio = Math.min(0.5, Constants.REPLICATION_FACTOR / est);
			}

			// naive approach
			// M = 0;

			// our approach
			ld1 = ratio * ld12;
			ld2 = ld12 - ld1;

			if (ratio < (1 - Math.log(2))) {
				M = 0;
			} else {
				M = (6.6959 * Math.pow(ld1, 7) +
						28.0673 * Math.pow(ld1, 6) * ld2 +
						Math.pow(ld1, 2) * (-10.3905 - 3.789 * ld2) * Math.pow(ld2, 4) +
						ld1 * (-1.635 - 4.414 * ld2) * Math.pow(ld2, 5) -
						Math.pow(ld2, 7) +
						Math.pow(ld1, 3) * Math.pow(ld2, 3) * (-29.3952 + 13.0966 * ld2) +
						Math.pow(ld1, 4) * Math.pow(ld2, 2) * (-35.2584 + 39.5797 * ld2) +
						Math.pow(ld1, 5) * ld2 * (-59.5137 + 47.4795 * ld2)) / Math.pow(ld1 + ld2, 7);
			}
		}
		return M;
	}

	static float estimateN(float r, float r1, float r2) {
		if (r == 0)
			return (float) 0;
		if ((r1 * r2) == 0)
			return (float) 0;
		return (float)(r1 * r2 / r);
	}

	/**
	 * Returns the path extension for this host.
	 *
	 * @param locally flag if the exchange was started by the local host or
	 *                remote.
	 * @param random the random variable.
	 * @return the path extension.
	 */
	static String pathExtension(boolean locally, double random) {
		if (locally) {
			if (random < 0.5) {
				return "0";
			} else {
				return "1";
			}
		} else {
			if (random < 0.5) {
				return "1";
			} else {
				return "0";
			}
		}
	}

}