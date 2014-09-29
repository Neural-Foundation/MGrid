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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

import pgrid.Constants;
import pgrid.util.Compression;

/**
 * The Communication Writer provides basic functions to write messages to an
 * Output Stream.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
class ConnectionWriter {

	/**
	 * The data output stream writer.
	 */
	private DataOutputStream mDataWriter = null;

	/**
	 * The output stream.
	 */
	private OutputStream mOut = null;

	/**
	 * Creates a writer.
	 *
	 * @param out the Output Stream.
	 */
	ConnectionWriter(OutputStream out) {
		mOut = out;
		mDataWriter = new DataOutputStream(new BufferedOutputStream(mOut));
	}

	/**
	 * Writes an array of bytes to the Output Stream.
	 *
	 * @param data the array of bytes.
	 * @throws IOException
	 */
	void write(byte[] data) throws IOException {
		mDataWriter.write(data, 0, data.length);
		mDataWriter.flush();
	}

	/**
	 * Writes an array of bytes to the Output Stream.
	 *
	 * @param data  the array of bytes.
	 * @param start the first byte to write.
	 * @param len   the length to write.
	 * @throws IOException
	 */
	void write(byte[] data, int start, int len) throws IOException {
		mDataWriter.write(data, start, len);
		mDataWriter.flush();
	}

//	/**
//	* Given file location this function writes the contents from this file to the network output stream
//	* 
//	* @param fileLocation
//	*/
//	void writeFromFile(String fileLocation){
//	int i = 0;
//	BufferedReader in = null;
//	String line = "";
//	try {
////	System.out.println("Writing the file to the network stream");
//	in = new BufferedReader(new FileReader(fileLocation));
//	while( (line = in.readLine()) != null){
//	i++;
//	mDataWriter.writeUTF(line);
//	mDataWriter.flush();
////	if(i%1000==0){
////	System.out.println((line).length()+"Read line: "+i);
////	}
//	}
//	mDataWriter.flush();
////	System.out.println("Writing done from Client side!!!!");

//	} catch (FileNotFoundException e) {
//	// TODO Auto-generated catch block
//	e.printStackTrace();
//	} catch (IOException e) {
//	// TODO Auto-generated catch block
////	System.out.println(line);
////	System.out.println(line.length());
//	e.printStackTrace();
//	}
//	finally{
//	try {
//	in.close();
//	} catch (IOException e) {
//	// TODO Auto-generated catch block
//	e.printStackTrace();
//	}
//	}

//	}

	
	
	/**
	 * Sending a file over the network.
	 * @throws Exception 
	 */
	void writeFileToStream(String filePath, boolean compressed) throws Exception{
		
		String fileLocation = filePath;
		if (compressed) fileLocation += ".zip";

		int BUFFER = 256;
		byte[] data = new byte[BUFFER];
		BufferedInputStream bis = null;
		int count=0,bytesWritten=0;
		try {
			bis = new BufferedInputStream(new FileInputStream(fileLocation),BUFFER);

			while( (count = bis.read(data, 0, BUFFER)) != -1){
				bytesWritten+=count;
				mDataWriter.write(data,0,count);
				mDataWriter.flush();
			}
			mDataWriter.flush();

		} catch (FileNotFoundException e) {
			Constants.LOGGER.warning("Trying to send a file which doesnot exists :"+new File(fileLocation).getName());
			System.out.println("Trying to send a file which doesnot exists :"+new File(fileLocation).getName());
			throw e;
//			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		} finally{
			try {
				bis.close();
				if (compressed) {
//					if(new File(fileLocation+".old").exists()) new File(fileLocation+".old").delete();
//					new File(fileLocation).renameTo(new File(fileLocation+".old"));//renaming the sent zipped file
					new File(fileLocation).delete();
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Given file location this function writes the contents from this file to the network output stream
	 * 
	 * @param fileLocation
	 * @throws IOException 
	 */
	/*
	void writeFromFile(Connection mConn, String fileName) throws IOException{

		String compressedFileLocation = Constants.CSV_DIR+fileName+".zip";
//		Compression.compressFile(fileLocation, compressedFileLocation);
		long t = System.currentTimeMillis();		
		int BUFFER = 256;
		byte[] data = new byte[BUFFER];
		BufferedInputStream origin = null;
		int count=0,bytesWritten=0;
		try {
			origin = new BufferedInputStream(new FileInputStream(compressedFileLocation),BUFFER);

//			mConn.getSocket().setSoTimeout(0);
			while( (count = origin.read(data, 0, BUFFER)) != -1){
				bytesWritten+=count;
				mDataWriter.write(data,0,count);
				mConn.resetIOTimer();

				mDataWriter.flush();
			}
			mDataWriter.flush();
			//System.out.println("Number Of Bytes Written: "+bytesWritten);
//			mConn.getSocket().shutdownOutput();
			Constants.LOGGER.finest("Connection_WRITER_TIMETAKEN: it tooks " + (System.currentTimeMillis()-t) + " to write " + bytesWritten + " bytes.");

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			try {
				origin.close();
//				new File(compressedFileLocation).delete();
				if(new File(compressedFileLocation+".old").exists()) new File(compressedFileLocation+".old").delete();
				new File(compressedFileLocation).renameTo(new File(compressedFileLocation+".old"));//renaming the sent zipped file
//				mDataWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	*/

}