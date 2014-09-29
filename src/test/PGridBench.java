package test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Properties;
import ch.epfl.lsir.nbench.basic.GUID;
import ch.epfl.lsir.nbench.client.NBClient;
import ch.epfl.lsir.nbench.client.events.PGridBenchmarkListener;
import p2p.basic.Message;
import p2p.basic.P2P;
import p2p.basic.P2PFactory;
import p2p.basic.Peer;
import p2p.basic.events.P2PListener;
import p2p.index.Index;
import p2p.index.IndexEntry;
import p2p.index.IndexFactory;
import p2p.index.Type;
import p2p.index.events.DownloadListener;
import p2p.index.events.SearchListener;
import pgrid.Constants;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.interfaces.basic.PGridP2PFactory;
import pgrid.interfaces.index.PGridIndexFactory;
import pgrid.util.Tokenizer;
import pgrid.util.monitoring.MonitoringManager;
import pgrid.core.index.CSVIndexTable;
import test.gridella.GridellaTesterFiles;
import test.gridella.XMLFileIndexEntry;
import pgrid.network.protocol.*;
import pgrid.network.router.MessageWaiter;

public class PGridBench implements P2PListener, PGridBenchmarkListener, MessageWaiter, DownloadListener {
	
	
	private boolean writeDirectlyToDB = false;
	private boolean CSV_WAY = true;
	private boolean logInsertedFiles = true;
	private boolean monitored = true;
	
	private int BATCH_SIZE = 5000;
	private boolean preInsert = true;
	private static int preInsertAmount = 10000;
	
	private final String FILES_LOG = Constants.LOG_DIR + "inserted_files.log";
	
	private File log;
	
	private P2PFactory p2pFactory;
	private P2P p2p;
	private NBClient client;

	private IndexFactory indexFactory;
	private Index index;
	
	private int localPort;
	private int NBClientPort;
	
	private String monitorHost;
	private int monitorPort;
	private String bootstrapHost;
	
	private int replicationFactor = 1;
	
	GridellaTesterFiles testFiles = new GridellaTesterFiles();
	
	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.out.println("Usage: program  localPort NBClientPort preInsertionAmount");
			System.exit(0);
		}
		
		System.out.println("PGrid.main(): Start P-Grid Benchmark node ... ");
		
		int lPort = Integer.parseInt(args[0]);
		int nPort = Integer.parseInt(args[1]);
		preInsertAmount = Integer.parseInt(args[2]);
		
		String config = "/config.properties";
		
