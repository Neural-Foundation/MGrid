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

import pgrid.util.WorkerThread;
import pgrid.network.protocol.ExchangeIndexEntriesMessage;
import pgrid.network.protocol.PGridMessage;
import pgrid.network.protocol.PGridCompressedMessage;
import pgrid.network.protocol.MessageHeader;
import pgrid.network.router.Router;
import pgrid.core.index.DBIndexTable;
import pgrid.core.DBManager;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.Constants;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Class description goes here
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public class LowPriorityMessageManager extends WorkerThread {

	final private Object mLock = new Object();

	private int mContinue = 0;

	private DBManager mDBManager = DBManager.sharedInstance();

	/**
	 * Decode a PGridMessage data
	 */
	protected PGridDecoder mDecoder = new PGridDecoder();

	/**
	 * The SAX Parser.
	 */
	private XMLReader mParser = null;

	/**
	 * Message pick.
	 */
	private PGridCompressedMessage mPick = null;

	/**
	 * Message header list
	 */
//	private List<MessageHeader> mMessages = new ArrayList<MessageHeader>();
	private Queue<MessageHeader> mMessages = new LinkedList<MessageHeader>();

	/**
	 * The PGridP2P facility.
	 */
	private Router mRouter = PGridP2P.sharedInstance().getRouter();

	private Thread mThread = null;

	/**
	 * The reference to the only instance of this class (Singleton
	 * pattern). This differs from the C++ standard implementation by Gamma
	 * et.al. since Java ensures the order of static initialization at runtime.
	 *
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	private static final LowPriorityMessageManager SHARED_INSTANCE = new LowPriorityMessageManager();

	/**
	 * This creates the only instance of this class. This differs from the C++ standard implementation by Gamma et.al.
	 * since Java ensures the order of static initialization at runtime.
	 *
	 * @return the shared instance of this class.
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	public static LowPriorityMessageManager sharedInstance() {
		return SHARED_INSTANCE;
	}

	public LowPriorityMessageManager() {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		try {
			mParser = spf.newSAXParser().getXMLReader();
		} catch (SAXException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		} catch (ParserConfigurationException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}

//		mDBManager.dropMessages();
	}

	/**
	 * Add a new low priority message to process
	 * @param message
	 */
	public void incomingMessage(PGridCompressedMessage message) {
		synchronized(mLock) {
			if (mPick == null) {
				mPick = message;
				message.getHeader().setParser(mParser);
			} else {
				mDBManager.pushMessage(message.getHeader().getGUID().toString(), message.getBytes());

				Constants.LOGGER.finest("PGrid Compressed  Message from " + message.getHeader().getHost().toHostString() + " added to the low priority message queue.");
				message.getHeader().setParser(mParser);
				mMessages.add(message.getHeader());
			}

			mContinue=mContinue+1;
		}

	}

	/**
	 * Checks if the task can proceed.
	 *
	 * @return <tt>true</tt> if this task can proceed, for example after been waiting on the internal lock.
	 */
	protected boolean isCondition() {
		boolean cont;
		synchronized(mLock) {
			cont = (mContinue>0);
		}
		return cont;
		/*synchronized(mLock) {
			return !mMessages.isEmpty();
		}*/
	}

	/**
	 * Handles an occured exception.
	 *
	 * @param t error to be handled
	 */
	protected void handleError(Throwable t) {
		if (t instanceof InterruptedException) {
			Constants.LOGGER.finer("Maintenance manager interupted.");
			halt();
		} else {
			Constants.LOGGER.log(Level.WARNING, "Error in Maintenance thread", t);
		}
	}

	/**
	 * Called just after run() method starts
	 */
	protected void prepareWorker() throws Exception {
		mThread = Thread.currentThread();
	}

	/**
	 * Called just before run() method stops
	 */
	protected void releaseWorker() throws Exception {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Does a task's job; can wait on the internal lock Object or any other
	 * Objects during execution
	 *
	 * @throws Exception: including InterruptedException if the task was
	 *                    interrupted by thread's interrupt()
	 */
	protected void work() throws Exception {
		int msgCount = 0;
		byte[] msg;
		boolean cont=true;
		ArrayList<PGridMessage> messages;
		PGridMessage message;
		PGridCompressedMessage cMessage;
		MessageHeader msgHeader = null;


		/*synchronized(mLock) {
					messages = new ArrayList<PGridMessage>(mMessages);
					mMessages.clear();
				}
				for (PGridMessage message: messages) { */
		while (cont) {
			//Constants.LOGGER.info("In message proc");
			synchronized(mLock) {
				if (mPick != null) {
					msgHeader = mPick.getHeader();
					msg = mPick.getBytes();
					mPick = null;
				} else {
					if (mMessages.isEmpty()) {
						cont=false;
						continue;
					}

					msgHeader = mMessages.remove();
					if ((msg = mDBManager.popMessage(msgHeader.getGUID().toString())) == null) {
						Constants.LOGGER.warning("Message missing");
						cont=false;
						continue;
					}
				}

			}
			// parse header
			/*MessageHeader msgHeader = new MessageHeader();

			msgHeader.setParser(mParser);

			try {
				mParser.setContentHandler(msgHeader);
				mParser.parse(new InputSource(new StringReader(msg[0])));
			} catch (SAXException e) {
				if (PGridP2P.sharedInstance().isInDebugMode()) {
					Constants.LOGGER.log(Level.WARNING, "Error while parsing message header.", e);
				}
				continue;
			}  */
			cMessage = new PGridCompressedMessage(msgHeader, msg);
			String msgString = mDecoder.getDecompressedData(cMessage);


			// decode message
			message = mDecoder.decode(msgHeader, msgString);

			Constants.LOGGER.finest("PGrid " + message.getDescString() + " Message from " + message.getHeader().getHost().toHostString() + " will by processed by the low priority manager.");
			ExchangeIndexEntriesMessage msgt = (ExchangeIndexEntriesMessage)message;
			Constants.LOGGER.finest(msgt.getCurrent()+" of "+msgt.getTotal()+" messages");
			
			mRouter.incomingMessage(message);
			msgCount++;
		}

		synchronized(mLock) {

			mContinue = mContinue-msgCount;
		}
	}

	public void shutdown() {
		if (mThread != null)
			mThread.interrupt();
	}
}
