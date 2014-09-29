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
import pgrid.interfaces.basic.PGridP2P;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

/**
 * The communication listener accepts new incoming connections from remote
 * hosts.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
class Listener implements Runnable {

	/**
	 * The listen flag.
	 */
	private boolean listening = true;

	/**
	 * The pause flag.
	 */
	private boolean pause = false;

	/**
	 * The PGridP2P facility.
	 */
	private PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 * The socket to listen.
	 */
	private ServerSocket mSocket;

	/**
	 * Creates a new listener.
	 */
	public Listener() {
	}

	/**
	 * Starts the listener. A new socket is created at a definied port (config
	 * facility).
	 */
	public void run() {
		connect();
		
		ConnectionManager connMgr = ConnectionManager.sharedInstance();
		Constants.LOGGER.info("start listening for incoming connections at port " + String.valueOf(mPGridP2P.getLocalHost().getPort()) + " ...");
		while (listening) {
			try {
				Socket s = mSocket.accept();

				if (!pause)
					connMgr.accept(s);
				else s.close();

			} catch (IOException e) {
				//Constants.LOGGER.log(Level.WARNING, null, e);
			}
		}
		try {
			mSocket.close();
		} catch (IOException e) {
			Constants.LOGGER.log(Level.SEVERE, null, e);
			System.exit(-1);
		}
	}

	/**
	 * Stop listening
	 */
	public void stopListening() {
		pause = true;
		try {
			if (mSocket != null)
				mSocket.close();
		} catch (IOException e) {

		}
	}

	/**
	 * Restart listening
	 */
	public void restartListening() {
		pause = false;
		connect();
	}

	public boolean isListenning(){
		if (mSocket == null) return false;
		if (mSocket.isClosed()) return false;
		return mSocket.isBound();
	}
	
	private void connect() {

        int port = mPGridP2P.getLocalHost().getPort();
        mSocket = PGridSocket.sharedInstance().getServerSocket(port);

        //TODO: this was the old code- why we are exit() ing and continueing when port is used by another app
        //TODO: the same functionality (while loop) should be continued in socket creation classes too?
/*		while (true) {
                mSocket = PGridSocket.sharedInstance().getServerSocket(port);
			} catch (BindException e) {
				Constants.LOGGER.warning("Port " + String.valueOf(mPGridP2P.getLocalHost().getPort()) + " is already used by another application!");
				System.exit(-1);
				continue;
			} catch (IOException e) {
				Constants.LOGGER.log(Level.SEVERE, null, e);
				System.exit(-1);
			}
			break;
		}*/
	}

}