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
package test.gridella;

import pgrid.*;
import pgrid.core.index.CSVIndexTable;
import pgrid.core.maintenance.DbCsvUtils;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.interfaces.basic.PGridP2PFactory;
import pgrid.interfaces.index.SimpleIndexEntry;
import pgrid.interfaces.index.PGridIndexFactory;
import pgrid.util.logging.LogFormatter;
import pgrid.util.logging.FlushedStreamHandler;
import pgrid.util.Tokenizer;

import java.security.SecureRandom;
import java.util.logging.StreamHandler;
import java.util.logging.Level;
import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;

import gnu.getopt.LongOpt;
import gnu.getopt.Getopt;
import sun.misc.Signal;
import sun.misc.SignalHandler;
import p2p.index.Index;
import p2p.index.IndexEntry;
import p2p.index.IndexFactory;
import p2p.index.TypeHandler;

/**
 * Class description goes here
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public class GridellaTester {

	private static final String SHARED_DATA_FILE = Constants.LOG_DIR + "sharedData.dat";

	/**
	 * The reference to the only instance of this class (Singleton
	 * pattern). This differs from the C++ standard implementation by Gamma
	 * et.al. since Java ensures the order of static initialization at runtime.
	 *
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	private static final GridellaTester SHARED_INSTANCE = new GridellaTester();

	/**
	 * This creates the only instance of this class. This differs from the C++ standard implementation by Gamma et.al.
	 * since Java ensures the order of static initialization at runtime.
	 *
	 * @return the shared instance of this class.
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	public static GridellaTester sharedInstance() {
		return SHARED_INSTANCE;
	}

	/*static {
		LogFormatter formatter = new LogFormatter();
		formatter.setDateFormat("HH:mm:ss");
		Constants.initChildLogger(LOGGER, formatter, null);
	}  */

	/**
	 * Checks the delivered arguments, and starts all required facilities of the
	 * Gridella program.
	 *
	 * @param argv delivered arguments by the user.
	 */
	public static void main(String[] argv) {
		System.out.println("starting P-Grid server [" + Constants.VERSION + "] ...");
		Constants.LOGGER.setUseParentHandlers(false);
		// System.err handler
		LogFormatter logFormatter = new LogFormatter();
		StreamHandler eHandler = new FlushedStreamHandler(System.err, logFormatter);
		eHandler.setLevel(Level.WARNING);
		Constants.LOGGER.addHandler(eHandler);

		int port = -1;
		String cfgFile = null;
		String routeFile = null;
		String logFile = null;
		boolean verboseMode = false;
		int debugMode = -1;
		int nbFiles = 0;
		int repFact = -1;
		String bootstrap = null;
		boolean lb = false;
		boolean superpeer = false;
		boolean testmode = false;

		// process delivered arguments
		LongOpt[] longOpts = new LongOpt[13];
		longOpts[0] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
		longOpts[1] = new LongOpt("port", LongOpt.REQUIRED_ARGUMENT, null, 'p');
		longOpts[2] = new LongOpt("config", LongOpt.REQUIRED_ARGUMENT, null, 'c');
		longOpts[3] = new LongOpt("route", LongOpt.REQUIRED_ARGUMENT, null, 'r');
		longOpts[4] = new LongOpt("log", LongOpt.REQUIRED_ARGUMENT, null, 'l');
		longOpts[5] = new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v');
		longOpts[6] = new LongOpt("debug", LongOpt.REQUIRED_ARGUMENT, null, 'd');
		longOpts[7] = new LongOpt("nb_files", LongOpt.REQUIRED_ARGUMENT, null, 'n');
		longOpts[8] = new LongOpt("freplication", LongOpt.REQUIRED_ARGUMENT, null, 'f');
		longOpts[9] = new LongOpt("bootstraphost", LongOpt.REQUIRED_ARGUMENT, null, 'b');
		longOpts[10] = new LongOpt("Loadbalancing", LongOpt.NO_ARGUMENT, null, 'L');
		longOpts[11] = new LongOpt("Superpeer", LongOpt.NO_ARGUMENT, null, 'S');
		longOpts[12] = new LongOpt("test", LongOpt.NO_ARGUMENT, null, 't');
		Getopt args = new Getopt("pgrid", argv, "", longOpts, true);
		args.setOpterr(true);

		int opt;
		while ((opt = args.getopt()) != -1) {
			switch (opt) {
				case 'h': // help
					usage();
					break;
				case 'p': // local port
					String portStr = args.getOptarg();
					if (portStr == null) {
						System.err.println("option '-p|-port' requires an argument");
						usage();
					}
					try {
						port = new Integer(args.getOptarg()).intValue();
					} catch (NumberFormatException e) {
						System.err.println("invalid value for option '-p|-port' (number required)");
						usage();
					}
					break;
				case 'c': // P-Grid property file
					cfgFile = args.getOptarg();
					if ((cfgFile == null) || (cfgFile.length() == 0)) {
						System.err.println("option '-c|-config' requires an argument");
						usage();
					}
					break;
				case 'r': // P-Grid routing file
					routeFile = args.getOptarg();
					if ((routeFile == null) || (routeFile.length() == 0)) {
						System.err.println("option '-r|-route' requires an argument");
						usage();
					}
					break;
				case 'l': // logging file
					logFile = args.getOptarg();
					if ((logFile == null) || (logFile.length() == 0)) {
						System.err.println("option '-l|-log' requires an argument");
						usage();
					}
					break;
				case 'v': // verbose mode
					verboseMode = true;
					break;
				case 'd': // debug mode
					//@todo debug level is only optional, not required
					debugMode = 0;
					if (args.getOptarg() == null) {
						debugMode = 0;
						break;
					}
					try {
						debugMode = new Integer(args.getOptarg()).intValue();
					} catch (NumberFormatException e) {
						System.err.println("invalid value for option '-d|-debug' (number required)");
						usage();
					}
					break;
				case 'n':
					try {
						nbFiles = new Integer(args.getOptarg()).intValue();
					} catch (NumberFormatException e) {
						System.err.println("invalid value for option '-n|-nb_files' (number required)");
						usage();
					}
					break;
				case 'f':
					try {
						repFact = new Integer(args.getOptarg()).intValue();
					} catch (NumberFormatException e) {
						System.err.println("invalid value for option '-f|-rep' (replication factor)");
						usage();
					}
					break;
				case 'L':
					lb = true;
					break;
				case 'S':
					superpeer = true;
					break;
				case 'b':
					try {
						bootstrap = args.getOptarg();
					} catch (NumberFormatException e) {
						System.err.println("invalid value for option '-b|-bootstraphost'");
						usage();
					}
					break;
				case 't':
					testmode = true;
					break;
				case '?': // unknown option
					usage();
					break;
				default:
					usage();
					break;
			}
		}

		// logging facility
		Constants.initLogger(null, debugMode, verboseMode, logFile);

		PGridP2P pgrid = PGridP2P.sharedInstance();
		// create the properties, if neccessery
		java.util.Properties props = new java.util.Properties();
		if ((port != -1) || (cfgFile != null) || (routeFile != null) || (repFact != -1) || (bootstrap!=null)) {
			if (port != -1)
				props.setProperty(PGridP2P.PROP_LOCAL_PORT, String.valueOf(port));
			if (cfgFile != null)
				props.setProperty(PGridP2P.PROP_PROPERTY_FILE, cfgFile);
			if (repFact != -1)
				props.setProperty(PGridP2P.PROP_REPLICATION_FACTOR, repFact+"");
			if (bootstrap!=null)
				props.setProperty(Properties.BOOTSTRAP_HOSTS, bootstrap);
		}

		// Start the PGridP2P listener
		//props.setProperty(Properties.IN_MEMORY_DB, "true");
		props.setProperty(Properties.SUPER_PEER, superpeer+"");
		props.setProperty(Properties.REPLICATION_BALANCE, lb+"");
		props.setProperty(Properties.TEST_MODE, testmode+"");
		props.setProperty(PGridP2P.PROP_START_LISTENER, "true");
		props.setProperty(Properties.DEBUG_MODE, testmode+"");
		pgrid.init(props);

		// creating data item
		IndexFactory sf = PGridIndexFactory.sharedInstance();
		Index indexService = sf.createIndex(pgrid);
		GridellaTesterFiles testFiles = new GridellaTesterFiles();

		// creating and registering data type
		p2p.index.Type type = sf.createType("text/file");
		TypeHandler handler = new FileTypeHandler(type);
		sf.registerTypeHandler(type, handler);

		// creating data item
//		String[] filenames = testFiles.uniformFilenames(nbFiles);
		Vector items = new Vector();
		long time = System.currentTimeMillis();
		System.out.print("Inserting data:");
		
		CSVIndexTable localStore = new CSVIndexTable(true);
		try {
			localStore.openFileForWriting();
		
		for (int i = 0; i < (int)(nbFiles * 1.0); i++) {
			String rnd = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(new SecureRandom().nextInt(25))+""+new SecureRandom().nextInt(Integer.MAX_VALUE)+"";
			String fileName = testFiles.uniformFilenames(1)[0];//filenames[i];
			String[] parts = Tokenizer.tokenize(fileName, "\t");
			
			pgrid.IndexEntry ie = new XMLFileIndexEntry(PGridP2PFactory.sharedInstance().generateGUID(), type,
					PGridP2PFactory.sharedInstance().generateKey(rnd+parts[1]), pgrid.getLocalHost(), 0, parts[0], rnd+parts[1], Integer.parseInt(parts[2]), "", rnd+parts[1]);
//			pgrid.getIndexManager().getIndexTable().sequentialAdd(ie);
			localStore.addIndexEntry(ie);

			/*StringTokenizer st = new StringTokenizer(parts[1], " .:;_-");
			while(st.hasMoreElements()) {
				String tmp = st.nextToken().trim();
				if (tmp.length() == 0) continue;
				pgrid.getIndexManager().getIndexTable().sequentialAdd(new XMLFileIndexEntry(PGridP2PFactory.sharedInstance().generateGUID(), type,
						PGridP2PFactory.sharedInstance().generateKey(tmp), pgrid.getLocalHost(), 0, parts[0], parts[1], Integer.parseInt(parts[2]), "", tmp));
			} */
			if(i%2000 == 0){
				pgrid.getIndexManager().getIndexTable().sequentialAdd(ie);
				System.out.println("Inserted :"+i + "\t :Took: "+((System.currentTimeMillis()-time)/1000)+" secs");
			}

		}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			localStore.closeFileForWriting();
		}
		pgrid.getIndexManager().getIndexTable().flushInsert();
		Constants.LOGGER.info("Insert took: "+(System.currentTimeMillis()-time));

//		long time1 = System.currentTimeMillis();
//		
//		new DbCsvUtils().csvToDb();
//		Constants.LOGGER.finer("CSV to DB took: "+(System.currentTimeMillis()-time1)+"ms.");


		/*p2p.index.Type type = sf.createType("SimpleType");
		TypeHandler handler = new DefaultTypeHandler(type);
		sf.registerTypeHandler(type, handler);

		String[] filenames = testFiles.uniformFilenames(nbFiles);
		for (int i = 0; i < (int)(nbFiles * 1.0); i++) {
			String fileName = filenames[i];
			String[] parts = Tokenizer.tokenize(fileName, "\t");
			IndexEntry dataItem = sf.createDataItem(type, parts[0].trim());
			allFiles.add(dataItem);
		}
		// inserting the data items*/

		// join the network
		pgrid.join();

		// this main makes something and catch Ctrl-C in order to clean its work env before to leave.
		final Thread mainThread = Thread.currentThread();
		// makes a signal holder which will interrupt the main thread if Ctrl-C is typed
		try {
			Signal.handle(new Signal("INT"), new SignalHandler() {
				public void handle(Signal sig) {
					mainThread.interrupt();
				}
			});
		} catch (IllegalArgumentException exc) {
			// do nothing
		}

		System.out.println("... P-Grid server started at " + PGridP2P.sharedInstance().getLocalHost().toHostString() + "!");
		// makes a unused object in the main thread, // sleep on it until wait() raise InterruptedException (raised if ctrl-C is typed)
		Object sync = new Object();

		// makes a signal holder which will interrupt the main thread if Ctrl-C is typed
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				mainThread.interrupt();
			}
		});

		synchronized (sync) {
			try {
				sync.wait();
			} catch (InterruptedException exc) {
				System.exit(0);
			}
		}

	}

	/**
	 * Prints the usage message, when the wrong number of arguments was deliverd,
	 * or the arguments had a wrong format.
	 */
	private static void usage() {
		System.out.println("Usage: PGridServer <options>");
		System.out.println("where possible options include:");
		System.out.println("  -p|-port <port>           Specify listening port");
		System.out.println("  -n|-nb_files <# files>    Generate # data items");
		System.out.println("  -f|-replication <#>       Set the replication factor");
		System.out.println("  -c|-config <file>         Specify the P-Grid configuration file");
		System.out.println("  -r|-route <file>          Specify the P-Grid routing file");
		System.out.println("  -l|-log                   Specify the logging file");
		System.out.println("  -v|-verbose               Enables verbose output");
		System.out.println("  -d|-debug <level>         Enables debug output (level 0-3)");
		System.out.println("  -h|-help                  Shows this information");
		System.exit(-1);
	}

	public static void saveIndexItems(Collection items) {
		File sharedDataFile = new File(SHARED_DATA_FILE);
		sharedDataFile.delete();

		// save to data items file
		try {
			FileWriter writer = new FileWriter(sharedDataFile, false);
			for (Iterator it = items.iterator(); it.hasNext();) {
				SimpleIndexEntry dataItem = (pgrid.interfaces.index.SimpleIndexEntry)it.next();
				writer.write(dataItem.getGUID()+"\t"+dataItem.getKey()+"\t"+dataItem.getData()+"\t"+((PGridHost)dataItem.getPeer()).toHostString()+"\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}


}

