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

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import pgrid.Constants;
import pgrid.PGridHost;
import pgrid.XMLizable;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.protocol.*;
import pgrid.network.router.Router;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Vector;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class reads a P-Grid messages from the Input Stream.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class PGridReader implements Runnable {

	/**
	 *  A vector of PGridWriterListener. Use for testing and debuging purpose
	 */
	static private Vector mListener = new Vector();

	/**
	 * The Communication Manager.
	 */
	private ConnectionManager mConnMgr = ConnectionManager.sharedInstance();

	/**
	 * The connection.
	 */
	private Connection mConn = null;

	/**
	 * The SAX Parser.
	 */
	private XMLReader mParser = null;

	/**
	 * The PGridP2P facility.
	 */
	private PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 * The PGridP2P facility.
	 */
	private Router mRouter = mPGridP2P.getRouter();

	/**
	 * The Communication reader.
	 */
	private ConnectionReader mReader = null;

	/**
	 * This map is used to avoid looping messages in case of stale routing table
	 */
	private ConcurrentHashMap<String,Integer> loopingMessages = new ConcurrentHashMap<String, Integer>();
	private ArrayList<String> loopingGUIDs = new ArrayList<String>();
	
	private final int MAX_LOOPS = 2;
	
	private final int MAX_LOOP_SIZE = 100;
	
	/**
	 * Register a P-Grid Reader listener. This listener will be called just after
	 * the processing of the message.
	 *
	 * @param listener
	 */
	static public void registerListener(PGridReaderListener listener) {
		mListener.add(listener);
	}


	/**
	 * Creates a new Gridella reader.
	 *
	 * @param conn        the Connection.
	 * @param msgDispatcher the listener for incoming messages.
	 */
	PGridReader(Connection conn, MessageDispatcher msgDispatcher) {
		mConn = conn;
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);
			mParser = spf.newSAXParser().getXMLReader();//XMLReaderFactory.createXMLReader();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}

	/**
	 * Reads and returns a PGridP2P message header from the Input Stream.
	 *
	 * @return the message header string.
	 * @throws ConnectionClosedException  the connection was closed by the remote host.
	 * @throws ConnectionTimeoutException the connection has timed out.
	 */
	private String readHeader() throws ConnectionClosedException, ConnectionTimeoutException {
		// read message header (leading part)
		StringBuffer buffer = new StringBuffer(100);
		while (true) {
			String line = mReader.readLine();
			if (line == null)
				break;

			if (line.trim().equals(XMLizable.XML_ELEMENT_OPEN_END + MessageHeader.XML_HEADER + XMLizable.XML_ELEMENT_CLOSE)) {
				buffer.append(line + XMLizable.XML_NEW_LINE);
				break;
			}
			buffer.append(line + XMLizable.XML_NEW_LINE);
		}
		return buffer.toString();
	}

	/**
	 * Reads a PGridP2P message from the Input Stream and calls the Message Handler.           
	 *
	 * @throws ConnectionClosedException  the connection was closed by the remote host.
	 * @throws ConnectionTimeoutException the connection has timed out.
	 */
	private void readMsg() throws ConnectionClosedException, ConnectionTimeoutException {
		// read message header
		String headerStr = readHeader();
		if (headerStr == null)
			return;

		headerStr += MessageHeader.CLOSING_TAG;

		MessageHeader msgHeader = new MessageHeader();

		msgHeader.setParser(mParser);

		try {
			mParser.setContentHandler(msgHeader);
			mParser.parse(new InputSource(new StringReader(headerStr)));
		} catch (SAXException e) {
			mConn.incDroppedCount();
			if (PGridP2P.sharedInstance().isInDebugMode()) {
				Constants.LOGGER.log(Level.SEVERE, "Error while parsing message header." + e);
				Constants.LOGGER.log(Level.SEVERE, "Message header:" + headerStr);
			}
			return;
		} catch (IOException e) {
			mConn.incDroppedCount();
			if (PGridP2P.sharedInstance().isInDebugMode()) {
				Constants.LOGGER.log(Level.SEVERE, "Error while parsing message header." + e);
				Constants.LOGGER.log(Level.SEVERE, "Message header." + headerStr);
			}
			return;
		}
		mConn.incReceivedBytes(msgHeader.getSize());

		if (!msgHeader.isValid()) {
			
			mConn.incDroppedCount();
			if (PGridP2P.sharedInstance().isInDebugMode())
				Constants.LOGGER.finer("Received message not valid.");
				Constants.LOGGER.log(Level.INFO, "Decoding header: " + msgHeader.toXMLString());
			return;
		}

		// read message content
		byte[] msgContent = mReader.readBytes(msgHeader.getContentLen());

		mConn.incReceivedBytes(msgContent.length);

		// read message header (ending part)
		String endingHeader = mReader.readLine();
		if (!endingHeader.equals(MessageHeader.CLOSING_TAG)) {
			mConn.incDroppedCount();
			if (PGridP2P.sharedInstance().isInDebugMode())
				Constants.LOGGER.finer("Message closing is missing.");
			return;
		}

		// increment the number of hops for this message
		msgHeader.incHops();
		PGridCompressedMessage recvMsg = new PGridCompressedMessage(msgHeader, msgContent);

		// check if the message is compressed
		if (mConn.isCompressed()) {
			msgHeader.setCompressedFlag(true);
		}
		mConn.getHost().setPort(recvMsg.getHeader().getHost().getPort());

		Constants.LOGGER.finer("Incomming PGrid message " + recvMsg.getGUID() + " received from " + mConn.getHost().toHostString());

		if (PGridP2P.sharedInstance().isInTestMode()) {
			// statistics
			mPGridP2P.getStatistics().Messages[msgHeader.getDesc()]++;
			mPGridP2P.getStatistics().Bandwidth[msgHeader.getDesc()] += msgHeader.getSize() + msgContent.length;
			mPGridP2P.getStatistics().BandwidthUncompr[msgHeader.getDesc()] += msgHeader.getSize() + msgContent.length;
		}
		
		/***
		 * NaFT handling: 
		 * 
		 * If message is not for me, forward it direcly without decompressing it
		 */
		PGridHost destinationHost = recvMsg.getHeader().getDestinationHost();

		if (destinationHost != null){
			
			if (!mPGridP2P.getLocalHost().equals(destinationHost)) {
				NaFTManager.LOGGER.finer("Message " + recvMsg.getGUID() + " is not for this host. Trying to relay it to: " + destinationHost);
				
				if (loopingMessages.containsKey(recvMsg.getGUID().toString())){
					int count = loopingMessages.get(recvMsg.getGUID().toString()) + 1;
					if (count >= MAX_LOOPS) {
						NaFTManager.LOGGER.finer("Message " + recvMsg.getGUID() + " has been seen " + MAX_LOOPS + " on this host. Silently discarding it");
						return;
					}
					loopingMessages.put(recvMsg.getGUID().toString(), count);
				} else {
					
					while (loopingGUIDs.size() > MAX_LOOP_SIZE) {
						loopingMessages.remove(loopingGUIDs.remove(0));
					}
					loopingGUIDs.add(recvMsg.getGUID().toString());
					loopingMessages.put(recvMsg.getGUID().toString(), 1);
				}
				
				mPGridP2P.send(destinationHost, recvMsg);
				mConn.incReceivedCount();
				
				
				/**
				 * If the message announces a direct tcp connection (file streaming),
				 * then prepare a NaFTConnectionRelay thread and send the relay information back to sending host
				 */
				if (PGridMessageMapping.sharedInstance().isFileStreaming(recvMsg.getHeader().getDesc())) {
					
					NaFTManager.LOGGER.fine("TCP connection relay is needed from host " + mConn.getHost().toHostString() + " to " + destinationHost.toHostString() + "(msg: " + recvMsg.getGUID() + ")");
					
					Connection outConn = ConnectionManager.sharedInstance().connect(destinationHost);
					if (outConn != null && outConn.getStatus() == Connection.STATUS_CONNECTED) {
											
						DataOutputStream out = null;
						try {
							out = new DataOutputStream(new BufferedOutputStream(outConn.getSocket().getOutputStream()));
						} catch (IOException e) {
							e.printStackTrace();
						}
						mReader.readBytesToStream(Integer.parseInt(msgHeader.getAdditionalAttribute("FileLength")), out);
						
						NaFTManager.LOGGER.fine("TCP connection relay should be closed now (from host " + mConn.getHost().toHostString() + " to " + destinationHost.toHostString() + ")");
						
					} else {
						NaFTManager.LOGGER.warning("Streaming message " + recvMsg.getGUID() + " for host " + destinationHost.toHostString() + " has no outgoing connection available. Wrong relay ?");
					}
				}
				
				return;
			}
		}
		
		/**
		 * If forwarded message is for me, then replace the header with the source value
		 * in order to contact the source node directly
		 */
		
		PGridHost sourceHost = recvMsg.getHeader().getSourceHost();
		
		if (sourceHost != null && destinationHost != null){
			
			if (mPGridP2P.getLocalHost().equals(destinationHost)) {
				// Updating Relay
				mPGridP2P.getNaFTManager().setRelay(recvMsg.getHeader().getHost());
				
				NaFTManager.LOGGER.fine("Replacing relay host information with the source host information:\n" +
						"relay host: " + recvMsg.getHeader().getHost() + "\nsource host: " + sourceHost);
				recvMsg.getHeader().setHost(sourceHost);
			}
		}
		
		
		/*
		 * 
		 * NaFT handling
		 ***/
	
		
		/**
		 * GetFileReply message has type 31, according to MessageMapping.xml
		 */
		if (PGridMessageMapping.sharedInstance().isFileStreaming(recvMsg.getHeader().getDesc()) &&
				recvMsg.getHeader().getDesc() == 31) {
			Constants.LOGGER.finest("PGridReader: writing " + msgHeader.getAdditionalAttribute(GetFileReply.XML_FILE_SIZE) + 
					" bytes from network stream to file " + 
					msgHeader.getAdditionalAttribute(GetFileReply.XML_FILE_NAME));
			
			mReader.saveStreamToFile(Constants.DOWNLOAD_DIR + msgHeader.getAdditionalAttribute(GetFileReply.XML_FILE_NAME),
					Integer.parseInt(msgHeader.getAdditionalAttribute(GetFileReply.XML_FILE_SIZE)), false);
			
		} else 	if (PGridMessageMapping.sharedInstance().isFileStreaming(recvMsg.getHeader().getDesc())) {
			//@TODO change the file-name
			Constants.LOGGER.finest("PGridReader: Writing From Network Stream to CSV File :"+ msgHeader.getAdditionalAttribute("FileName")+"("+msgHeader.getAdditionalAttribute("FileLength")+")");

//			recvMsg.getHeader().setAdditionalAttribute("fileName", Constants.CSV_DIR+"RECEIVED.csv");
			Constants.LOGGER.finest("===========>"+msgHeader.getAdditionalAttribute("FileLength"));
			mReader.saveStreamToFile(Constants.CSV_DIR + msgHeader.getAdditionalAttribute("FileName"),
					Integer.parseInt(msgHeader.getAdditionalAttribute("FileLength")),
					true);
		}		

		if (PGridMessageMapping.sharedInstance().isLowPriority(recvMsg.getHeader().getDesc())) {
			LowPriorityMessageManager.sharedInstance().incomingMessage(recvMsg);
		} else {
			mRouter.incomingMessage(recvMsg);
		}

		mConn.incReceivedCount();

		if (PGridP2P.sharedInstance().isInTestMode()) {
			Iterator it = mListener.iterator();

			for(;it.hasNext();) {
				((PGridReaderListener)it.next()).messageRead(recvMsg);
			}
		}
	}

	/**
	 * Starts the P-Grid reader.
	 */
	public void run() {
		if (mConn.getSocket() == null) {
			mConnMgr.socketClosed(mConn);
			if (PGridP2P.sharedInstance().isInDebugMode())
				Constants.LOGGER.finest("Connection to host " + mConn.getHost().toHostString() + " closed. Socket is null.");

			return;
		}
		try {
			mReader = new ConnectionReader(mConn.getSocket().getInputStream(), mConn);
		} catch (NullPointerException e) {
			mConnMgr.socketClosed(mConn);
			if (PGridP2P.sharedInstance().isInDebugMode())
				Constants.LOGGER.finest("Connection to host " + mConn.getHost().toHostString() + " closed. NullPointerException.");

			return;
		} catch (IOException e) {
			mConnMgr.socketClosed(mConn);
			e.printStackTrace();
			return;
		}
		if (mParser == null)
			return;

		while (mConn.getStatus() == Connection.STATUS_CONNECTED || mConn.isClosing()) {
			// receive a new message

			try {
				readMsg();
			} catch (ConnectionClosedException e) {
				mConn.setStatus(Connection.STATUS_ERROR, "Closed");
				break;
			} catch (ConnectionTimeoutException e) {
				mConnMgr.connectionTimeout(mConn);
			}
		}
		mConnMgr.socketClosed(mConn);
	}

}