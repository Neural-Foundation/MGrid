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

package pgrid;

import pgrid.network.PGridMessageMapping;
import pgrid.network.protocol.QueryMessage;
import pgrid.util.Tokenizer;
import pgrid.util.monitoring.MonitoringManager;
import pgrid.core.index.CSVIndexTable;
import pgrid.interfaces.basic.PGridP2P;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

import ch.epfl.lsir.nbench.util.Util;

/**
 * This class represents the statistics of P-Grid.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class Statistics {

	/**
	 * The statistic file for the bandwidth.
	 */
	private static final String BANDWIDTH_STAT_FILE = Constants.LOG_DIR + "bandwidth.dat";

	/**
	 * The statistic file for the uncompressed bandwidth.
	 */
	private static final String BANDWIDTH_UNCOMPR_STAT_FILE = Constants.LOG_DIR + "bandwidthUncompr.dat";

	/**
	 * The statistic file for data items.
	 */
	private static final String DATA_ITEMS_STAT_FILE = Constants.LOG_DIR + "dataItems.dat";

	/**
	 * The statistic file for the exchange cases.
	 */
	private static final String EXCHANGE_CASES_STAT_FILE = Constants.LOG_DIR + "exchangeCases.dat";

	/**
	 * The statistic file for the exchanges.
	 */
	private static final String EXCHANGE_STAT_FILE = Constants.LOG_DIR + "exchanges.dat";

	/**
	 * The statistic file for the messages.
	 */
	private static final String MESSAGES_STAT_FILE = Constants.LOG_DIR + "messages.dat";

	/**
	 * The statistic file for queries.
	 */
	private static final String MESSAGE_ROUTING_STAT_FILE = Constants.LOG_DIR + "routerStat.dat";

	/**
	 * The statistic file for queries.
	 */
	private static final String QUERY_STAT_FILE = Constants.LOG_DIR + "queryStat.dat";

	/**
	 * The statistic file for the amount of replicas.
	 */
	private static final String SYSTEM_STAT_FILE = Constants.LOG_DIR + "system.dat";

	/**
	 * The statistic file for the updates.
	 */
	private static final String UPDATES_STAT_FILE = Constants.LOG_DIR + "updates.dat";

	/**
	 * The statistic file for the updates.
	 */
	private static final String DB_STAT_FILE = Constants.LOG_DIR + "db.dat";

	/**
	 * The amount of exchange cases.
	 */
	private static final int EXCHANGE_CASES = 10;

	/**
	 * The amount of message types.
	 */
	private final int MESSAGE_TYPES;

	/**
	 * The time between two saves.
	 */
	private static final int STAT_SAVE_RATE = 1000*60*5; // 5 min.

	/**
	 * Statistics about "ExchangeCase1", the amount of how often the Exchange case 1 was taken.
	 */
	public static final short EXCHANGE_CASE_1 = 0;

	/**
	 * Statistics about "ExchangeCase2", the amount of how often the Exchange case 2 was taken.
	 */
	public static final short EXCHANGE_CASE_2 = 1;

	/**
	 * Statistics about "ExchangeCase2.1", the amount of how often the Exchange case 2.1 was taken.
	 */
	public static final short EXCHANGE_CASE_2_1 = 2;

	/**
	 * Statistics about "ExchangeCase2.2", the amount of how often the Exchange case 2.2 was taken.
	 */
	public static final short EXCHANGE_CASE_2_2 = 3;

	/**
	 * Statistics about "ExchangeCase3a", the amount of how often the Exchange case 3a was taken.
	 */
	public static final short EXCHANGE_CASE_3a = 4;

	/**
	 * Statistics about "ExchangeCase3a.1", the amount of how often the Exchange case 3a.1 was taken.
	 */
	public static final short EXCHANGE_CASE_3a_1 = 5;

	/**
	 * Statistics about "ExchangeCase3a.2", the amount of how often the Exchange case 3a.2 was taken.
	 */
	public static final short EXCHANGE_CASE_3a_2 = 6;

	/**
	 * Statistics about "ExchangeCase3b", the amount of how often the Exchange case 3b was taken.
	 */
	public static final short EXCHANGE_CASE_3b = 7;

	/**
	 * Statistics about "ExchangeCase3b.1", the amount of how often the Exchange case 3b.1 was taken.
	 */
	public static final short EXCHANGE_CASE_3b_1 = 8;

	/**
	 * Statistics about "ExchangeCase3b.2", the amount of how often the Exchange case 3b.2 was taken.
	 */
	public static final short EXCHANGE_CASE_3b_2 = 9;

	/**
	 * The bandwidth.
	 */
	public int[] Bandwidth;

	/**
	 * The uncompressed bandwidth.
	 */
	public int[] BandwidthUncompr;

	/**
	 * The amount of exchange per case.
	 */
	public int[] ExchangeCases = new int[EXCHANGE_CASES];

	/**
	 * The amount of exchanges.
	 */
	public int Exchanges = 0;

	/**
	 * The amount of failed exchanges.
	 */
	public int ExchangesFailed = 0;

	/**
	 * The amount of ignored exchange invitations.
	 */
	public int ExchangesIgnored = 0;

	/**
	 * The amount of initiated exchanges.
	 */
	public int ExchangesInitiated = 0;

	/**
	 * The amount of exchanges with replicas having equal data sets.
	 */
	public int ExchangesRealReplicas = 0;

	/**
	 * The amount of exchanges with replicas.
	 */
	public int ExchangesReplicas = 0;

	/**
	 * True if in an emulated churn phase
	 */
	public boolean Churn = false;

	/**
	 * The amount of managed data items.
	 */
	public int DataItemsManaged = 0;

	/**
	 * The amount of managed data items belonging to the local path.
	 */
	public int DataItemsPath = 0;

	/**
	 * The amount of sent data items.
	 */
	public int DataItemsSent = 0;

	/**
	 * Time in millisecond to process the last insrt
	 */
	public long InsertDataItemProcessingTime = 0;

	/**
	 * NB of data items inserted during the last insert.
	 */
	public long InsertedDataItems = 0;

	/**
	 * Indicates if exchanges are currently initiated by the local peer or not.
	 */
	public int InitExchanges = 0;

	/**
	 * The amount of messages.
	 */
	public int[] Messages;

	/**
	 * Message statistic to increase
	 */
	public static enum messageStats{initiated,
		forwarded,
		resolved,
		cannotRoute,
		alreadySeen,
		notSuperpeer,
		badRequest,
		timeout,
		notFound,
		found}

	/**
	 * Message sent locally
	 */
	private int[] MessageInitiated;

	/**
	 * Message forwarded
	 */
	private int[] MessageForwarded;

	/**
	 *  Message resolved
	 */
	private int[] MessageResolved;

	/**
	 * Message timeout
	 */
	private int[] MessageCannotRoute;

	/**
	 * Message already seen
	 */
	private int[] MessageAlreadySeen;

	/**
	 * Message bad request
	 */
	private int[] MessageBadRequest;

	/**
	 * Message timeout
	 */
	private int[] MessageTimeOut;


	/**
	 * Message dirty flag to save some IO
	 */
	private boolean[] MessageDirtyFlag;


	/**
	 * If this peers tries to contact a non super peer host for routing...
	 */
	private int[] NotSuperpeer;

	/**
	 * The current path length.
	 */
	public int PathLength = 0;

	/**
	 * In which phase the local peer is.
	 */
	public int Phase = 0;

	/**
	 * how many queries resulted in a hit
	 */
	private int QueryHit = 0;

	/**
	 * how many queries resulted in nothing found
	 */
	private int QueryMissed = 0;

	/**
	 * The lookup not found.
	 */
	public int LookupNotFound = 0;

	/**
	 * The amount of replicas.
	 */
	public int Replicas = 0;

	/**
	 * The amount of bad requests for updates.
	 */
	public int UpdatesBadRequests = 0;

	/**
	 * The amount of local initiated updates.
	 */
	public int UpdatesLocalProcessed = 0;

	/**
	 * The amount of remote initiated updates.
	 */
	public int UpdatesRemoteProcessed = 0;

	/**
	 * Minimum storage
	 */
	public int MinStorage = 0;

	/**
	 * The shutdown flag.
	 */
	private boolean shutdown = false;


	/**
	 * Constructs the application statistics.
	 */
	public Statistics() {
		MESSAGE_TYPES = PGridMessageMapping.sharedInstance().getNBMessagesType();
		Bandwidth = new int[MESSAGE_TYPES];
		BandwidthUncompr = new int[MESSAGE_TYPES];
		Messages = new int[MESSAGE_TYPES];
		MessageTimeOut = new int[MESSAGE_TYPES];
		MessageAlreadySeen = new int[MESSAGE_TYPES];
		MessageBadRequest = new int[MESSAGE_TYPES];
		MessageForwarded = new int[MESSAGE_TYPES];
		MessageInitiated = new int[MESSAGE_TYPES];
		MessageResolved = new int[MESSAGE_TYPES];
		MessageCannotRoute = new int[MESSAGE_TYPES];
		MessageDirtyFlag = new boolean[MESSAGE_TYPES];
		NotSuperpeer = new int[MESSAGE_TYPES];
		for (int i=0; i<MessageDirtyFlag.length;i++)
			MessageDirtyFlag[i]=false;
	}

	private Date getGlobalTime() {
		Process p = null;
		try {
			p = Runtime.getRuntime().exec("rdate swisstime.ethz.ch");
		} catch (IOException e) {
			return new Date(System.currentTimeMillis());
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String time = null;
		try {
			time = in.readLine();
			if (time != null) time = time.substring(30).trim();
			else return new Date(System.currentTimeMillis());
		} catch (Exception e) {
			return new Date(System.currentTimeMillis());
		}
		SimpleDateFormat date = new SimpleDateFormat("MMM dd hh:mm:ss yyyy");
		try {
			return date.parse(time);
		} catch (ParseException e) {
			return new Date(System.currentTimeMillis());
		}
	}

	private String getCurrentRuntimeConsumption(){
		// percentage cpu, percentage memory, resident set size (in MB)
		String consumption = "0.0 0.0 0";
		Process p = null;
		try {
			//p = Runtime.getRuntime().exec("ps -eo pcpu,pmem,rss,args | sort -k 1 -r | grep p-grid.jar | grep -v grep");
			p = Runtime.getRuntime().exec("ps -eo pcpu,pmem,rss,args");
		} catch (IOException e) {
			e.printStackTrace();
			return consumption;
		}
		
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String topStr = "";
		try {
			while ((topStr = in.readLine()) != null) {
				/*
				 * FIXME:find a way to clearly identify a p-grid instance 
				 * in case of multiple instances on the same machines 
				 * 
				 * Currently the port of the node should be on the command line when starting the node
				 */  
				
				//if (topStr.indexOf("p-grid.jar") > 0 && Tokenizer.tokenize(topStr)[3].startsWith("java")) {
				if (topStr.indexOf("p-grid.jar") > 0 && 
						topStr.indexOf(String.valueOf(PGridP2P.sharedInstance().getLocalPeer().getPort())) > 0){
						break;
				}
				
			}
			if (topStr == null) {
				return consumption;
			}
			String[] vals = Tokenizer.tokenize(topStr);
			if (vals.length > 2){
				consumption = vals[0] + " " + vals[1] + " " + String.valueOf(Double.parseDouble(vals[2])/1024);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return consumption;
		}
		return consumption;
	}
		
	private String getMemoryConsumption() {
		String mem = "0 0";
		Process p = null;
		try {
			p = Runtime.getRuntime().exec("ps -A v");
		} catch (IOException e) {
			return mem;
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String topStr = null;
		try {
			while (true) {
				topStr = in.readLine();
				if (topStr == null) {
					break;
				}	else if (topStr.indexOf("java") > 0) {
					break;
				}
			}
			if (topStr == null) {
				return mem;
			}
			String[] vals = Tokenizer.tokenize(topStr);
			if (vals[7].endsWith("m")) {
				vals[7] = vals[7].substring(0, vals[7].length()-1);
			}
			double rss = Double.parseDouble(vals[7]) / 1000;
			double proz = Double.parseDouble(vals[8]);
			mem = String.valueOf(rss) + " " + String.valueOf(proz);
			if (PGridP2P.sharedInstance().isInTestMode()) {
				if ((rss > 300.0) || (proz > 30.0)) {
					Constants.LOGGER.severe("Memory consumption is too high: " + String.valueOf(rss) + "MB physical memory (" + String.valueOf(proz) + "%)");
					System.exit(0);
				}
			}
		} catch (Exception e) {
			return mem;
		}
		return mem;
	}

	/**
	 * Initializes the properties with the given property file and properties.
	 */
	synchronized public void init() {
		final long globalStartTime = getGlobalTime().getTime();
		final long timeDiff = globalStartTime - System.currentTimeMillis();
		// start thread to store statistic series
		Thread t = new Thread("Statistics") {
			File bandwidthFile = new File(BANDWIDTH_STAT_FILE);
			File bandwidthUncomprFile = new File(BANDWIDTH_UNCOMPR_STAT_FILE);
			File dataItemsFile = new File(DATA_ITEMS_STAT_FILE);
			File exchangeCasesFile = new File(EXCHANGE_CASES_STAT_FILE);
			File exchangesFile = new File(EXCHANGE_STAT_FILE);
			File messagesFile = new File(MESSAGES_STAT_FILE);
			File routingFile = new File(MESSAGE_ROUTING_STAT_FILE);
			File systemFile = new File(SYSTEM_STAT_FILE);
			File updatesFile = new File(UPDATES_STAT_FILE);
			File dbFile = new File(DB_STAT_FILE);
			File queryFile = new File(QUERY_STAT_FILE);

			public void run() {
				bandwidthFile.delete();
				bandwidthUncomprFile.delete();
				dataItemsFile.delete();
				exchangeCasesFile.delete();
				exchangesFile.delete();
				messagesFile.delete();
				routingFile.delete();
				systemFile.delete();
				updatesFile.delete();
				dbFile.delete();
				queryFile.delete();
				boolean init = true;

				while (!shutdown) {
					try {
						Thread.sleep(STAT_SAVE_RATE);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					String time = String.valueOf(System.currentTimeMillis() + timeDiff);
					try {
						// bandwidth
						FileWriter writer = new FileWriter(bandwidthFile, true);
						String bdwdth = time + " " + String.valueOf(Phase);

						for (int i = 0; i < Bandwidth.length; i++) {
							bdwdth += " " + String.valueOf(Bandwidth[i] / (STAT_SAVE_RATE / 1000));
							Bandwidth[i] = 0;
						}
						writer.write(bdwdth + "\n");
						writer.close();
						MonitoringManager.sharedInstance().reportStatistics(bandwidthFile.getName(), bdwdth);
						

						// bandwidth uncompressed
						writer = new FileWriter(bandwidthUncomprFile, true);
						String bdwdthUnc = time + " " + String.valueOf(Phase);
						for (int i = 0; i < BandwidthUncompr.length; i++) {
							bdwdthUnc += " " + String.valueOf(BandwidthUncompr[i] / (STAT_SAVE_RATE / 1000));
							BandwidthUncompr[i] = 0;
						}
						writer.write(bdwdthUnc + "\n");
						writer.close();
						MonitoringManager.sharedInstance().reportStatistics(bandwidthUncomprFile.getName(), bdwdthUnc);
						

						// data items
						writer = new FileWriter(dataItemsFile, true);
						writer.write(time + " " + String.valueOf(Phase) + " " + String.valueOf(DataItemsManaged) + " " + String.valueOf(DataItemsPath) + " " + String.valueOf(DataItemsSent) + "\n");
						writer.close();
						MonitoringManager.sharedInstance().reportStatistics(dataItemsFile.getName(), time + " " + String.valueOf(Phase) + " " + String.valueOf(DataItemsManaged) + " " + String.valueOf(DataItemsPath) + " " + String.valueOf(DataItemsSent));
						DataItemsSent = 0;

						// exchange cases
						writer = new FileWriter(exchangeCasesFile, true);
						String exchCases = time + " " + String.valueOf(Phase);
						for (int i = 0; i < ExchangeCases.length; i++) {
							exchCases += " " + String.valueOf(ExchangeCases[i]);
							ExchangeCases[i] = 0;
						}
						writer.write(exchCases + "\n");
						writer.close();
						MonitoringManager.sharedInstance().reportStatistics(exchangeCasesFile.getName(), exchCases);

						// exchanges
						writer = new FileWriter(exchangesFile, true);
						writer.write(time + " " + String.valueOf(Phase) + " " + String.valueOf(Exchanges) + " " + String.valueOf(ExchangesInitiated) + " " + String.valueOf(ExchangesFailed) + " " + String.valueOf(ExchangesIgnored) + " " + String.valueOf(ExchangesReplicas) + " " + String.valueOf(ExchangesRealReplicas) + "\n");
						writer.close();
						MonitoringManager.sharedInstance().reportStatistics(exchangesFile.getName(), time + " " + String.valueOf(Phase) + " " + String.valueOf(Exchanges) + " " + String.valueOf(ExchangesInitiated) + " " + String.valueOf(ExchangesFailed) + " " + String.valueOf(ExchangesIgnored) + " " + String.valueOf(ExchangesReplicas) + " " + String.valueOf(ExchangesRealReplicas));
						Exchanges = 0;
						ExchangesInitiated = 0;
						ExchangesFailed = 0;
						ExchangesIgnored = 0;
						ExchangesReplicas = 0;
						ExchangesRealReplicas = 0;

						// messages
						writer = new FileWriter(messagesFile, true);
						String msg = time + " " + String.valueOf(Phase);
						for (int i = 0; i < Messages.length; i++) {
							msg += " " + String.valueOf(Messages[i]);
							Messages[i] = 0;
						}
						writer.write(msg + "\n");
						writer.close();
						MonitoringManager.sharedInstance().reportStatistics(messagesFile.getName(), msg);

						// router
						writer = new FileWriter(routingFile, true);
						if (init)  {
							writer.write(time + " " + String.valueOf(Phase) + " 0 0 0 0 0 0 0 0\n");
						} else {
							for (int i = 3; i < MessageDirtyFlag.length; i++) {
								if (MessageDirtyFlag[i]) {
									writer.write(time + " " + String.valueOf(Phase) + " " + i + " " + String.valueOf(MessageInitiated[i]) + " " + String.valueOf(MessageResolved[i]) + " " + String.valueOf(MessageForwarded[i]) + " " + String.valueOf(MessageAlreadySeen[i]) + " " + String.valueOf(MessageBadRequest[i]) + " " + String.valueOf(MessageCannotRoute[i]) + " " + String.valueOf(MessageTimeOut[i]) + " " + String.valueOf(NotSuperpeer[i]) +"\n");
									MonitoringManager.sharedInstance().reportStatistics(routingFile.getName(), time + " " + String.valueOf(Phase) + " " + i + " " + String.valueOf(MessageInitiated[i]) + " " + String.valueOf(MessageResolved[i]) + " " + String.valueOf(MessageForwarded[i]) + " " + String.valueOf(MessageAlreadySeen[i]) + " " + String.valueOf(MessageBadRequest[i]) + " " + String.valueOf(MessageCannotRoute[i]) + " " + String.valueOf(MessageTimeOut[i]) + " " + String.valueOf(NotSuperpeer[i]));
									MessageInitiated[i] = 0;
									MessageResolved[i] = 0;
									MessageForwarded[i] = 0;
									MessageAlreadySeen[i] = 0;
									MessageBadRequest[i] = 0;
									MessageCannotRoute[i] = 0;
									NotSuperpeer[i] = 0;
									MessageTimeOut[i] = 0;
									MessageDirtyFlag[i] = false;
								}
							}
						}
						writer.close();

						// queries
						if (QueryHit != 0 || QueryMissed != 0 || init) {
							writer = new FileWriter(queryFile, true);
							writer.write(time + " " + String.valueOf(Phase) + " " + String.valueOf(QueryHit) + " " + String.valueOf(QueryMissed) + "\n");
							writer.close();
							MonitoringManager.sharedInstance().reportStatistics(queryFile.getName(), time + " " + String.valueOf(Phase) + " " + String.valueOf(QueryHit) + " " + String.valueOf(QueryMissed));
							QueryHit = 0;
							QueryMissed = 0;
						}

						// db statistics
						writer = new FileWriter(dbFile, true);
						writer.write(time + " " + String.valueOf(InsertedDataItems) + " " + String.valueOf(InsertDataItemProcessingTime) + "\n");
						writer.close();
						MonitoringManager.sharedInstance().reportStatistics(dbFile.getName(), time + " " + String.valueOf(InsertedDataItems) + " " + String.valueOf(InsertDataItemProcessingTime));

						// system values
						writer = new FileWriter(systemFile, true);
						//writer.write(time + " " + (Churn?0:String.valueOf(Phase)) + " " + String.valueOf(InitExchanges) + " " + String.valueOf(PathLength) + " " + String.valueOf(Replicas) + " " + String.valueOf(MinStorage) + " " + getMemoryConsumption() + "\n");
						writer.write(time + " " + (Churn?0:String.valueOf(Phase)) + " " + String.valueOf(InitExchanges) + " " + String.valueOf(PathLength) + " " + String.valueOf(Replicas) + " " + String.valueOf(MinStorage) + " " + getCurrentRuntimeConsumption() + "\n");
						// writer.write(time + " " + String.valueOf(Phase) + " " + String.valueOf(InitExchanges) + " " + String.valueOf(PathLength) + " " + String.valueOf(Replicas) + " " + String.valueOf(Runtime.getRuntime().totalMemory()) + " " + String.valueOf(MinStorage) + "\n");
						// writer.write(time + " " + String.valueOf(Phase) + " " + String.valueOf(InitExchanges) + " " + String.valueOf(PathLength) + " " + String.valueOf(Replicas) + " " + String.valueOf(Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory()) + "\n");
						writer.close();
						MonitoringManager.sharedInstance().reportStatistics(systemFile.getName(), time + " " + (Churn?0:String.valueOf(Phase)) + " " + String.valueOf(InitExchanges) + " " + String.valueOf(PathLength) + " " + String.valueOf(Replicas) + " " + String.valueOf(MinStorage) + " " + getCurrentRuntimeConsumption());

						// updates
						writer = new FileWriter(updatesFile, true);
						writer.write(time + " " + String.valueOf(Phase) + " " + String.valueOf(UpdatesLocalProcessed) + " " + String.valueOf(UpdatesRemoteProcessed) + " " + String.valueOf(UpdatesBadRequests) + "\n");
						writer.close();
						MonitoringManager.sharedInstance().reportStatistics(updatesFile.getName(), time + " " + String.valueOf(Phase) + " " + String.valueOf(UpdatesLocalProcessed) + " " + String.valueOf(UpdatesRemoteProcessed) + " " + String.valueOf(UpdatesBadRequests));
						UpdatesLocalProcessed = 0;
						UpdatesRemoteProcessed = 0;
						UpdatesBadRequests = 0;
						init = false;
						
						// Send Local Path
						MonitoringManager.sharedInstance().reportLocalPath(PGridP2P.sharedInstance().getLocalPath());
						
						// Send LOCAL.csv size
						CSVIndexTable localStore = new CSVIndexTable(true);
						MonitoringManager.sharedInstance().reportLocalCSVSize(time + " " + localStore.count());
						
					} catch (IOException e) {
						// ignore error on stat
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();
		
		
		// Report Local Path every 15 seconds
		Util.timer.schedule(new TimerTask() {
            public void run() {
            	MonitoringManager.sharedInstance().reportLocalPath(PGridP2P.sharedInstance().getLocalPath());
            }
        }, 1000, 15 * 1000);
		
	}

	public void incMessageStat(messageStats stat, int type) {
		switch (stat) {
			case alreadySeen:
				MessageAlreadySeen[type]++;
				MessageDirtyFlag[type] = true;
				break;
			case badRequest:
				MessageBadRequest[type]++;
				MessageDirtyFlag[type] = true;
				break;
			case cannotRoute:
				MessageCannotRoute[type]++;
				MessageDirtyFlag[type] = true;
				break;
			case notSuperpeer:
				NotSuperpeer[type]++;
				MessageDirtyFlag[type] = true;
				break;
			case forwarded:
				MessageForwarded[type]++;
				MessageDirtyFlag[type] = true;
				break;
			case initiated:
				MessageInitiated[type]++;
				MessageDirtyFlag[type] = true;
				break;
			case resolved:
				MessageResolved[type]++;
				MessageDirtyFlag[type] = true;
				break;
			case timeout:
				MessageTimeOut[type]++;
				MessageDirtyFlag[type] = true;
				break;
			case found:
				QueryHit++;
				break;
			case notFound:
				QueryMissed++;
				break;
		}
	}

	/**
	 * Shutdowns the Statistics facility.
	 */
	synchronized public void shutdown() {
		shutdown = true;
	}

}