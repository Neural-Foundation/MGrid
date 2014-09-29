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

import javax.net.ssl.*;
import javax.net.ServerSocketFactory;
import java.security.*;
import java.security.cert.CertificateException;
import java.io.*;
import java.net.InetAddress;
import java.net.BindException;
import java.util.logging.Level;

/**
 * Created by IntelliJ IDEA.
 * User: narendul
 * Date: May 7, 2007
 * Time: 6:14:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class PGridSSL {

    private static final PGridSSL SHARED_INSTANCE =  new PGridSSL();

    private char[] keyStorePwd;
    private char[] trustStorePwd;

    SSLServerSocketFactory serverSocketFactory;
    SSLServerSocket serverSocket;

    SSLSocketFactory socketFactory;
    SSLSocket socket;

    SSLContext ctx;
    TrustManagerFactory tmf;
    KeyManagerFactory kmf;
    KeyStore ks1, ks2;

    /**
	 * The constructor must be protected to ensure that only subclasses can
	 * call it and that only one instance can ever get created. A client that
	 * tries to instantiate PGridSSL directly will get an error at compile-time.
	 */
	protected PGridSSL() {
	}

    public static PGridSSL sharedInstance(){
        return SHARED_INSTANCE;
    }

    public void init() {
        //Read both passwords
 /*       //TODO password has to be supplied by the application...so try removing it from here
        System.out.println("Enter KeyStore Password");
        System.out.flush();
        BufferedReader d = new BufferedReader(new InputStreamReader(System.in));
        try {
            keyStorePwd = d.readLine().toCharArray();
            //System.out.println("Enter TrustStore Password");
            //If same keystore is used as truststore, then simply copy the pwd or read it separately
            trustStorePwd = keyStorePwd;            
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }*/
        /* TODO: Converting strings to char array mystifies the things esp w.r.t passwords.
         * TODO: This conversion is not proper in general. Reading a pwd into char array is preferred.
         * TODO: So change it so in future if problems occur.
         */

        keyStorePwd = PGridP2P.sharedInstance().propertyString(Properties.SSL_KEYSTORE_PASSWORD).toCharArray();
        trustStorePwd = PGridP2P.sharedInstance().propertyString(Properties.SSL_TRUSTSTORE_PASSWORD).toCharArray();
        try {
            //Create an SSLContext instance implementing the TLS protocol
            ctx = SSLContext.getInstance("TLS");
            //Create a TrustManagerFactory implementing the X.509 key management algo
            tmf = TrustManagerFactory.getInstance("SunX509");
            //Create a KeyStore instance implementing the JavaKeyStore algorithm
            ks1 =  KeyStore.getInstance("JKS");
            //Load the KeyStore file "myTrustStoreFile"
            //ks.load(new FileInputStream("C:\\ramKeyStores\\myTrustStoreFile"),passwdTrustStore);
            //TODO: the key store file with the following name should be present in the current work directory
            //TODO: change it the application provided location...future TODO
            ks1.load(new FileInputStream("myKeyStoreFile"),trustStorePwd);
            //ks.load(new FileInputStream("C:\\Program Files\\Java\\jre1.6.0\\lib\\security\\cacerts"),passwdTrustStore);
            //Init the TrustManagerFactory object with this keystore
            tmf.init(ks1);

           //Since we need to authenticate with the other end, we have to present our certificates
           //Create a KeyManagerFactory implementing the X.509 key management algorithm
            kmf = KeyManagerFactory.getInstance("SunX509");
           //Create a KeyStore instance implementing the JKS algo
            ks2 = KeyStore.getInstance("JKS");
            //Load the key store file "myKeyStoreFile"
            ks2.load(new FileInputStream("myKeyStoreFile"),keyStorePwd);
            //Init the keymanagerfactory
            kmf.init(ks2,keyStorePwd);
            //Init the SSLContext with the Key and Trust manager factories
            ctx.init(kmf.getKeyManagers(),tmf.getTrustManagers(),null);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (KeyManagementException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (CertificateException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (KeyStoreException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public SSLServerSocket getSSLServerSocket(int port) throws IOException {
        serverSocketFactory = ctx.getServerSocketFactory();        
        return (SSLServerSocket) serverSocketFactory.createServerSocket(port);
    }
    
    public SSLSocket getSSLSocket(InetAddress adr, int port) throws IOException {
        socketFactory = ctx.getSocketFactory();
        return (SSLSocket) socketFactory.createSocket(adr,port);
    }
}