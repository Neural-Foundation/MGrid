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
package pgrid.network;

import java.util.ArrayList;

import p2p.basic.GUID;
import p2p.index.events.DownloadListener;
import pgrid.Constants;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.protocol.GetFile;
import pgrid.network.protocol.GetFileReply;

public class DownloadManager {

	private ArrayList<DownloadListener> listeners = new ArrayList<DownloadListener>();
	private PGridP2P mPGridP2P = null;
	
	public DownloadManager(PGridP2P p2p){
		mPGridP2P = p2p;
	}
	
	/**
	 * Register an object to be notified of new messages.
	 *
	 * @param listener the DownloadListener implementation to register
	 */
	public void addDownloadListener(DownloadListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove registration of a current listener of new messages.
	 *
	 * @param listener the DownloadListener implementation to unregister
	 */
	public void removeDownloadListener(DownloadListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Handles a new received GetFileReply message.
	 *
	 * @param message the GetFileReply message.
	 * @param origin the originating host.
	 */
	public void newGetFileReplyMessage(GetFileReply message) {
		Constants.LOGGER.fine("GetFileReply message received from " + message.getHeader().getHost());
		for (DownloadListener l : listeners) l.downloadFinished(message.getGUID());
		
	}
	
	public void newGetFileMessage(GetFile message){
		String fileName = message.getFileName();
		Constants.LOGGER.fine("GetFile message received from " + message.getHeader().getHost() 
				+ " for file " + fileName);
		GetFileReply reply = new GetFileReply(fileName);
		reply.getHeader().setGUID(message.getGUID());
		mPGridP2P.send(message.getHeader().getHost(), reply);
	}
}
