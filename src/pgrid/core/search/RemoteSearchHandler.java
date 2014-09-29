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

import p2p.basic.GUID;
import p2p.index.events.NoSuchTypeException;
import p2p.index.events.SearchListener;
import pgrid.network.MessageManager;
import pgrid.network.RemoteMessageHandler;
import pgrid.network.protocol.*;
import pgrid.AbstractQuery;
import pgrid.Constants;
import pgrid.QueryReply;
import pgrid.Statistics;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.core.index.IndexManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;

/**
 * This class processes search requests.
 *
 * NOTE: The name is missleading since this class will also take care of local search.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class RemoteSearchHandler implements SearchListener, RemoteMessageHandler {

	/**
	 * The Message Manager.
	 */
	private MessageManager mMsgMng = null;

	private Hashtable<GUID, RemoteSearchHandler.QueryData> mQueryRefs = new Hashtable<GUID, RemoteSearchHandler.QueryData>();

	/**
	 * Creates a new RemoteSearchHandler.
	 */
	public RemoteSearchHandler() {
		mMsgMng = MessageManager.sharedInstance();
	}

	/**
	 * Invoked when a new search result is available
	 *
	 * @param guid    the GUID of the original query
	 * @param results a Collection of DataItems matching the original query
	 */
	public void newSearchResult(GUID guid, Collection results) {
		// get the requesting host
		RemoteSearchHandler.QueryData data = mQueryRefs.get(guid);
		if (data == null) {
	
			Constants.LOGGER.config("no query '" + guid.toString() + "' found for search results!");
		} else {
			// reply the results to the requesting host
			Constants.LOGGER.fine("return "+results.size()+" results for query '" + guid.toString() + "' to host: "+data.mQuery.getRequestingHost().toHostString()+".");
			QueryReplyMessage msg = new QueryReplyMessage(guid, QueryReply.TYPE_OK, results.size());

			// statistic
			if (PGridP2P.sharedInstance().isInTestMode())
				PGridP2P.sharedInstance().getStatistics().incMessageStat(Statistics.messageStats.found, msg.getHeader().getDesc());

			mMsgMng.reply(data.mQuery.getRequestingHost(), msg, data.mMsg, null, null);
			mQueryRefs.remove(guid);
		}
	}
	
	
	/**
	 * Invoked when a new search result is available
	 *
	 * @param guid    the GUID of the original query
	 * @param results a Collection of DataItems matching the original query
	 */
	public void newSearchResult(GUID guid, int hits) {
		// get the requesting host

		
		RemoteSearchHandler.QueryData data = mQueryRefs.get(guid);
		if (data == null) {
			Constants.LOGGER.config("no query '" + guid.toString() + "' found for search results!");
		} else {
			// reply the results to the requesting host
			Constants.LOGGER.fine("return "+hits+" results for query '" + guid.toString() + "' to host: "+data.mQuery.getRequestingHost().toHostString()+".");
			QueryReplyMessage msg = new QueryReplyMessage(guid, QueryReply.TYPE_OK, hits);
			mMsgMng.reply(data.mQuery.getRequestingHost(), msg, data.mMsg, null, null);
			mQueryRefs.remove(guid);
		}
	}

	/**
	 * Invoked when a search resulted in no results.
	 *
	 * @param guid the GUID of the original query
	 */
