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

import pgrid.Constants;
import pgrid.Properties;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.network.protocol.PGridMessage;
import pgrid.network.protocol.MessageHeader;
import pgrid.network.protocol.BootstrapMessage;
import pgrid.network.protocol.PGridCompressedMessage;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class read the file MessagesMapping.xml containing the mapping between the xml message name
 * and its java class.
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public class PGridMessageMapping extends DefaultHandler {

	/**
	 * A part of the XML string.
	 */
	public static final String XML_MESSAGE_MAPPING = "MessageMapping";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_MAPPING = "Mapping";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_NAME = "MessageName";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_TYPE = "Type";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_MESSAGECLASS = "MessageClass";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_HANDLERCLASS = "RemoteHandlerClass";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_DESCRIPTION = "Description";

	/**
	 * A part of the XML string.
	 */
	public static final String XML_LOW_PRIORITY = "LowPriority";

	/**
	 * A part of the XML string. Indicates if the message is a file-stream-message
	 */
	public static final String XML_FILE_STREAMING = "FileStreaming";

	/**
	 * The reference to the only instance of this class (Singleton
	 * pattern). This differs from the C++ standard implementation by Gamma
	 * et.al. since Java ensures the order of static initialization at runtime.
	 *
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	private static final PGridMessageMapping SHARED_INSTANCE = new PGridMessageMapping();

	/**
	 * Number of diffrent message
	 */
	private int mNBMsg = 0;

	/**
	 * Mapping bt the message name and all information needed to recreate a message object
	 */
	private Hashtable<String, MessageMapping> mMessageClass = new Hashtable<String, MessageMapping>();

	/**
	 * Mapping bt the message type and all information needed to recreate a message object
	 */
	private Hashtable<Integer, MessageMapping> mMessageTypeClass = new Hashtable<Integer, MessageMapping>();

	/**
	 * Remote message handler
	 */
	private Hashtable<String, RemoteMessageHandler> mRemoteMessageHandlers =
			new Hashtable<String, RemoteMessageHandler>();

	/**
	 * This creates the only instance of this class. This differs from the C++ standard implementation by Gamma et.al.
	 * since Java ensures the order of static initialization at runtime.
	 *
	 * @return the shared instance of this class.
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	public static PGridMessageMapping sharedInstance() {
		return SHARED_INSTANCE;
	}

	/**
	 * Initialize the pgrid message mapper facility with the given mapping file
	 *
	 * @param mappingFile	XML file where the mapping is discribed
	 */
	public void init(String mappingFile) {
		try {
			StringBuffer buf = new StringBuffer();
			String line = null;
			Constants.LOGGER.config("reading P-Grid message mapping from '" + mappingFile + "' ...");
			InputStream input = getClass().getResourceAsStream("/" + mappingFile);
			BufferedReader in = new BufferedReader(new InputStreamReader(input));

			while((line = in.readLine()) != null) {
				buf.append(line).append("\n");
			}

			SAXParserFactory spf = SAXParserFactory.newInstance();
			XMLReader parser = spf.newSAXParser().getXMLReader();

			parser.setContentHandler(this);
			parser.parse(new InputSource(new StringReader(buf.toString())));
		} catch (SAXException e) {
			Constants.LOGGER.log(Level.SEVERE, null, e);
			System.exit(-1);
		} catch (FileNotFoundException e) {
			Constants.LOGGER.log(Level.SEVERE, null, e);
			System.exit(-1);
		} catch (IOException e) {
			Constants.LOGGER.log(Level.SEVERE, null, e);
			System.exit(-1);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}

	/**
	 * This method takes a message header and instanciate a PGrid message corresponding to it. If no class is found,
	 * null is returned.
	 *
	 * There are two way a message can be instantiated. Either by reflection or by a call the the clone method. The first
	 * one is used only to create the very first message of a given type, then this message will be cloned.
	 *
	 * @return a PGrid message corresponding to the msgName message name. If no class is found,
	 * null is returned.
	 */
	public PGridMessage getPGridMessage(MessageHeader header) {
		MessageMapping m = mMessageTypeClass.get(header.getDesc());
		PGridMessage msg = null;

		if (m == null) return null;

		if (m.mCloneMe == null) {
			try {
				Constructor con = m.mMessageClass.getConstructor(MessageHeader.class);
				m.mCloneMe = (PGridMessage)con.newInstance(header);
			} catch (InstantiationException e) {
				Constants.LOGGER.warning("Class \""+m.mMessageClass.getName()+"\" cannot be instanciated.");
				return null;
			} catch (IllegalAccessException e) {
				Constants.LOGGER.warning("Illegal access exception triggered for "+m.mMessageClass.getName()+".");
				return null;
			} catch (ClassCastException e) {
				Constants.LOGGER.warning("Class \""+m.mMessageClass.getName()+"\" is not a subclass of PGridMessage.");
				return null;
			} catch (NoSuchMethodException e) {
				Constants.LOGGER.warning("Class \""+m.mMessageClass.getName()+"\" cannot be instanciated.");
				return null;
			} catch (InvocationTargetException e) {
				Constants.LOGGER.warning("Class \""+m.mMessageClass.getName()+"\" throw an exception:"+e.getTargetException().toString()+".");
				e.getTargetException().printStackTrace();
				return null;
			}

		}

		msg = (PGridMessage) m.mCloneMe.clone();
		msg.setHeader(header);

		return msg;
	}

	/**
	 * Return the type of a given message
	 *
	 * @return the type of a given message or -1 if it is unknown
	 */
	public int getType(String msgName) {
		MessageMapping m = mMessageClass.get(msgName);

		if (m == null) return -1;

		return m.mType;
	}

	/**
	 * Return the description for a given message
	 *
	 * @return the description for a given message or null if it is unknown
	 */
	public String getDescription(String msgName) {
		MessageMapping m = mMessageClass.get(msgName);

		if (m == null) return null;

		return m.mDesc;
	}

	/**
	 * Return true if the given message has a low priority.
	 *
	 * @return the description for a given message or null if it is unknown
	 */
	public boolean isLowPriority(int type) {
		MessageMapping m = mMessageTypeClass.get(type);

		if (m == null) return false;

		return m.mLowPriority;
		}

	/**
	 * Return true if the given message is a File-Streaming-Message-Type
	 *
	 * @return the description for a given message or null if it is unknown
	 */
	public boolean isFileStreaming(int type) {
		MessageMapping m = mMessageTypeClass.get(type);

		if (m == null) return false;

		return m.mFileStreaming;
		}

	/**
	 * This method register all remote message handler to the message manager
	 */
	public void registerRemoteHandler() {
		for(MessageMapping m: mMessageClass.values()) {
			if (m.mHandler != null)
				MessageManager.sharedInstance().registerMessageRemoteHandler(m.mType, m.mHandler);
		}
	}

	/**
	 * Returns the number of message type
	 * @return the number of message type
	 */
	public int getNBMessagesType() {
		return mNBMsg+1;
	}

	/**
	 * The Parser will invoke this method at the beginning of every element in the XML document; there will be a
	 * corresponding endElement event for every startElement event (even when the element is empty). All of the element's
	 * content will be reported, in order, before the corresponding endElement event.
	 *
	 * @param uri   the Namespace URI.
	 * @param lName the local name (without prefix), or the empty string if Namespace processing is not being performed.
	 * @param qName the qualified name (with prefix), or the empty string if qualified names are not available.
	 * @param attrs the attributes attached to the element. If there are no attributes, it shall be an empty Attributes
	 *              object.
	 * @throws org.xml.sax.SAXException any SAX exception, possibly wrapping another exception.
	 */
	public void startElement(String uri, String lName, String qName, Attributes attrs) throws SAXException {
		if (qName.equals(XML_MAPPING)) {
			MessageMapping messageMapping = new MessageMapping();
			Class handler;
			String sHandler = attrs.getValue(XML_HANDLERCLASS);

			try {
				messageMapping.mMessageClass = Class.forName(attrs.getValue(XML_MESSAGECLASS));
			} catch (ClassNotFoundException e) {
				Constants.LOGGER.warning("Class \""+attrs.getValue(XML_MESSAGECLASS)+"\" is unknown.");
				return;
			}

			messageMapping.mMessageName = attrs.getValue(XML_NAME);

			messageMapping.mDesc = attrs.getValue(XML_DESCRIPTION);
			messageMapping.mType = Integer.parseInt(attrs.getValue(XML_TYPE));
			String lp = attrs.getValue(XML_LOW_PRIORITY);
			if (lp != null)
				messageMapping.mLowPriority = Boolean.parseBoolean(lp);
			else messageMapping.mLowPriority = false;
			
			String fs = attrs.getValue(XML_FILE_STREAMING);
			if (fs != null)
				messageMapping.mFileStreaming = Boolean.parseBoolean(fs);
			else messageMapping.mFileStreaming = false;

			if (messageMapping.mType > mNBMsg) mNBMsg = messageMapping.mType;

			if (sHandler != null && sHandler.length() > 0) {
				if ((messageMapping.mHandler = mRemoteMessageHandlers.get(sHandler)) == null) {
					try {
						handler = Class.forName(sHandler);
					} catch (ClassNotFoundException e) {
						Constants.LOGGER.warning("Class \""+sHandler+"\" found or unknown.");
						return;
					}

					try {
						messageMapping.mHandler = (RemoteMessageHandler)handler.newInstance();
						if (!mRemoteMessageHandlers.containsKey(sHandler))	mRemoteMessageHandlers.put(sHandler, messageMapping.mHandler);
					} catch (InstantiationException e) {
						Constants.LOGGER.warning("Class \""+handler.getName()+"\" cannot be instanciated.");
					} catch (IllegalAccessException e) {
						Constants.LOGGER.warning("Illegal access exception triggered for "+handler.getName()+".");
					} catch (ClassCastException e) {
						Constants.LOGGER.warning("Class \""+handler.getName()+"\" is not a subclass of PGridMessage.");
					}
				}
			}

			if (!mMessageClass.containsKey(messageMapping.mMessageName)) mMessageClass.put(messageMapping.mMessageName, messageMapping);
			if (!mMessageTypeClass.containsKey(messageMapping.mType)) mMessageTypeClass.put(messageMapping.mType, messageMapping);
			
			/*
			System.out.println("PGridMessageMapping.startElement(): mMessageClass: " + mMessageClass.size());
			System.out.println("PGridMessageMapping.startElement(): mMessageTypeClass: " + mMessageTypeClass.size());
			System.out.println("PGridMessageMapping.startElement(): mRemoteMessageHandlers: "+ mRemoteMessageHandlers.size());
			*/

		}
	}

	class MessageMapping {
		public String mMessageName = null;
		public PGridMessage mCloneMe = null;
		public Class mMessageClass = null;
		public String mDesc = null;
		public int mType = -1;
		public RemoteMessageHandler mHandler = null;
		public boolean mLowPriority = false;
		public boolean mFileStreaming = false;
	}

}
