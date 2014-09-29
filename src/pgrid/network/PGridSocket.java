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

import java.net.ServerSocket;
import java.net.Socket;
import java.net.BindException;
import java.net.InetAddress;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Created by IntelliJ IDEA.
 * User: narendul
 * Date: Jul 26, 2007
 * Time: 6:23:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class PGridSocket {
    private static final PGridSocket SHARED_INSTANCE = new PGridSocket();
    private boolean mUseSSLSockets = false;
    private PGridP2P mPGridP2P = PGridP2P.sharedInstance(); 

    /**
	 * The constructor must be protected to ensure that only subclasses can
	 * call it and that only one instance can ever get created. A client that
	 * tries to instantiate PGridSocket directly will get an error at compile-time.
	 */

    protected PGridSocket(){
        };

    public static PGridSocket sharedInstance(){
        return SHARED_INSTANCE;
    }

    /*
     Just store whether to use SSL Sockets or normal sockets in a member variable
    Instantiate the PGridSSL class if SSL Sockets are required.
   */
    public void init()
    {
       mUseSSLSockets = mPGridP2P.propertyBoolean(Properties.USE_SSLSOCKETS);        
       if (mUseSSLSockets)
       {
           PGridSSL.sharedInstance().init();
       }
    }

    public ServerSocket getServerSocket(int port)
    {
        try {
            return (mUseSSLSockets ? (PGridSSL.sharedInstance().getSSLServerSocket(port)) : (new ServerSocket(port)));
        }

        catch (BindException e) {
            Constants.LOGGER.warning("Port " +
                    String.valueOf(mPGridP2P.getLocalHost().getPort()) +
                    " is already used by another application!");
            System.exit(-1);
        }

        catch (IOException e) {
            Constants.LOGGER.log(Level.SEVERE, null, e);            
            System.exit(-1);
        }
        return null;
    }

    /*
       the exceptions are not handled intentionally. They are better handled in Connector.java
     */
    public Socket getSocket(InetAddress adr, int port) throws IOException, BindException {
        return (mUseSSLSockets ? (PGridSSL.sharedInstance().getSSLSocket(adr,port)) : (new Socket(adr,port)));
    }

}
