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
package test;

import pgrid.interfaces.basic.PGridP2P;
import pgrid.interfaces.basic.PGridP2PFactory;
import pgrid.interfaces.index.PGridIndexFactory;
import pgrid.interfaces.index.DefaultTypeHandler;
import pgrid.PGridHost;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Vector;

import p2p.basic.Peer;
import p2p.basic.P2PFactory;
import p2p.basic.P2P;
import p2p.index.*;

/**
 * Class description goes here
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public class InsertTest {

	private P2PFactory p2pFactory;
	private P2P p2p;

	private IndexFactory indexFactory;
	private Index index;

	public void run(InetAddress bootIP, int bootPort, Properties properties) {
		/** P2P INITIALIZATION **/

		// Set the debug mode to the minimum. Debug can be set to a number between 0-3
		properties.setProperty(PGridP2P.PROP_DEBUG_LEVEL, "3");
		// Use a verbose mode
		properties.setProperty(PGridP2P.PROP_VERBOSE_MODE, "false");

		// Get an instance of the P2PFactory
		p2pFactory = PGridP2PFactory.sharedInstance();
		System.out.println("Acquired P-Grid factory reference. ");

		// Get an instance of the P2P object, aka P-Grid
		p2p = p2pFactory.createP2P(properties);
		System.out.println("Created a P2P instance. ");

		// Create an instance of the bootstrap host that will be use to bootstrap the
		// network
		Peer bootstrap = null;
		try {
			bootstrap = p2pFactory.createPeer(bootIP, bootPort);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		System.out.println("Bootstrap Peer instance Created.");

		// Get an instance of the StorageFactory
		indexFactory = PGridIndexFactory.sharedInstance();
		System.out.println("Storage factory reference acquired. ");


		// Get an instance of the Storage object, aka PGridStorage
		index = indexFactory.createIndex(p2p);
		System.out.println("Storage instance acquired. ");

		// creating and registering data type.
		Type type = indexFactory.createType("SimpleType");
		TypeHandler handler = new DefaultTypeHandler(type);
		indexFactory.registerTypeHandler(type, handler);


		System.out.println("Network joined. Current key range (path) is: " + ((PGridHost)p2p.getLocalPeer()).getPath());

		Vector<IndexEntry> entries = new Vector<IndexEntry>();
		String data;
		IndexEntry entry;
		int number = 10000;
		int counter = 10;

		for (int k=0; k< counter;k++) {
			for (int i=0; i<number; i++) {

				data = "This is a data object.";

				// create a data entry object through the storage interface
				entry = indexFactory.createIndexEntry(type, data);
				entries.add(entry);
			}
			index.insert(entries);
			entries = new Vector<IndexEntry>();
			System.out.println("Inserted "+number+" index entries.");
		}

		p2p.leave();
		index.shutdown();

	}

	/**
	 * Let the sample peer send a message.
	 *
	 * @param args the command line arguments to be passed on to the peer-to-peer layer
	 * @see test.CommandLineArgs usage
	 */
	public static void main(String[] args) {
		test.CommandLineArgs cla = new test.CommandLineArgs(args);
		InsertTest storage = new InsertTest();
		storage.run(cla.getAddress(), cla.getPort(), cla.getOtherProperties());
	}
}
