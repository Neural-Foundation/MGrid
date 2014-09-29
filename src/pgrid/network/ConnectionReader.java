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

import java.io.*;
import java.net.SocketTimeoutException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.Charset;

import pgrid.Constants;
import pgrid.util.Compression;

/**
 * The Communication Reader provides basic functions to read messages from an
 * Input Stream.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
class ConnectionReader {

	private ByteBuffer mByteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
	private CharBuffer mCharBuffer = CharBuffer.allocate(BUFFER_SIZE);

	/**
	 * Buffer size
	 */
	static private int BUFFER_SIZE = 256;


	/**
	 * Unicode charset
	 */
	static private Charset mCharset = Charset.forName("UTF-8");

	/**
	 * Unicode decoder
	 */
	private CharsetDecoder mDecoder = mCharset.newDecoder();

	/**
	 * The buffered Input Stream reader.
	 */
	private DataInputStream mDataReader = null;

	/**
	 * The Input Stream.
	 */
	private InputStream mIn = null;

	/**
	 * P-Grid conection
	 */
	private Connection mConn;

	/**
	 * Creates a reader.
	 *
	 * @param in the Input Stream.
	 */
	ConnectionReader(InputStream in, Connection conn) {
		mConn = conn;
		mIn = in;
		mDataReader = new DataInputStream(new BufferedInputStream(mIn));
	}

	/**
	 * Reads the delivered amount of bytes.
	 *
	 * @param len the length to read.
	 * @return the read bytes.
	 * @throws ConnectionClosedException  the connection was closed by the remote host.
	 * @throws ConnectionTimeoutException the connection has timed out.
	 * @throws IllegalArgumentException   the length to read was illegal.
	 */
	byte[] readBytes(int len) throws ConnectionClosedException, ConnectionTimeoutException, IllegalArgumentException {
		if (len < 1)
			throw new IllegalArgumentException("len " + String.valueOf(len) + " is illegal!");
		byte[] data = new byte[len];
		try {
			mDataReader.readFully(data, 0, len);
			mConn.resetIOTimer();
		} catch (EOFException e) {
			throw new ConnectionClosedException();
		} catch (SocketTimeoutException e) {
			// something bad happend if I did not received any thing
			throw new ConnectionClosedException();
		} catch (InterruptedIOException e) {
			throw new ConnectionTimeoutException();
		} catch (IOException e) {
			throw new ConnectionClosedException();
		}

		return data;
	}

	/**
	 * Returns a line from the Input Stream.
	 *
	 * @return the read line.
	 * @throws ConnectionClosedException  the connection was closed by the remote host.
	 * @throws ConnectionTimeoutException the connection has timed out.
	 */
	String readLine() throws ConnectionClosedException, ConnectionTimeoutException {

		mByteBuffer.clear();
		mCharBuffer.clear();

		StringBuffer line = new StringBuffer(10);
		
		try {
			byte ch;
			byte endline = (byte)'\n';
			while (true) {
				ch = (byte) mDataReader.read();
				mConn.resetIOTimer();
				if (ch == endline) // New line in Unicode || end of stream
					break;
				if (ch == -1) throw new ConnectionClosedException();

				mByteBuffer.put(ch);

				if (mByteBuffer.position() == mByteBuffer.capacity()-1) {
					mByteBuffer.flip();
					mDecoder.decode(mByteBuffer,mCharBuffer,false);
					mCharBuffer.flip();
					line.append(mCharBuffer.toString());
					mCharBuffer.clear();
					mByteBuffer.compact();
				}
			}
		} catch (EOFException e) {
			throw new ConnectionClosedException();
		} catch (SocketTimeoutException e) {
			throw new ConnectionTimeoutException();
		} catch (InterruptedIOException e) {
			throw new ConnectionTimeoutException();
		} catch (SocketException e)    {
			throw new ConnectionClosedException();
		} catch (IOException e) {
			throw new ConnectionClosedException();
		} catch (java.nio.BufferOverflowException e) {
			System.err.println("Buffer overflow. Position: "+mByteBuffer.position()+" capacity: "+(mByteBuffer.capacity()-1));
		}
		
		mByteBuffer.flip();
		mDecoder.decode(mByteBuffer,mCharBuffer,true);
		mCharBuffer.flip();
		line.append(mCharBuffer.toString());
		mDecoder.flush(mCharBuffer);
		mCharBuffer.flip();
		line.append(mCharBuffer.toString());

		mDecoder.reset();

		return line.toString();
	}

	/**
	 * Reads the greeting message of an incoming connection request.
	 *
	 * @return the read message.
	 * @throws ConnectionClosedException  the connection was closed by the remote host.
	 * @throws ConnectionTimeoutException the connection has timed out.
	 */
	String readGreeting() throws ConnectionClosedException, ConnectionTimeoutException {
		StringBuffer greeting = new StringBuffer(256);
		String line;
		while ((line = readLine()).length() != 0) {
			greeting = greeting.append(line).append("\n");
			if (line.trim().length() == 0)
				break;
		}
		return greeting.toString();
	}

	/**
	 * Skips the delivered amount of bytes.
	 *
	 * @param len the length to skip.
	 * @throws ConnectionClosedException  the connection was closed by the remote host.
	 * @throws ConnectionTimeoutException the connection has timed out.
	 * @throws IllegalArgumentException   the delivered length to skip was illegal.
	 */
	void skipBytes(long len) throws ConnectionClosedException, ConnectionTimeoutException, IllegalArgumentException {
		long skipLen = 0;

		try {
			if ((len < 1) || (len > 65536)) {
				throw new IllegalArgumentException("len " + String.valueOf(len) + " is illegal!");
			}
			mConn.getSocket().setSoTimeout(10*1000);
			while (skipLen != len) {
				skipLen += mDataReader.skipBytes((int)(len - skipLen));
				mConn.resetIOTimer();
			}
		} catch (SocketTimeoutException e) {
			throw new ConnectionTimeoutException();
		} catch (InterruptedIOException e) {
			throw new ConnectionTimeoutException();
		} catch (IOException e) {
			throw new ConnectionClosedException();
		}

	}

	/**
	 * Reads bytesAmount bytes from current connection and sends them to DataOutputStream out
	 * This function is used as a relay for NaFT hanflding
	 * 
	 * @param bytesAmount number of bytes to transmit to outputstream
	 * @param out outputstream
	 */
	public void readBytesToStream(int bytesAmount, DataOutputStream out){
		
		NaFTManager.LOGGER.fine("Reading " + bytesAmount + " bytes and write them to outputstream");
		
		int BUFFER = 256;
		byte[] data = new byte[BUFFER];

		int count=0,bytesSoFar = 0;
		int bytesToRead = BUFFER;
		int bytesLeft = bytesAmount;
		try {

			do {
				NaFTManager.LOGGER.fine("Writing " + count + " bytes");
				out.write(data,0,count);
				mConn.resetIOTimer();
				bytesLeft -= count;
				bytesSoFar += count;
				if (bytesLeft == 0) break;
				
				if (bytesLeft < BUFFER) {
					bytesToRead = bytesLeft;
				}
			} while ((count = mDataReader.read(data, 0, bytesToRead)) != -1);
			
			NaFTManager.LOGGER.fine("Total bytes sent: " + bytesSoFar + " bytes (of " + bytesAmount + ")");
			
			out.flush();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
//	/**
//	 * The data from the network-input-stream will be written to the specified file location
//	 * @param fileLocation
//	 */
//	void readToFile(String fileLocation){
//		int i = 0;
//		PrintWriter out = null;
//		String line;
//		try {
//			File outputFile = new File(fileLocation);
//			outputFile.getParentFile().mkdirs();
//			if(!outputFile.exists()) outputFile.createNewFile();
//			out = new PrintWriter(new FileWriter(fileLocation));
//			while( (line = DataInputStream.readUTF(mDataReader)) != null){
//				i++;
//				out.println(line);
//				mConn.resetIOTimer();
////				if(i%1000 == 0){
////					System.out.println(line.length()+" : Wrote line: "+i);
////				}
//			}
//		} catch (IOException e) {
////			e.printStackTrace();
////			System.out.println("Writing Done on Server Side");
//		}
//		finally{
//			out.close();
//		}
//	}
	
	/**
	 * The data from the network-input-stream will be written to the specified file location
	 */
	void saveStreamToFile(String filePath, int fileSize, boolean compressed){
		
		String fileLocation = filePath;
		if (compressed) fileLocation += ".zip"; 
		
		int BUFFER = 256;
		byte[] data = new byte[BUFFER];
		BufferedOutputStream out = null;

		int count=0;
		int bytesToRead = BUFFER;
		int bytesLeft = fileSize;
		try {

			File outputFile = new File(fileLocation);
			outputFile.getParentFile().mkdirs();
			if(!outputFile.exists()) outputFile.createNewFile();
			out = new BufferedOutputStream(new FileOutputStream(fileLocation));

			do {			
				out.write(data,0,count);
				mConn.resetIOTimer();
		
				bytesLeft -= count;
				
				if (bytesLeft == 0) break;
				
				if (bytesLeft < BUFFER) {
					bytesToRead = bytesLeft;
				}
			} while ((count = mDataReader.read(data, 0, bytesToRead)) != -1);
			
			out.flush();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (compressed){
			Compression.decompressFile(fileLocation, filePath);
//			if(new File(fileLocation+".old").exists()) new File(fileLocation+".old").delete();
//			new File(fileLocation).renameTo(new File(fileLocation+".old"));//renaming the received zip file
			new File(fileLocation).delete();
		}
		
	}
	
	
	/**
	 * The data from the network-input-stream will be written to the specified file location
	 * @param fileLocation
	 */
	/*
	void readToFile(String fileLocation, int fileLength){
		long t = System.currentTimeMillis();
		String decompressedFileLocation = new String(fileLocation);
		fileLocation = fileLocation+".zip";
		int BUFFER = 256;
		byte[] data = new byte[BUFFER];
		BufferedOutputStream out = null;

		int count=0,bytesSoFar = 0;
		int bytesToRead = BUFFER;
		int bytesLeft = fileLength;
		try {
//			mConn.getSocket().setSoTimeout(8*1000);
			File outputFile = new File(fileLocation);
			outputFile.getParentFile().mkdirs();
			if(!outputFile.exists()) outputFile.createNewFile();
			out = new BufferedOutputStream(new FileOutputStream(fileLocation));

			do {			
				out.write(data,0,count);
				mConn.resetIOTimer();
				
				bytesLeft -= count;
				
				if (bytesLeft == 0) break;
				
				if (bytesLeft < BUFFER) {
					bytesToRead = bytesLeft;
				}
			} while ((count = mDataReader.read(data, 0, bytesToRead)) != -1);
			
			out.flush();
			
		} catch (IOException e) {
			System.out.println("End of Stream Detected by Timeout :( c:"+count+":fl:"+fileLength+":r:"+bytesSoFar);
//			Constants.LOGGER.finest("Connection_READER_TIMETAKEN : HERE :"+e.getMessage());
//			e.printStackTrace();
		}
		finally{
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Compression.decompressFile(fileLocation, decompressedFileLocation);
//		new File(fileLocation).delete();
		if(new File(fileLocation+".old").exists()) new File(fileLocation+".old").delete();
		new File(fileLocation).renameTo(new File(fileLocation+".old"));//renaming the received zip file
	}
	*/
}