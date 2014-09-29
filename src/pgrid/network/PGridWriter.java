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

//import test.planetlab.RangeQueryTester;
import pgrid.Constants;
import pgrid.RangeQuery;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.interfaces.utils.IFileStreamingMessage;
import pgrid.network.protocol.GetFileReply;
import pgrid.network.protocol.MessageHeader;
import pgrid.network.protocol.PGridMessage;
import pgrid.network.protocol.PGridCompressedMessage;
import pgrid.util.Compression;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Vector;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.zip.DataFormatException;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

/**
 * This class writes Gridella messages at the Output Stream.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class PGridWriter {

	/**
	 * The Communication Manager.
	 */
	private ConnectionManager mConnMgr = ConnectionManager.sharedInstance();

	/**
	 * The connection.
	 */
	private Connection mConn = null;

	/**
	 * The PGridP2P facility.
	 */
	private PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 *  A vector of PGridWriterListener. Use for testing and debuging purpose
	 */
	static private Vector mListener = new Vector();

	/**
	 * Lock object
	 */
	final private Object mLock = new Object();

	/**
	 * The Communication writer.
	 */
	private ConnectionWriter mWriter = null;

	/**
	 * Register a P-Grid Writer listener. This listener will be called just before
	 * the processing of the message to be sent
	 * @param listener
	 */
	static public void registerListener(PGridWriterListener listener) {
		mListener.add(listener);
	}

	/**
	 * Creates a new Gridella writer.
	 *
	 * @param conn the connection.
	 */
	PGridWriter(Connection conn) {
		mConn = conn;
		try {
			mWriter = new ConnectionWriter(conn.getSocket().getOutputStream());
		} catch (IOException e) {
			// do nothing
		}
	}

	/**
	 * Writes a PGrid message to the Output Stream.
	 *
	 * @param msg the msg to write.
	 */
	boolean sendMsg(PGridMessage msg) {
		if (msg instanceof IFileStreamingMessage) {
			//if the message is a file stream message then the contents of the file must be written to the stream.
			// if the file is empty.. no need of sending it
			if(((IFileStreamingMessage) msg).getFileSize()==0){
				//TODO: figure out why empty messages are being sent around
				return true;
			}
		}
		MessageHeader header = (MessageHeader) msg.getHeader().clone();
		header.setHost(mPGridP2P.getLocalHost());
		byte[] content = msg.getBytes();
		byte[] msgContent;
		boolean sent = true;
		// compress the bytes if necessary. PGridCompressedMessage are not compress twice
		if (mConn.isCompressed() && !header.isCompressed()) {
			msgContent = Compression.compress(content, 0, content.length);
		}	else if (!mConn.isCompressed() && header.isCompressed()) {
			try {
				msgContent = Compression.decompress(content, 0, content.length);
			} catch (DataFormatException e) {
				Constants.LOGGER.log(Level.WARNING, "Error while decompressing message data.", e);
				return false;
			}
		} else
			msgContent = content;
		
		header.setContentLen(msgContent.length);

		try {
			synchronized(mLock) {
				mWriter.write(header.getBytes(MessageHeader.LEADING_PART));
				mWriter.write(msgContent);
				mWriter.write(header.getBytes(MessageHeader.ENDING_PART));
				mConn.resetIOTimer();
				
				//if the message is a file stream message then the contents of the file must be written to the stream.
				if (msg instanceof IFileStreamingMessage) {
					IFileStreamingMessage fileStreamMsg = (IFileStreamingMessage) msg;
					Constants.LOGGER.finest("Sending a fileStream Message :"+fileStreamMsg.getFileName()+" of size "+fileStreamMsg.getFileSize() + " to host " + mConn.getHost().toHostString() + "(" + header.getHost().toHostString() + ")");
					
					try {
						if (msg instanceof GetFileReply){
							mWriter.writeFileToStream(Constants.DOWNLOAD_DIR + fileStreamMsg.getFileName(), false);
						} else {
							mWriter.writeFileToStream(Constants.CSV_DIR + fileStreamMsg.getFileName(), true);
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						Constants.LOGGER.warning("Exception in fileStream Message :"+fileStreamMsg.getFileName()+" of size "+fileStreamMsg.getFileSize() + " to host " + mConn.getHost().toHostString() + "(" + header.getHost().toHostString() + ")");
						System.out.println("Exception in fileStream Message :"+fileStreamMsg.getFileName()+" of size "+fileStreamMsg.getFileSize() + " to host " + mConn.getHost().toHostString() + "(" + header.getHost().toHostString() + ")");
					}
				}
				mConn.resetIOTimer();
			}
			if (!mConn.getSocket().isConnected()) {
				Constants.LOGGER.finest("PGrid " + msg.getDescString() + " Message could not be sent to " + header.getHost().toHostString() +". Reconnect.");
				sent = false;
			}
		} catch (IOException e) {
			if (PGridP2P.sharedInstance().isInDebugMode())
				Constants.LOGGER.finest("PGrid " + msg.getDescString() + " Message could not be sent to " + header.getHost().toHostString() +". IOException.");
			mConn.setStatus(Connection.STATUS_ERROR);
			mConnMgr.socketClosed(mConn, false);
			sent = false;
		}

		if (sent) {
		//	Constants.LOGGER.finer("PGrid " + msg.getDescString() + " Message sent to " + mConn.getHost().toHostString());
			}

		if (PGridP2P.sharedInstance().isInDebugMode())
		//	Constants.LOGGER.finest("Message Content:\n" + header.toXMLString(MessageHeader.LEADING_PART) + msg.toXMLString() + header.toXMLString(MessageHeader.ENDING_PART));

		if (PGridP2P.sharedInstance().isInTestMode()) {
			// statistics
			mPGridP2P.getStatistics().Messages[header.getDesc()]++;
			mPGridP2P.getStatistics().Bandwidth[header.getDesc()] += msgContent.length + header.getSize();
			mPGridP2P.getStatistics().BandwidthUncompr[header.getDesc()] += content.length + header.getSize();

			Iterator it = mListener.iterator();

			for(;it.hasNext();) {
				((PGridWriterListener)it.next()).messageWritten(msg);
			}
		}

		mConn.incSentCount();
		mConn.incSentBytes(msgContent.length + header.getSize());



		return true;
	}

}