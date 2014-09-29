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

import pgrid.network.protocol.*;
import pgrid.util.Compression;
import pgrid.util.LexicalDefaultHandler;
import pgrid.Constants;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.interfaces.utils.IFileStreamingMessage;

import java.util.zip.DataFormatException;
import java.util.logging.Level;
import java.io.UnsupportedEncodingException;
import java.io.StringReader;
import java.io.IOException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Class description goes here
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public class PGridDecoder {

	/**
	 * Message name to Pgrid message mapper
	 */
	private PGridMessageMapping mMapping = PGridMessageMapping.sharedInstance();

	public String getDecompressedData(PGridCompressedMessage message) {
		// read message content
		byte[] msgContent = message.getBytes();

		StringBuffer msg = new StringBuffer(msgContent.length);
		// decompress the bytes if necessary
		if (message.getHeader().isCompressed()) {
			byte[] byteArray;
			try {
				byteArray = Compression.decompress(msgContent, 0, msgContent.length);
			} catch (DataFormatException e) {
				Constants.LOGGER.log(Level.WARNING, "Error while decompressing message data.", e);
				return null;
			}
			if (byteArray != null)
				try {
					msg.append(new String(byteArray, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					Constants.LOGGER.log(Level.WARNING, "Charset conversion.", e);
				}

			message.setBytes(byteArray);
			message.getHeader().setCompressedFlag(false);
		} else {
			try {
				msg.append(new String(msgContent, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				Constants.LOGGER.log(Level.WARNING, "Charset conversion.", e);
			}
		}

		return msg.toString().trim();

	}

	/**
	 * Decode a compressed P-Grid message
	 * @param message to decode
	 * @return the decompressed message
	 */
	public PGridMessage decode(PGridCompressedMessage message) {
		XMLReader parser = message.getHeader().getParser();

		String msgString = getDecompressedData(message);
		if (msgString.length() == 0) {
			if (PGridP2P.sharedInstance().isInDebugMode())
				Constants.LOGGER.finer("Message is null.");
			return null;
		}

		PGridMessage recvMsg = null;
		try {
			// create an instance of the message
			recvMsg = mMapping.getPGridMessage(message.getHeader());

			synchronized(parser) {
				// the message name could be separeted either a space, a slash or a bigger then in an XML representation
				parser.setProperty("http://xml.org/sax/properties/lexical-handler", recvMsg);
				parser.setContentHandler((LexicalDefaultHandler)recvMsg);
			
				parser.parse(new InputSource(new StringReader(msgString)));
		
				if(recvMsg instanceof IFileStreamingMessage){
					((IFileStreamingMessage)recvMsg).notifyEnd();
				}
			}
		} catch (SAXParseException e) {
			Constants.LOGGER.warning("Could not parse message in line '" + e.getLineNumber() + "', column '" + e.getColumnNumber() + "'! (" + e.getMessage() + ")");
			if (PGridP2P.sharedInstance().isInDebugMode())
				Constants.LOGGER.log(Level.WARNING, "", e);
			return null;
		} catch (SAXException e) {
			Constants.LOGGER.log(Level.WARNING, "Sax error", e);
			return null;
		} catch (IOException e) {
			Constants.LOGGER.log(Level.WARNING, "IO error", e);
			return null;
		} catch (Throwable e) {
			e.printStackTrace();
			Constants.LOGGER.log(Level.WARNING, "Unable to decode message:\n" + message.getHeader().toXMLString(MessageHeader.LEADING_PART) + msgString + message.getHeader().toXMLString(MessageHeader.ENDING_PART)+
					"\nError: "+e.getMessage());
		return null;
		}

		Constants.LOGGER.finer("PGrid " + recvMsg.getDescString() + " Message received from " + recvMsg.getHeader().getHost().toHostString());
		if (PGridP2P.sharedInstance().isInDebugMode()) {
			Constants.LOGGER.finest("Message Content:\n" + message.getHeader().toXMLString(MessageHeader.LEADING_PART) + recvMsg.toXMLString() + message.getHeader().toXMLString(MessageHeader.ENDING_PART));
		}

		return recvMsg;
	}

	/**
	 * Decode a compressed P-Grid message
	 * @param header message header
	 * @param msgString to decode
	 * @return the decompressed message
	 */
	public PGridMessage decode(MessageHeader header, String msgString) {
		XMLReader parser = header.getParser();

		if (msgString.length() == 0) {
			if (PGridP2P.sharedInstance().isInDebugMode())
				Constants.LOGGER.finer("Message is null.");
			return null;
		}

		PGridMessage recvMsg = null;
		try {
			// create an instance of the message
			recvMsg = mMapping.getPGridMessage(header);

			synchronized(parser) {
				// the message name could be separeted either a space, a slash or a bigger then in an XML representation
				parser.setProperty("http://xml.org/sax/properties/lexical-handler", recvMsg);
				parser.setContentHandler((LexicalDefaultHandler)recvMsg);
				parser.parse(new InputSource(new StringReader(msgString)));
			}
		} catch (SAXParseException e) {
			Constants.LOGGER.warning("Could not parse message in line '" + e.getLineNumber() + "', column '" + e.getColumnNumber() + "'! (" + e.getMessage() + ")");
			if (PGridP2P.sharedInstance().isInDebugMode())
				Constants.LOGGER.log(Level.WARNING, "", e);
			return null;
		} catch (SAXException e) {
			Constants.LOGGER.log(Level.WARNING, "Sax error", e);
			return null;
		} catch (IOException e) {
			Constants.LOGGER.log(Level.WARNING, "IO error", e);
			return null;
		} catch (Throwable e) {
			Constants.LOGGER.log(Level.WARNING, "Unable to decode message:\n" + header.toXMLString(MessageHeader.LEADING_PART) + msgString + header.toXMLString(MessageHeader.ENDING_PART)+
					"\nError: "+e.getMessage());
			return null;
		}

	/*	if (PGridP2P.sharedInstance().isInDebugMode()) {
			Constants.LOGGER.finest("Message Content:\n" + header.toXMLString(MessageHeader.LEADING_PART) + recvMsg.toXMLString() + header.toXMLString(MessageHeader.ENDING_PART));
		}*/

		return recvMsg;
	}

}
