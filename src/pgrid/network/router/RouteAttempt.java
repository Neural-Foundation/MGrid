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
package pgrid.network.router;

import p2p.basic.Message;

import java.util.Collection;
import java.util.Iterator;

import pgrid.interfaces.basic.PGridP2P;

/**
 * Created by IntelliJ IDEA.
 * User: pgrid.helper
 * Date: 15.06.2005
 * Time: 15:26:24
 * To change this template use File | Settings | File Templates.
 */
class RouteAttempt implements Runnable{

	private Collection mCollection = null;

	private Iterator mIterator = null;

	private Message mMessage = null;

	private long mStartTime = 0;

	public RouteAttempt(Message msg, Collection col, Iterator it) {
		mMessage = msg;
		mCollection = col;
		mIterator = it;
		mStartTime = System.currentTimeMillis();
	}

	public Collection getCollection() {
		return mCollection;
	}

	public Iterator getIterator() {
		return mIterator;
	}

	public Message getMessage() {
		return mMessage;
	}

	public long getSentTime() {return mStartTime;}

	public void resetSentTime() {mStartTime = System.currentTimeMillis();}

	public void run() {
		PGridP2P.sharedInstance().getRouter().route(this);
	}
}
