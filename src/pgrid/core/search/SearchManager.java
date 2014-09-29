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

package pgrid.core.search;

import p2p.basic.events.NoRouteToKeyException;
import p2p.basic.GUID;
import p2p.index.events.NoSuchTypeException;
import p2p.index.events.SearchListener;
import pgrid.*;
import pgrid.network.router.MessageWaiter;
import pgrid.network.protocol.PGridMessage;
import pgrid.network.protocol.QueryReplyMessage;
import pgrid.network.protocol.QueryMessage;
import pgrid.network.protocol.RangeQueryMessage;
import pgrid.network.MessageManager;
import pgrid.interfaces.basic.PGridP2P;
import test.demo.KnnQuery;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

/**
 * This class maintains a list of queries and of those listeners that are
 * interested in the results of these queries.
 *
 * @author <a href="mailto:Tim van Pelt <tim@vanpelt.com>">Tim van Pelt</a> &amp;
 *         <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class SearchManager extends pgrid.util.WorkerThread implements MessageWaiter {

	/**
	 * The reference to the only instance of this class (Singleton
	 * pattern). This differs from the C++ standard implementation by Gamma
	 * et.al. since Java ensures the order of static initialization at runtime.
	 *
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	private static final SearchManager SHARED_INSTANCE = new SearchManager();

	/**
	 * The data item manager.
	 */
	private MessageManager mMsgMng = MessageManager.sharedInstance();

	/**
	 * The P-Grid P2P facility.
	 */
	private PGridP2P mPGridP2P = null;
	/**
	 * The search requests.
	 */
	private final Vector mRequests = new Vector();

	/**
	 * Search listener comparator
	 */
	private SearchListenerComparator mSLComparator = new SearchListenerComparator();

	/**
	 * List of search listner
	 */
	private final Hashtable<GUID, Collection<SearchListener>> mListeners = new Hashtable<GUID, Collection<SearchListener>>();

	private Thread mThread = null;

	/**
	 * The constructor must be protected to ensure that only subclasses can
	 * call it and that only one instance can ever get created. A client that
	 * tries to instantiate the search manager directly will get an error at
	 * compile-time.
	 */
	protected SearchManager() {
	}

	/**
	 * This creates the only instance of this class. This differs from the C++ standard implementation by Gamma et.al.
	 * since Java ensures the order of static initialization at runtime.
	 *
	 * @return the shared instance of this class.
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	public static SearchManager sharedInstance() {
		return SHARED_INSTANCE;
	}

	protected void handleError(Throwable t) {
		if (t instanceof InterruptedException) {
			Constants.LOGGER.finer("Searcher manager interupted.");
		} else {
			Constants.LOGGER.log(Level.WARNING, "Error in Search thread", t);
			t.printStackTrace();
		}
	}

	/**
	 * Initializes the search manager.
	 */
	public void init() {
		mPGridP2P = PGridP2P.sharedInstance();
	}

	protected boolean isCondition() {
		return !mRequests.isEmpty();
	}

	protected void prepareWorker() throws Exception {
		mThread = Thread.currentThread();
		Constants.LOGGER.config("Search thread prepared.");
	}

	protected void releaseWorker() throws Exception {
		Constants.LOGGER.config("Search thread released.");
	}

	/**
	 * Registers a search listener for a given query.
	 *
	 * This method allows to register additional listeners for a query. Listeners are
	 * usually provided with a query by invoking the search method.
	 *
	 * @param guid guid of the query.
	 * @param listener the query search listener.
	 */
	public void registerListener(GUID guid, SearchListener listener) {
		if (listener == null)
			throw new NullPointerException();

		Collection<SearchListener> listeners = mListeners.get(guid);
		if (listeners == null) {
			listeners = new TreeSet<SearchListener>(mSLComparator);
			mListeners.put(guid, listeners);
		}

		MessageManager.sharedInstance().registerWaiter(guid, this);

		listeners.add(listener);
	}

	/**
	 * Search the network for matching items. Implemented as
	 * an asynchronous operation, because search might take
	 * some time. Callback is notified for each new result. <br>
	 *
	 * This method register the given listener for the query GUID. If you want to
	 * trigger the listener for query reference GUID, you should explicitly register
	 * the listener for that.
	 * <code>
	 * for (GUID guid: query.getQueryReferences()) {
	 *		registerListener(guid, listener);
	 *	}
	 * </code>
	 *
	 * @param query    the query used to specify the search
	 * @param listener an object to notify when results arrive
	 * @throws p2p.index.events.NoSuchTypeException
	 *          if the provided Type is unknown.
	 * @throws p2p.basic.events.NoRouteToKeyException
	 *          if the query cannot be routed to a responsible peer.
	 */
	public void search(p2p.index.Query query, SearchListener listener) throws NoSuchTypeException, NoRouteToKeyException {
		// register listener
		search(query, listener, query.getGUID());
	}

	/**
	 * Search the network for matching items. Implemented as
	 * an asynchronous operation, because search might take
	 * some time. Callback is notified for each new result. <br>
	 *
	 * This method register the given listener for the given GUID. Use this method if you are only interested in binding
	 * a listener to a query reference GUID and not the query GUID itself. 
	 *
	 * @param query    the query used to specify the search
	 * @param listener an object to notify when results arrive
	 * @param guid	the guid used for registering the listener. If null, no listener is registered
	 * @throws p2p.index.events.NoSuchTypeException
	 *          if the provided Type is unknown.
	 * @throws p2p.basic.events.NoRouteToKeyException
	 *          if the query cannot be routed to a responsible peer.
	 */
	public void search(p2p.index.Query query, SearchListener listener, GUID guid) throws NoSuchTypeException, NoRouteToKeyException {
		// register listener
		if (guid != null)
	
			registerListener(guid, listener);

		mRequests.add(query);
		broadcast();
	}

	protected void work() throws Exception {
		Iterator requests = null;
		synchronized (mRequests) {
			requests = ((Vector) mRequests.clone()).iterator();
			mRequests.clear();
		}
		p2p.index.Query q;
		
		while (requests.hasNext()) {
			q = (p2p.index.Query) requests.next();
			PGridHost initiator = (((pgrid.AbstractQuery)q).getRequestingHost() == null) ? mPGridP2P.getLocalHost() : ((pgrid.AbstractQuery)q).getRequestingHost();
		
			
			if (q instanceof pgrid.Query || (q instanceof pgrid.RangeQuery && (q.getLowerBound().equals(q.getHigherBound()) && q.getKeyRange().getMin().equals(q.getKeyRange().getMax())))) {
				
				QueryMessage msg = new QueryMessage(q.getGUID(),q.getType(), q.getLowerBound(), q.getKeyRange().getMin(),0,0,initiator,0,null);
			
				if (!q.getQueryReferences().isEmpty())
	
					msg.getHeader().setReferences(new Vector<GUID>(q.getQueryReferences()));
				mMsgMng.route(q.getKeyRange().getMin(),msg,null,this);
			}
			else if (q instanceof pgrid.RangeQuery) {
	
				RangeQueryMessage msg = new RangeQueryMessage(q.getGUID(),q.getType(), 0, 
						RangeQuery.SHOWER_ALGORITHM, q.getLowerBound(), q.getHigherBound(), 
						q.getOrigxMin(), q.getOrigxMax(), q.getOrigyMin(), q.getOrigyMax(), 
						q.getKeyRange(), 0, "",0, initiator, q.getHits());
				if (!q.getQueryReferences().isEmpty())
					msg.getHeader().setReferences(new Vector<GUID>(q.getQueryReferences()));
				mMsgMng.route(q.getKeyRange(),msg,null,this);
				
				
			}
		}
	}

	/**
	 * Shutdown
	 */
	public void shutdown() {
		if (mThread != null)
			mThread.interrupt();
	}

	/**
	 * This message is called when a new query reply is received
	 * @param msg
	 * @param guid
	 */
	public void newMessage(PGridMessage msg, GUID guid) {
	
		
		if (msg instanceof QueryReplyMessage) {
			QueryReplyMessage query = (QueryReplyMessage)msg;
			Collection<SearchListener> listeners = mListeners.get(guid);
			if (listeners == null) {
				Constants.LOGGER.finer("No search listener bind to [" + guid.toString() + "].");				
			}

			Constants.LOGGER.fine("Response for remote search (" + guid.toString() + ") received with " + query.getQueryReply().getmHits()+ " hit(s).");
			if (query.getQueryReply().getType() == QueryReply.TYPE_OK) {
				// local request => add results
			
				Constants.LOGGER.fine("" + query.getQueryReply().getmHits() + " hit(s) for the search (" + guid.toString() + ") returned from host " + query.getHeader().getHost().toHostString() + ".");
				for (SearchListener listener: listeners) {
				//	listener.newSearchResult(guid, query.getQueryReply().getResultSet());
					if (query.getQueryReply().getmHits() == 0l) {
						listener.newSearchResult(guid, 0);
					} else 
					listener.newSearchResult(guid, query.getQueryReply().getmHits());
					if (msg instanceof QueryReplyMessage) {
						listener.searchFinished(guid);
					}
				}
			} else if (query.getQueryReply().getType() == QueryReply.TYPE_BAD_REQUEST) {
	
				if (PGridP2P.sharedInstance().isInTestMode())
					PGridP2P.sharedInstance().getStatistics().incMessageStat(Statistics.messageStats.badRequest, msg.getHeader().getDesc());
				if (msg instanceof QueryReplyMessage) {
					for (SearchListener listener: listeners) {
						listener.searchFailed(guid);
					}
				}
			} else if (query.getQueryReply().getType() == QueryReply.TYPE_NOT_FOUND) {
		
				Constants.LOGGER.fine("Return NOT_FOUND for the search (" + guid.toString() + ") returned from host " + query.getHeader().getHost().toHostString() + ".");
				if (msg instanceof QueryReplyMessage) {
						for (SearchListener listener: listeners) {
						listener.noResultsFound(guid);
						listener.searchFinished(guid);
					}
				}
			}
		} // end of first if 
	}

	protected class SearchListenerComparator implements Comparator {

		public int compare(Object o, Object o1) {
			if (o.hashCode() == o1.hashCode()) return 0;
			return (o.hashCode() > o1.hashCode()?1:-1);
		}
	}

}