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

import pgrid.Exchange;

/**
 * This class represent a request for an exchange
 */
class ExchangeRequest {

	private Exchange mExchange = null;

	private long mStartTime = 0;

	private boolean mSplitted = false;

	public ExchangeRequest(Exchange exchange, boolean splitted) {
		mExchange = exchange;
		mStartTime = System.currentTimeMillis();
		mSplitted = splitted;
	}

	public Exchange getExchange() {
		return mExchange;
	}

	public long getStartTime() {
		return mStartTime;
	}

	public boolean hasSeparatedData() {
		return mSplitted;
	}

}
