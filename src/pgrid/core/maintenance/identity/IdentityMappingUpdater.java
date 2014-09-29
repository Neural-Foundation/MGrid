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

package pgrid.core.maintenance.identity;

import pgrid.Constants;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.PGridHost;
import pgrid.network.MessageManager;
import pgrid.util.logging.LogFormatter;

import java.util.Vector;
import java.util.logging.Logger;


/**
 * This class is in charge of doing the identity mapping
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */
public class IdentityMappingUpdater {
	/**
	 * The time to wait for lookup messages.
	 */
	private static final int REPLY_TIMEOUT = 1000 * 60; // 60s.

	/**
	 * The PGridP2P.Searcher logger.
	 */
	public static final Logger LOGGER = Logger.getLogger("PGridP2P.Id-ip_updater");

	/**
	 * The logging file.
	 */
	public static final String LOG_FILE = "Id-ip_updater.log";

	/**
	 * Host to where the lookup peer message and update message have been send
	 */
	protected Vector mLookupReply = new Vector();

	/**
	 * The requesting host.
	 */
	private PGridHost mHost = null;

	/**
	 * The Message Manager.
	 */
	private MessageManager mMsgMgr = MessageManager.sharedInstance();

	/**
	 * The PGridP2P facility.
	 */
	private PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 * Minimum quorum
	 */
	protected int mQuorum;

	/**
	 * The updated itentity item
	 */
	protected XMLIdentityIndexEntry mIdentity;

	/**
	 * The amount of started remote update.
	 */
	private int mRemoteUpdates = 0;

	/**
	 * The maximum number of tries
	 */
	private int mMaxAttemptPerThread = 3;

	static {
		LogFormatter formatter = new LogFormatter();
		formatter.setDateFormat("HH:mm:ss");
		formatter.setFormatPattern(LogFormatter.DATE + ": " + LogFormatter.MESSAGE + LogFormatter.NEW_LINE + LogFormatter.THROWABLE);
		Constants.initChildLogger(LOGGER, formatter, LOG_FILE);
	}

	/**
	 * Creates a new updater for a locally update.
	 *
	 * @param dataItem the mMapping.
	 */
	public IdentityMappingUpdater(XMLIdentityIndexEntry dataItem) {
		mIdentity = dataItem;

	}

	/**
	 * Perform a remote update
	 *
	 * @return true if the update met the quorum minumun, false otherwise
	 */
	public boolean remoteUpdate() {
		return false;

	}

}