//		if (args.length == 3){
//			config = "/" + args[2];
//		}
		

		@SuppressWarnings("unused")
		PGridBench storage = new PGridBench(lPort, nPort, config);
		
		/*
		Object waiter = new Object();
		synchronized (waiter) {
			waiter.wait();
		}	
		*/
	
		
	}
	

	
	PGridBench(int localPort, int NBClientPort, String configFile) throws UnknownHostException{
		
		Properties properties = new Properties();
	    try {
	        properties.load(this.getClass().getResourceAsStream(configFile));
	        
	    } catch (IOException e) {
	    	System.out.println("Config file not found: " + e.getMessage());
	    	System.exit(0);
	    }

		this.monitorHost = properties.getProperty("nbench.monitorhost");
		this.monitorPort = Integer.parseInt(properties.getProperty("nbench.monitorport"));
		this.bootstrapHost = properties.getProperty("pgrid.bootstraphost");
		this.localPort = localPort;		
		this.NBClientPort = NBClientPort;
		
		System.out.println("monitorHost: " + this.monitorHost);
		System.out.println("monitorPort: " + this.monitorPort);
		System.out.println("boostrapHost: " + this.bootstrapHost);
		System.out.println("preInsertion Amount: " + PGridBench.preInsertAmount);

		this.run();
	}
	
	
	//public void run(InetAddress bootIP, int bootPort) {
	public void run() {
		Constants.LOGGER.finest("BENCH: Startss..");
		/** P2P INITIALIZATION **/

		Properties properties = new Properties();
		// Set the debug mode to the minimum. Debug can be set to a number between 0-3
		properties.setProperty(PGridP2P.PROP_DEBUG_LEVEL, "0");
		// Use a verbose mode
		properties.setProperty(PGridP2P.PROP_VERBOSE_MODE, "false");
		// Set local port
		properties.setProperty(PGridP2P.PROP_LOCAL_PORT, String.valueOf(this.localPort));
		// Set replication factor
		properties.setProperty(PGridP2P.PROP_REPLICATION_FACTOR, String.valueOf(this.replicationFactor));
		// Set debug mode
		properties.setProperty(pgrid.Properties.DEBUG_MODE, "true");
		// Set test mode
		properties.setProperty(pgrid.Properties.TEST_MODE, "true");
		// Set Monitor mode
		properties.setProperty(pgrid.Properties.MONITORED_MODE, Boolean.toString(monitored));
		// Set Construction Time
		properties.setProperty(pgrid.Properties.CONSTRUCTION_START_TIME, "180000");
		// Set Properties File
		properties.setProperty(PGridP2P.PROP_PROPERTY_FILE, "PGrid.ini");
		// Set SuperPeer
		properties.setProperty(pgrid.Properties.SUPER_PEER, "true");
		// Set Bootstrap host
		properties.setProperty(pgrid.Properties.BOOTSTRAP_HOSTS, this.bootstrapHost);
		// Set maximum number of Fidget
		properties.setProperty(pgrid.Properties.MAX_FIDGETS, "10");
		

		// Get an instance of the P2PFactory
		p2pFactory = PGridP2PFactory.sharedInstance();
		System.out.println("Acquired P-Grid factory reference. ");
		
		// Get an instance of the P2P object, aka P-Grid
		p2p = p2pFactory.createP2P(properties);
		System.out.println("Created a P2P instance. ");
		
		// Get an instance of the StorageFactory
		indexFactory = PGridIndexFactory.sharedInstance();
		System.out.println("Storage factory reference acquired. ");
		
		// Get an instance of the Storage object, aka PGridStorage
		index = indexFactory.createIndex(p2p);
		System.out.println("Storage instance acquired. ");

		// Set log file
		log = new File(FILES_LOG);
		
		type = PGridIndexFactory.sharedInstance().createType("SimpleType");
		
		/*
		
		WaitingArea.waitTillSignal(1000 * 10);
		System.out.println("Pre-inserting 1000 items ...");
		preInsertion(100);
		System.out.println("Preinsertion completed.");
		WaitingArea.waitTillSignal(1000 * 20 * 1);

		*/
		
		Constants.LOGGER.finest("BENCH: Just before starting PGrid");
		
		// Try to join the network
		PGridP2P pgrid = PGridP2P.sharedInstance();
		
		//preInsert before joining the network
		if(preInsert){
			preInsertion(preInsertAmount);
		}

		//		// join the network
		pgrid.join();
		
		Constants.LOGGER.finest("BENCH: Just after joining PGrid");
		
		// add this object as a p2p listener
		p2p.addP2PListener(this);

		pgrid.getDownloadManager().addDownloadListener(this);
		
//		 Each client should be able to provide a GUID for the overlay
		client = new NBClient(p2p.getLocalPeer().getGUID().toString(), this.NBClientPort, monitorHost, monitorPort);
		client.addBenchmarkListener(this);
		if (monitored)	{
			System.out.println("Monitoring activated !");
			MonitoringManager.init(client);
		}

		
//		/*
//		// Sending a connection reversal init message
//		
//		PGridHost destination = null;
//		PGridHost relay = null;
//		try {
//			relay = PGridHost.getHost(InetAddress.getByName("128.178.151.218"), 2000);
//			destination = PGridHost.getHost(InetAddress.getByName("10.0.2.15"), 1805);
//		} catch (UnknownHostException e1) {
//			e1.printStackTrace();
//		}
//		NaFTConnectionReversalInitMessage message = new NaFTConnectionReversalInitMessage(destination);
//		message.getHeader().setDestinationHost(new XMLPGridHost(destination));
//
//		MessageManager.sharedInstance().sendMessage(relay, message, this);
//		System.out.println("Sending NaFTConnectionReversalInitMessage ... ");
//		*/
//		
//		/*
//		// Sending a direct message to a NATed peer
//		GenericMessage msg = new GenericMessage("hello".getBytes());
//		PGridHost destination = null;
//		try {
//			destination = PGridHost.getHost(InetAddress.getByName("10.0.2.15"), 1805);
//		} catch (UnknownHostException e1) {
//			e1.printStackTrace();
//		}
//		
//		MessageManager.sharedInstance().sendMessage(destination, msg, this);
//		System.out.println("Sending a direct message to 10.0.2.15 ... ");
//		
//		*/
//		
//		
//		/**
//		 * Create a test file to download
//		 */
//		if (this.localPort == 1805){
//			File f = new File(Constants.DOWNLOAD_DIR);
//			if (!f.exists()) f.mkdirs();
//			
//			try {
//			FileWriter fstream = new FileWriter(Constants.DOWNLOAD_DIR + "test.txt");
//		    BufferedWriter out = new BufferedWriter(fstream);
//		    for (int i=0; i<10000; i++) out.write("Hello Java\n");
//		    //Close the output stream
//		    out.close();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		
//		}
//
//		/*
//		 * Sending a GetFile Message 
//		 */
//		if (this.localPort == 1806) {
//			PGridHost destination = null;
//			try {
//				destination = PGridHost.getHost(InetAddress.getByName("128.178.151.218"), 1805);
//			} catch (UnknownHostException e1) {
//				e1.printStackTrace();
//			}
//			System.out.println("PGrid.run(): sending GetFile Message ...");
//			pgrid.send(destination, new GetFile("../../../test.txt"));
//		} else {
//			System.out.println("PGrid.run(): this host will not send a GetFile message.");
//		}
		
		
		((PGridP2P)p2p).setInitExchanges(true);
		
		Object waiter = new Object();
		synchronized (waiter) {
			try {
				waiter.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
		
		System.out.println("shutdown ...");
		p2p.leave();
		index.shutdown();

	}
	
	public void preInsertion(int amount){
		System.out.println("PreInsertion: "+amount);
		
		//PreInsertion.. We want to insert directly in to the CSV
		CSV_WAY = true;

		if (CSV_WAY){
			CSVIndexTable localStore = new CSVIndexTable(true);
			try {
				localStore.openFileForWriting();
				for (int j = 0; j < amount; j++) {
					localStore.addIndexEntry((pgrid.IndexEntry)randomIndexEntry(j));				
				}
			} catch (Exception e){
				localStore.closeFileForWriting();
			}
			
		} else {
//			System.out.println("Network Insertion :"+entries.size());
//			index.insert(entries);
		}

//		this.insert(keys,values);
//		//After PreInsertion.. We want to insert directly in to the Network
		CSV_WAY  = false;
		return;

		
		/*
		String[] filenames = Files.getFilenames(amount);
		byte[][] keys = new byte[amount][];
		byte[][] values = new byte[amount][];
		for (int i=0; i<filenames.length; i++){
			keys[i] = new byte[filenames[i].getBytes().length];
			values[i] = new byte[filenames[i].getBytes().length];
			keys[i] = filenames[i].getBytes();
			values[i] = filenames[i].getBytes();			
		}
		*/
		
		
		/*
		ArrayList<test.nepomuk.Pair> filenames = new ArrayList<test.nepomuk.Pair>();
		filenames = NepomukDataTester.getKeyValuePairs(amount);
		byte[][] keys = new byte[amount][];
		byte[][] values = new byte[amount][];
		
		for (int i=0; i<filenames.size(); i++){
			keys[i] = new byte[filenames.get(i).key.getBytes().length];
			values[i] = new byte[filenames.get(i).value.getBytes().length];
			keys[i] = filenames.get(i).key.getBytes();
			values[i] = filenames.get(i).value.getBytes();
		}
		this.insert(keys,values);
		
		*/
		
		
//		int items = amount;
//		
//		int size = 0;
//		boolean stop = false;
//		while (true){
//			
//			if ((items -= BATCH_SIZE) > 0)	size = BATCH_SIZE;
//			else {
//				size = amount % BATCH_SIZE;
//				stop = true;
//			}
//						
//			byte[][] keys = new byte[size][];
//			byte[][] values = new byte[size][];
//			
//			ArrayList<test.nepomuk.Pair> filenames = new ArrayList<test.nepomuk.Pair>();
//			filenames = NepomukDataTester.getKeyValuePairs(size);
//			if(filenames == null) break;
//			System.out.println("filenames-size :"+filenames.size());
//			
//			for (int i=0; i<filenames.size(); i++){
//				keys[i] = new byte[filenames.get(i).key.getBytes().length];
//				values[i] = new byte[filenames.get(i).value.getBytes().length];
//				keys[i] = filenames.get(i).key.getBytes();
//				values[i] = filenames.get(i).value.getBytes();
//			}
//			
//
//			//PreInsertion.. We want to insert directly in to the CSV
//			CSV_WAY = true;
//
//			if (CSV_WAY){
//				CSVIndexTable localStore = new CSVIndexTable(true);
//				try {
//					localStore.openFileForWriting();
//					for (int j = 0; j < amount; j++) {
//						localStore.addIndexEntry((pgrid.IndexEntry)randomIndexEntry(j));				
//					}
//				} catch (Exception e){
//					localStore.closeFileForWriting();
//				}
//				
//			} else {
////				System.out.println("Network Insertion :"+entries.size());
////				index.insert(entries);
//			}
//
////			this.insert(keys,values);
////			//After PreInsertion.. We want to insert directly in to the Network
//			CSV_WAY  = false;
//			
//			if (stop) break;
//		}
		
	}

	public void newMessage(Message arg0, Peer arg1) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean insert(byte[][] keys, byte[][] values){
		
		if (keys.length != values.length) {
			System.out.println("PGrid.insert(): keys and values arrays should have the same size !");
			return false;
		}
		
		System.out.println("Inserting " + keys.length + " entries");
		
		IndexEntry entry;
		Type type = indexFactory.createType("SimpleType");
		ArrayList<IndexEntry> entries = new ArrayList<IndexEntry>();
		
		
		// Write to log
		FileWriter writer = null;
		if (logInsertedFiles){
			try {
				writer = new FileWriter(log, true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		for (int i=0;i<keys.length;i++){
			
			entry = indexFactory.createIndexEntry(type, new String(values[i]));
			
			/*
			// For specifying the key
			entry = new XMLSimpleIndexEntry(p2pFactory.generateGUID(),
					type,
					p2pFactory.generateKey(new String(values[i])),
					p2p.getLocalPeer(), 
					new String(values[i]));
			*/
			
			if (logInsertedFiles){
				try {
					writer.write(entry.getKey().toString() + " " + entry.getData().toString() + " " + entry.getGUID().toString() + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			entries.add(entry);
		}
		
		
		if (logInsertedFiles){
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
		
		if (CSV_WAY){
			CSVIndexTable localStore = new CSVIndexTable(true);
			try {
				localStore.openFileForWriting();
				for (IndexEntry e : entries)
					localStore.addIndexEntry((pgrid.IndexEntry)e);
			} catch (Exception e){
				localStore.closeFileForWriting();
			}
			
			if (writeDirectlyToDB){
				System.out.println("Writing to DB");
				PGridP2P.sharedInstance().getIndexManager().insertIndexEntries(entries, true);
				PGridP2P.sharedInstance().getIndexManager().getIndexTable().flushInsert();
				PGridP2P.sharedInstance().getIndexManager().propagateAllLocalIndexes();
			}
		} else {
			System.out.println("Network Insertion :"+entries.size());
			index.insert(entries);
		}
		
		//index.insert(entries);
		
		return true;
	}

	
	public boolean insert(byte[] key, byte[] value) {

		/*
		GridellaTesterFiles testFiles = new GridellaTesterFiles();
		CSVIndexTable localStore = new CSVIndexTable(true);
		localStore.openFileForWriting();
		cp ../../pgrid-csv/p-grid.jar .
//		 creating and registering data type
		p2p.index.Type type = indexFactory.createType("text/file");
		TypeHandler handler = new FileTypeHandler(type);
		indexFactory.registerTypeHandler(type, handler);

		for (int i = 0; i < 10000; i++) {
			String rnd = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(new
		SecureRandom().nextInt(25))+""+new
		SecureRandom().nextInt(Integer.MAX_VALUE)+"";
			
			String fileName = testFiles.uniformFilenames(1)[0];// filenames[i];
			String[] parts = Tokenizer.tokenize(fileName, "\t");

			//public XMLFileIndexEntry(GUID guid, Type type, Key key, PGridHost host, int qoS, String path, String name, int size, String infos, String desc) {
			
			pgrid.IndexEntry ie = new XMLFileIndexEntry(PGridP2PFactory.sharedInstance().generateGUID(), type,
					PGridP2PFactory.sharedInstance().generateKey(rnd+parts[1]),
					PGridP2P.sharedInstance().getLocalHost(), 0, parts[0], rnd+parts[1], Integer.parseInt(parts[2]),
		"", rnd+parts[1]);

			localStore.addIndexEntry(ie);

			if(i%2000 == 0){
				PGridP2P.sharedInstance().getIndexManager().getIndexTable().sequentialAdd(ie);
			}

			//System.out.println("Inserting into localstore " + rnd+parts[1] + " with key " + PGridP2PFactory.sharedInstance().generateKey(rnd+parts[1]));
				
		}
		
		//  Insert special keys
		  
		
		pgrid.IndexEntry ie = new XMLFileIndexEntry(PGridP2PFactory.sharedInstance().generateGUID(), type,
				PGridP2PFactory.sharedInstance().generateKey("lsirtest1"),
				PGridP2P.sharedInstance().getLocalHost(), 0, "/ch/epfl/lsir/pgrid/lsirtest1", "lsirtest1", Integer.parseInt("12345"),
	"", "lsirtest1");
		localStore.addIndexEntry(ie);
		PGridP2P.sharedInstance().getIndexManager().getIndexTable().sequentialAdd(ie);
		
		ie = new XMLFileIndexEntry(PGridP2PFactory.sharedInstance().generateGUID(), type,
				PGridP2PFactory.sharedInstance().generateKey("lsirtest2"),
				PGridP2P.sharedInstance().getLocalHost(), 0, "/ch/epfl/lsir/pgrid/lsirtest2", "lsirtest2", Integer.parseInt("12345"),
	"", "lsirtest2");
		localStore.addIndexEntry(ie);
		PGridP2P.sharedInstance().getIndexManager().getIndexTable().sequentialAdd(ie);
		
		
		localStore.closeFileForWriting();
		PGridP2P.sharedInstance().getIndexManager().getIndexTable().flushInsert();
		PGridP2P.sharedInstance().getIndexManager().propagateAllLocalIndexes();	
		
		*/
		
		
		
		
		System.out.println("New insert witk key " + new String(key) + " and value " + new String(value));
				
		IndexEntry entry;
		//pgrid.IndexEntry entry;
		Type type = indexFactory.createType("SimpleType");
		ArrayList<IndexEntry> entries = new ArrayList<IndexEntry>();
		
		// create a data entry object through the storage interface
		entry = indexFactory.createIndexEntry(type, new String(value));
		entries.add(entry);
		
		// Write to log
		if (logInsertedFiles){
			FileWriter writer;
			try {
				writer = new FileWriter(log, true);
				writer.write(entry.getKey().toString() + " " + new String(value) + " " + entry.getGUID().toString() + "\n");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (CSV_WAY){
			CSVIndexTable localStore = new CSVIndexTable(true);
			try {
				localStore.openFileForWriting();
			} catch (Exception e) {
				e.printStackTrace();
			}
			localStore.addIndexEntry((pgrid.IndexEntry)entry);
			localStore.closeFileForWriting();
			if (writeDirectlyToDB){
				System.out.println("Writing to DB");
				PGridP2P.sharedInstance().getIndexManager().insertIndexEntries(entries, true);
				PGridP2P.sharedInstance().getIndexManager().getIndexTable().flushInsert();
				PGridP2P.sharedInstance().getIndexManager().propagateAllLocalIndexes();
			}
		} else {
			index.insert(entries);
		}
		return true;
	}

	public byte[] retrieve(byte[] key) {
		// TODO Auto-generated method stub
		return null;
	}

	public void rangeSearch(String lowerBound, String upperBound, GUID guid, SearchListener listener) {
		System.out.println("Range search: " + lowerBound + " - " + upperBound + " with GUID: " + guid);
		Type type = indexFactory.createType("SimpleType");
		p2p.index.Query q = indexFactory.createQuery(type, lowerBound, upperBound);
		System.out.println("Creating a range query with lower bound '" + q.getLowerBound() + "' and upper bound '" + q.getHigherBound() + "'.");
		System.out.println("Searching for data entries with query GUID: " + q.getGUID());
		//listener.addQueryGUID(q.getGUID());
		index.search(q, listener);
	}

	public void simpleSearch(String query, GUID guid, SearchListener listener) {
		System.out.println("Simple search: " + query + " with GUID: " + guid);
		
//		 Create a query to retrieve all data entries starting with "Updated"
		Type type = indexFactory.createType("SimpleType");
		p2p.index.Query q = indexFactory.createQuery(type, query);
		System.out.println("Creating a query with keyword '" + q.getLowerBound() + "'");
		System.out.println("Searching for data entries with query GUID: " + q.getGUID());
		//listener.addQueryGUID(q.getGUID());
		index.search(q, listener);
		
	}

	public void newMessage(PGridMessage msg, p2p.basic.GUID guid) {
		System.out.println("NaFTManager.newMessage(): Got a message: " + msg + " with GUID: " + guid);
		System.out.println("NaFTManager.newMessage(): Sending back a message to the host to see if we use the same connection");
		GenericMessage message = new GenericMessage("Test".getBytes());
		PGridP2P.sharedInstance().send(msg.getHeader().getHost(), message);
		
	}

	public void downloadFinished(p2p.basic.GUID guid) {
		System.out.println("PGrid.downloadFinished(): guid = " + guid);
	}

	private IndexEntry randomIndexEntry(int i){
		IndexEntry ie = null;
		String rnd = getRandomString();
		String fileName = testFiles.uniformFilenames(1)[0];//filenames[i];
		String[] parts = Tokenizer.tokenize(fileName, "\t");

		ie = new XMLFileIndexEntry(PGridP2PFactory.sharedInstance().generateGUID(), type,
				PGridP2PFactory.sharedInstance().generateKey(rnd+parts[1]+"___"+i), PGridP2P.sharedInstance().getLocalHost(), parts[1]+"___"+i/*+":"+parts[0]+":"+ parts[2]*/);

		return ie;
	}

	String RND = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abc";
	int rndLen = 36;
	// creating and registering data type
	p2p.index.Type type;
	
	private String getRandomString(){
		String str = "";
		for (int i = 0; i < 10; i++) {
			str += RND.charAt(new SecureRandom().nextInt(rndLen));
		}
		return str;
	}
}