/*	public void noResultsFound(GUID guid) {
		// get the requesting host
		RemoteSearchHandler.QueryData data = mQueryRefs.get(guid);
		if (data == null) {
			Constants.LOGGER.config("no query '" + guid.toString() + "' found for search results!");
		} else if (!data.mBroadcasted) {
			// reply with not found message
			Constants.LOGGER.fine("return no results for query '" + guid.toString() + "'.");
			QueryReplyMessage msg = new QueryReplyMessage(guid, QueryReply.TYPE_NOT_FOUND, null);
			// statistic
			if (PGridP2P.sharedInstance().isInTestMode())
				PGridP2P.sharedInstance().getStatistics().incMessageStat(Statistics.messageStats.notFound, msg.getHeader().getDesc());

			msg.getHeader().setHops(data.mMsg.getHeader().getHops());
			msg.getHeader().setReferences(data.mMsg.getHeader().getReferences());
			mMsgMng.reply(data.mQuery.getRequestingHost(), msg, data.mMsg, null, null);
			mQueryRefs.remove(guid);
		}
	}*/

	/**
	 * Invoked when a search resulted in no results.
	 *
	 * @param guid the GUID of the original query
	 */
	public void noResultsFound(GUID guid) {
		// get the requesting host
		RemoteSearchHandler.QueryData data = mQueryRefs.get(guid);
		if (data == null) {
			Constants.LOGGER.config("no query '" + guid.toString() + "' found for search results!");
		} else if (!data.mBroadcasted) {
			// reply with not found message
			Constants.LOGGER.fine("return no results for query '" + guid.toString() + "'.");
			QueryReplyMessage msg = new QueryReplyMessage(guid, QueryReply.TYPE_NOT_FOUND, 0);
			// statistic
			if (PGridP2P.sharedInstance().isInTestMode())
				PGridP2P.sharedInstance().getStatistics().incMessageStat(Statistics.messageStats.notFound, msg.getHeader().getDesc());

			msg.getHeader().setHops(data.mMsg.getHeader().getHops());
			msg.getHeader().setReferences(data.mMsg.getHeader().getReferences());
			mMsgMng.reply(data.mQuery.getRequestingHost(), msg, data.mMsg, null, null);
			mQueryRefs.remove(guid);
		}
	}
	/**
	 * Invoked when a search failed.
	 *
	 * @param guid the GUID of the original query
	 *//*
	public void searchFailed(GUID guid) {
		// get the requesting host
		RemoteSearchHandler.QueryData data = mQueryRefs.get(guid);
		if (data == null) {
			Constants.LOGGER.config("no query '" + guid.toString() + "' found for search results!");
		} else if (!data.mBroadcasted) {
			// reply with bad request message
			Constants.LOGGER.fine("return search failed for query '" + guid.toString() + "'.");

			QueryReplyMessage msg = new QueryReplyMessage(guid, QueryReply.TYPE_BAD_REQUEST, null);
			msg.getHeader().setHops(data.mMsg.getHeader().getHops());
			msg.getHeader().setReferences(data.mMsg.getHeader().getReferences());
			mMsgMng.reply(data.mQuery.getRequestingHost(), msg, data.mMsg, null, null);
			mQueryRefs.remove(guid);
		}
	}
	*/
	/**
	 * Invoked when a search failed.
	 *
	 * @param guid the GUID of the original query
	 */
	public void searchFailed(GUID guid) {
		// get the requesting host
		RemoteSearchHandler.QueryData data = mQueryRefs.get(guid);
		if (data == null) {
			Constants.LOGGER.config("no query '" + guid.toString() + "' found for search results!");
		} else if (!data.mBroadcasted) {
			// reply with bad request message
			Constants.LOGGER.fine("return search failed for query '" + guid.toString() + "'.");

			QueryReplyMessage msg = new QueryReplyMessage(guid, QueryReply.TYPE_BAD_REQUEST, 0);
			msg.getHeader().setHops(data.mMsg.getHeader().getHops());
			msg.getHeader().setReferences(data.mMsg.getHeader().getReferences());
			mMsgMng.reply(data.mQuery.getRequestingHost(), msg, data.mMsg, null, null);
			mQueryRefs.remove(guid);
		}
	}

	/**
	 * Invoked when a search finished.
	 *
	 * @param guid the GUID of the original query
	 */
	public void searchFinished(GUID guid) {
		// remove the query from the list of treated queries
		mQueryRefs.remove(guid);
	}

	/**
	 * Invoked when a search started (reached a responsible peer).
	 *
	 * @param guid the GUID of the original query
	 * @param message the explanation message.
	 */
	public void searchStarted(GUID guid, String message) {
		// do nothing
	}

	/**
	 * This method is called when a new message arrives.
	 *
	 * @param msg
	 * @param broadcasted
	 * @throws IOException 
	 * @throws NoSuchTypeException 
	 */
	public void newRemoteMessage(PGridMessage msg, boolean broadcasted) throws NoSuchTypeException {
		RemoteSearchHandler.QueryData data=null;

		if(msg instanceof QueryMessage) {
			data = new RemoteSearchHandler.QueryData(((QueryMessage)msg).getQuery(), msg, broadcasted);

		} else if(msg instanceof RangeQueryMessage) {
			data = new RemoteSearchHandler.QueryData(((RangeQueryMessage)msg).getQuery(), msg, broadcasted);
		} else {
			Constants.LOGGER.warning("Unknown query type for message '" + msg.getGUID() + "'!");
			return;
		}

		mQueryRefs.put(data.mQuery.getGUID(), data);
		IndexManager.getInstance().matchLocalItems(data.mQuery, this);
	}

	/**
	 * This method is called when the router is unable to route further a message. The routing behavior is left to
	 * the application layer.
	 *
	 * @param msg
	 *//*
	public void failedToRoute(PGridMessage msg) {
		//sent a query reply with a fail to route
		Constants.LOGGER.fine("Cannot route query '" + msg.getGUID().toString() + "' further. Send a query reply to the requesting host.");

		QueryReplyMessage reply = new QueryReplyMessage(msg.getGUID(), QueryReply.TYPE_BAD_REQUEST, null);
		msg.getHeader().setReferences(msg.getHeader().getReferences());
		msg.getHeader().setHops(msg.getHeader().getHops());
		if(msg instanceof QueryMessage) {
			mMsgMng.reply(((QueryMessage)msg).getQuery().getRequestingHost(), reply, msg, null, null);

		} else if(msg instanceof RangeQueryMessage) {
			mMsgMng.reply(((RangeQueryMessage)msg).getQuery().getRequestingHost(), reply, msg, null, null);
		} else {
			Constants.LOGGER.warning("Unknown query type for message '" + msg.getGUID() + "'!");
			return;
		}

	}*/
	
	/**
	 * This method is called when the router is unable to route further a message. The routing behavior is left to
	 * the application layer.
	 *
	 * @param msg
	 */
	public void failedToRoute(PGridMessage msg) {
		//sent a query reply with a fail to route
		Constants.LOGGER.fine("Cannot route query '" + msg.getGUID().toString() + "' further. Send a query reply to the requesting host.");

		QueryReplyMessage reply = new QueryReplyMessage(msg.getGUID(), QueryReply.TYPE_BAD_REQUEST, 0);
		msg.getHeader().setReferences(msg.getHeader().getReferences());
		msg.getHeader().setHops(msg.getHeader().getHops());
		if(msg instanceof QueryMessage) {
			mMsgMng.reply(((QueryMessage)msg).getQuery().getRequestingHost(), reply, msg, null, null);

		} else if(msg instanceof RangeQueryMessage) {
			mMsgMng.reply(((RangeQueryMessage)msg).getQuery().getRequestingHost(), reply, msg, null, null);
		} else {
			Constants.LOGGER.warning("Unknown query type for message '" + msg.getGUID() + "'!");
			return;
		}

	}

	class QueryData {
		public AbstractQuery mQuery;
		public PGridMessage mMsg;
		public boolean mBroadcasted;

		public QueryData(AbstractQuery mQuery, PGridMessage mMsg, boolean broadcasted) {
			this.mQuery = mQuery;
			this.mMsg = mMsg;
			this.mBroadcasted = broadcasted;
		}
	}

	

}