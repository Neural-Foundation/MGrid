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
package pgrid.util.monitoring;

/**
 * This class is used only when P-Grid runs in monitored mode.
 * Therefore it needs the NBench jar file
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import ch.epfl.lsir.nbench.client.NBClient;
import pgrid.Constants;
import pgrid.interfaces.basic.PGridP2P;


public class MonitoringManager {
	
	private static final MonitoringManager SHARED_INSTANCE = new MonitoringManager();
	
	private static NBClient nbclient = null;
	private short currentPhase = -1;
	private ConcurrentHashMap<Integer, Long> monitorTimes = new ConcurrentHashMap<Integer, Long>();
	protected static final Logger LOGGER = Logger.getLogger("PGrid.MonitoringManager");
	
	private ArrayList<Short> bufferPhase = new ArrayList<Short>(); 
	
	protected MonitoringManager(){}
	
	public static void init(NBClient client){
		if (PGridP2P.sharedInstance().isInMonitoredMode()){
			nbclient = client;
			startStabilizationTimeMonitor();
		} else {
			System.err.println("P-Grid is not in Monitored mode. Please change it before !!");
		}
	}
	
	public static void startStabilizationTimeMonitor(){
		Thread t2 = new Thread("StabilizationTimeMonitor") {
			public void run() {
				LOGGER.finest("Starting StabilizationTimeMonitor Thread.");
				String currentPath = PGridP2P.sharedInstance().getLocalPath();
				long SLEEP_TIME = 30 * 1000;
				long STABILIZED_TIME = 1000 * 60 * 15;
				int times = 0;
				long startTime = System.currentTimeMillis();
				long stabilizationTime = 0;
				
				WHILE:
				while (true) {

					try {
						Thread.sleep(SLEEP_TIME);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					if (!PGridP2P.sharedInstance().getLocalPath().equals(currentPath)){
						LOGGER.finest("Changed local path from " + currentPath + " to " + PGridP2P.sharedInstance().getLocalPath());
						currentPath = PGridP2P.sharedInstance().getLocalPath();
						stabilizationTime = System.currentTimeMillis() - startTime;
						times = 0;
					} else if (++times * SLEEP_TIME > STABILIZED_TIME){
							if (stabilizationTime == 0) {
								LOGGER.finest("Node has not yet a PATH. Continuing the stabilization Monitoring ...");
							} else {
								LOGGER.finest("Node has a stabilized Path. It tooks " + stabilizationTime + " ms. Exiting Thread.");
								MonitoringManager.sharedInstance().reportStatistics("StabilizationTime", String.valueOf(stabilizationTime));
								break WHILE;
							}
					}
				}
			}
		};
		t2.setDaemon(true);
		t2.start();
	}
	
	public static MonitoringManager sharedInstance(){
		return SHARED_INSTANCE;
	}
	
	private boolean checkInit(){
		return checkInit("");
	}
	
	private boolean checkInit(String msg){
		if (!PGridP2P.sharedInstance().isInMonitoredMode()){
			return false;
		}
		if (nbclient == null){
			//System.err.println("Init method ("+MonitoringManager.class.getCanonicalName()+".init() -> " + msg + ") has not been called. Monitoring manager is yet not ready.");
			return false;
		}
		return true;
	}
	
	public void reportLocalPath(String path){
		if (checkInit()){
			nbclient.reportLocalPath(path);
		}
	}
	
	public void reportLocalCSVSize(String size){
		if (checkInit()){
			nbclient.reportLocalCSVSize(size);
		}
	}
		
	public void reportPhase(short phase){
		if (checkInit("report phase: " + phase)){
			// Report buffered phases
			synchronized (bufferPhase) {
				for (short p : bufferPhase) nbclient.reportPhase(p);
				bufferPhase.clear();
			}
			
			if (phase != currentPhase){
				nbclient.reportPhase(phase);
				currentPhase = phase;
			}
		} else {
			if (PGridP2P.sharedInstance().isInMonitoredMode()) bufferPhase.add(phase);
		}
	}
	
	public void reportStatistics(String file, String stats){
		if (checkInit("report stat: " + file + ", " + stats)){
			nbclient.reportStatistics(file, stats);
		}
	}
	
	// FIXME: handle mutlithreading
	// public void startTimer(int action, long threadId){
	public void startTimer(int action){
		if (checkInit()){
			
			if (monitorTimes.containsKey(action)){
				LOGGER.fine("startTimer called several times for the same action: " + action + ". This should not occur.");
			}
			monitorTimes.put(action, System.currentTimeMillis());			
		}
	}
	
	public void stopTimer(int action){
		if (checkInit()){
			if (monitorTimes.containsKey(action)){
				long time = System.currentTimeMillis() - (long)monitorTimes.get(action);
				reportActionTime(action, time);
				monitorTimes.remove(action);
			} else {
				LOGGER.fine("Action " + action + " not found. This should *never* happen.");
			}
			
		}
	}
	
	private void reportActionTime(int action, long time){
		if (checkInit("report action: " + action + ", " + time)){
			nbclient.reportActionTime(action, time);
		}
	}
}
