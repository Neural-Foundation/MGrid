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
package pgrid.interfaces.basic;

import p2p.basic.*;
import pgrid.PGridKey;
import pgrid.PGridHost;
import pgrid.PGridKeyRange;
import pgrid.core.PGridTree;
import pgrid.network.protocol.GenericMessage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.io.File;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import mgrid.core.Point;

/**
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.2.0
 */
public class PGridP2PFactory extends P2PFactory {

	/**
	 * The reference to the only instance of this class (Singleton
	 * pattern). This differs from the C++ standard implementation by Gamma
	 * et.al. since Java ensures the order of static initialization at runtime.
	 *
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	private static final PGridP2PFactory SHARED_INSTANCE = new PGridP2PFactory();

	/**
	 * The P-Grid facility.
	 */
	private PGridP2P mPGridP2P = null;

	/**
	 * The PGrid hashing function.
	 */
	private PGridTree mHasher = new PGridTree();

	/**
	 * The constructor must be protected to ensure that only subclasses can
	 * call it and that only one instance can ever get created. A client that
	 * tries to instantiate PGridIndexManager directly will get an error at compile-time.
	 */
	protected PGridP2PFactory() {
		mHasher.init();
	}

	/**
	 * This creates the only instance of this class. This differs from the C++ standard implementation by Gamma et.al.
	 * since Java ensures the order of static initialization at runtime.
	 *
	 * @return the shared instance of this class.
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	public static PGridP2PFactory sharedInstance() {
		return SHARED_INSTANCE;
	}

	/**
	 * @see p2p.basic.P2PFactory#createP2P(java.util.Properties)
	 */
	public P2P createP2P(Properties properties) {
		// start the P-Grid facility
		mPGridP2P = PGridP2P.sharedInstance();
		mPGridP2P.init(properties);

		return mPGridP2P;
	}

	/**
	 * @see p2p.basic.P2PFactory#generateKey(java.lang.Object)
	 */
	public Key generateKey(Object obj) {
	//	return new PGridKey(mHasher.findKey((String)obj));
		if (obj instanceof Point)
		return new PGridKey(mHasher.findKey((Point)obj));
		else
			return new PGridKey(mHasher.findKey((String)obj));
	}

	/**
	 * Generate a KeyRange instance compatible with the P2P implementation.
	 * Acceptable source object types depend on implementation.
	 *
	 * @param lowerBound the source object from which to generate the lower key
	 * @param lowerBound the source object from which to generate the higher key
	 * @return the generated Key implementation
	 */
	public KeyRange generateKeyRange(Object lowerBound, Object higherBound) {
		return new PGridKeyRange(generateKey(lowerBound), generateKey(higherBound));
	}

	/**
	 * @see p2p.basic.P2PFactory#generateGUID()
	 */
	public GUID generateGUID() {
		return pgrid.GUID.getGUID();
	}

	/**
	 * @see p2p.basic.P2PFactory#createMessage(byte[])
	 */
	public Message createMessage(byte[] contents) {
		return new GenericMessage(contents);
	}

	/**
	 * @see p2p.basic.P2PFactory#createPeer(java.net.InetAddress, int)
	 */
	public Peer createPeer(InetAddress netAddr, int port) throws UnknownHostException {
		if (netAddr == null)
			throw new NullPointerException();
		if (port < 0)
			throw new IllegalArgumentException("port is negative");

		PGridHost host = PGridHost.getHost(netAddr, port, false);
		host.resolve();

		return host;
	}

	/**
	 * Set properties file
	 */
	public void setIniFile(String inifile) {
		pgrid.Constants.PROPERTY_FILE=inifile;
	}

	/**
	 * Set data path
	 */
	public void setDataPath(String datapath) {

		pgrid.Constants.DATA_DIR=datapath;
		if (!datapath.endsWith(File.separator)) {
			pgrid.Constants.DATA_DIR = pgrid.Constants.DATA_DIR+File.separator;
		}
	}


	/**
	 * Set log paths
	 */
	public void setLogPath(String logpath) {
		pgrid.Constants.LOG_DIR=logpath;
		if (!logpath.endsWith(File.separator)) {
			pgrid.Constants.LOG_DIR = pgrid.Constants.LOG_DIR+File.separator;
		}
	}


}
