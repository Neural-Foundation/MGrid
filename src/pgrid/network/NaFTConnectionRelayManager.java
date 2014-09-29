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
import java.io.IOException;
import java.util.ArrayList;


public class NaFTConnectionRelayManager {


	private static ArrayList<String> currentRelays = new ArrayList<String>();

	private static final NaFTConnectionRelayManager instance = new NaFTConnectionRelayManager();
	
	private NaFTConnectionRelayManager(){
		
	}
	
	public static NaFTConnectionRelayManager shared_instance(){
		return instance;
	}

	
	private boolean isAnotherRelayActive(Connection connIn, Connection connOut){
		String relayStr1 = connIn.getGUID().toString() + connOut.getGUID().toString();
		String relayStr2 = connOut.getGUID().toString() + connIn.getGUID().toString();
		
		NaFTManager.LOGGER.fine("Checking for active relay: " + relayStr1 + " / " + relayStr1 + "\nCurrent running relays:\n");
		
		for (String rel : currentRelays) NaFTManager.LOGGER.fine(rel);
		
		if (currentRelays.contains(relayStr1) || currentRelays.contains(relayStr2)) return true;
		return false;
	}
	
	
	public void startRelay(Connection connIn, Connection connOut, int totalBytesToRelay){

		String relayStr = connIn.getGUID().toString() + connOut.getGUID().toString();
		NaFTManager.LOGGER.fine("Starting a relay: " + relayStr + " / " + connOut.getGUID().toString() + connIn.getGUID().toString());
		while (isAnotherRelayActive(connIn, connOut)){
			try {
				NaFTManager.LOGGER.fine("Another relay is active for these connections. Waiting a bit ...");
				Thread.sleep(150);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		currentRelays.add(relayStr);

		//new  Thread(new NaFTConnectionRelay(connIn, connOut, totalBytesToRelay)).start();
		relay(connIn, connOut, totalBytesToRelay);
		
	}
	
	public void stopRelay(Connection connIn, Connection connOut){
		String relayStr1 = connIn.getGUID().toString() + connOut.getGUID().toString();
		String relayStr2 = connOut.getGUID().toString() + connIn.getGUID().toString();
		
		NaFTManager.LOGGER.fine("Stopping relay: " + relayStr1 + " / " +  relayStr2); 
		if (currentRelays.contains(relayStr1)) currentRelays.remove(relayStr1);
		if (currentRelays.contains(relayStr2)) currentRelays.remove(relayStr2);
		
	}
	
	private void relay(Connection connIn, Connection connOut, int totalBytesToRelay){

    	int BUFFER = 256;
		byte[] data = new byte[BUFFER];
		BufferedOutputStream out = null;
		BufferedInputStream in = null;

		int count=0, bytesSoFar = 0;
		int bytesToRead = BUFFER;
		int bytesLeft = totalBytesToRelay;
		try {

			NaFTManager.LOGGER.fine("Starting TCP connection relay from " + connIn.getSocket().getRemoteSocketAddress() + 
					" to " + connOut.getSocket().getRemoteSocketAddress() + " with " + totalBytesToRelay + " bytes to transfer.");
			
			in = new BufferedInputStream(connIn.getSocket().getInputStream());
			out = new BufferedOutputStream(connOut.getSocket().getOutputStream());
			
			do {
				NaFTManager.LOGGER.fine("Writing " + count + " bytes ...");
				out.write(data,0,count);
				out.flush();
				
				bytesSoFar += count;
				connOut.resetIOTimer();
				
				bytesLeft -= count;
				
				if (bytesLeft == 0) break;
				
				if (bytesLeft < BUFFER) {
					bytesToRead = bytesLeft;
				}
			} while ((count = in.read(data, 0, bytesToRead)) != -1);
	
			out.flush();
			//out.close();
			//in.close();
			
			NaFTManager.LOGGER.fine("TCP connection relay has successfully transferred " + bytesSoFar + " bytes. (File size = " + totalBytesToRelay + " bytes)");
			
		} catch (IOException ioe ){
			ioe.printStackTrace();
		}
	
		stopRelay(connIn, connOut);
	}

}


class NaFTConnectionRelay implements Runnable {

	private Connection connIn, connOut;
	private int totalBytesToRelay;
	
	public NaFTConnectionRelay(Connection in, Connection out, int totalBytesToRelay){
		this.connIn = in;
		this.connOut = out;
		this.totalBytesToRelay = totalBytesToRelay;
	}
	
	public void run() {

    	int BUFFER = 256;
		byte[] data = new byte[BUFFER];
		BufferedOutputStream out = null;
		BufferedInputStream in = null;

		int count=0, bytesSoFar = 0;
		int bytesToRead = BUFFER;
		int bytesLeft = totalBytesToRelay;
		try {

			NaFTManager.LOGGER.fine("Starting TCP connection relay from " + connIn.getSocket().getRemoteSocketAddress() + 
					" to " + connOut.getSocket().getRemoteSocketAddress() + " with " + totalBytesToRelay + " bytes to transfer.");
			
			in = new BufferedInputStream(connIn.getSocket().getInputStream());
			out = new BufferedOutputStream(connOut.getSocket().getOutputStream());
			
			do {
				NaFTManager.LOGGER.fine("Writing " + count + " bytes ...");
				out.write(data,0,count);
				out.flush();
				
				bytesSoFar += count;
				connOut.resetIOTimer();
				
				bytesLeft -= count;
				
				if (bytesLeft == 0) break;
				
				if (bytesLeft < BUFFER) {
					bytesToRead = bytesLeft;
				}
			} while ((count = in.read(data, 0, bytesToRead)) != -1);
	
			out.flush();
			//out.close();
			//in.close();
			
			NaFTManager.LOGGER.fine("TCP connection relay has successfully transferred " + bytesSoFar + " bytes. (File size = " + totalBytesToRelay + " bytes)");
			
		} catch (IOException ioe ){
			ioe.printStackTrace();
		}
	
		NaFTConnectionRelayManager.shared_instance().stopRelay(connIn, connOut);
	}
	
}