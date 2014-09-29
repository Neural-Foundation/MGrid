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
package pgrid.core.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.*;
import java.util.logging.Logger;

import com.google.uzaygezen.core.BitVector;

import mgrid.core.MGridUtils;
import mgrid.core.Point;

import p2p.index.events.NoSuchTypeException;
import pgrid.Constants;
import pgrid.IndexEntry;
import pgrid.GUID;
import pgrid.PGridHost;
import pgrid.PGridKey;
import pgrid.Properties;
import pgrid.Type;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.interfaces.index.PGridIndexFactory;
import pgrid.interfaces.index.XMLSimpleIndexEntry;


public class CSVIndexTable{

	private Signature mSignature;

	private String mCSVFileName;

	private String justFileName;

	private File mCSVFile;

	private PrintWriter mWriter;

	private BufferedReader mReader;

	private Object Lock = new Object();
 
  /**
  *  read the default values from ini file
  */
	private static PGridP2P mPGridP2P = PGridP2P.sharedInstance();	
	private static final String TYPE_NAME = mPGridP2P.propertyString(Properties.TYPE_NAME);	
	private static final int PORT_NUMBER = Integer.parseInt(mPGridP2P.propertyString(Properties.PORT_NUMBER));
	
	/**
	 * Lock
	 */
	private boolean isFileLocked=false;
	private String lockThread;

	private void acquireFileLock() {
		long tWait = System.currentTimeMillis();
		while(isFileLocked){
			if(lockThread.equals(Thread.currentThread().getName())) break;

			long duration = (System.currentTimeMillis() - tWait)/1000;
			System.out.println("Waiting for the Lock : "+justFileName+" by "+Thread.currentThread().getName() +" : ("+duration+" secs)");
			System.out.println("Lock currently held by "+lockThread);
			Constants.LOGGER.finest("Waiting for the Lock : "+justFileName+" by "+Thread.currentThread().getName() +" : ("+duration+" secs)");
			Constants.LOGGER.finest("Lock currently held by "+lockThread);

			try {
				Thread.currentThread().sleep(5*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		synchronized (Lock) {
			isFileLocked = true;
			lockThread = Thread.currentThread().getName();
			putThread(justFileName);
		}
	}
	private synchronized void releaseFileLock() {
		if(isFileLocked) isFileLocked = false;
		removeThread(justFileName);
	}

	private int count;

	public int count(){
		return count;
	}

	public String getCSVFileName(){
		return mCSVFileName;
	}

	public File getCSVFile(){
		return mCSVFile;
	}

	public void empty(){
		count = 0;
		if(mCSVFile.exists()) mCSVFile.delete();
		mCSVFile = new File(mCSVFileName);
	}

	public void openFileForReading() throws Exception{
		try {
			acquireFileLock();
			if(!mCSVFile.exists()) 
				mCSVFile.createNewFile();
			mReader = new BufferedReader(new FileReader(mCSVFileName));
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public void closeFileOnReading(){
		try {
			releaseFileLock();
			if(mReader!=null)mReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void openFileForWriting() throws Exception{
		try {
			acquireFileLock();
			if(mCSVFile.exists()) 
				mWriter =  new PrintWriter(new FileWriter(mCSVFile,true));
			else {
				mCSVFile.createNewFile();
				mWriter =  new PrintWriter(new FileWriter(mCSVFile));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public void closeFileForWriting(){
		releaseFileLock();
		if(mWriter != null)mWriter.close();
	}

	public CSVIndexTable() {
		this(true);
	}

	public CSVIndexTable(boolean append) {
		this("LOCAL.csv",append);
	}

	public CSVIndexTable(String filename) {
		this(filename,true);
	}

	public CSVIndexTable(String filename, boolean append) {
		justFileName = filename;
		mCSVFileName = Constants.CSV_DIR+filename;
		mCSVFile = new File(mCSVFileName);
		new File(mCSVFile.getParent()).mkdirs();

		if(!append){
			if(mCSVFile.exists()) mCSVFile.delete();
		}
		mCSVFile = new File(mCSVFileName);
		count=0;

		if(!mCSVFile.exists()){
			try {
				mCSVFile.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		try {
			openFileForReading();
			while(getNextLineNoCheck()!=null) count++;
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			closeFileOnReading();
		}
	}

	public synchronized void addIndexEntry(IndexEntry dataItem) {
		if(dataItem == null) return;
		try {
			addIndexEntry(toCSVIndexEntry(dataItem));
		} catch (Exception e) {
			Constants.LOGGER.warning("Ignoring Wrongly formed IndexEntry : "+toCSVIndexEntry(dataItem)+"\n"+e.getMessage());
			printThreads(justFileName);
		}
	}

	public synchronized void addIndexEntry(String dataItem) {
		if(dataItem == null) return;
		if(dataItem.split(",").length != 5){
			Constants.LOGGER.warning("Ignoring Wrongly formed StringEntry : "+dataItem);
			printThreads(justFileName);
			return;
		}
		mWriter.println(dataItem);
		mWriter.flush();
		mSignature = null;
		count++;
	}

	public synchronized void addIndexEntryNoCheck(String dataItem) {
		mWriter.println(dataItem);
		mWriter.flush();
		mSignature = null;
		count++;
	}
	private StringBuffer sb = new StringBuffer(1000);
	/**
	 * Converts the IndexEntry representation to the CSVFileEntry Format
	 * @param dataItem
	 * @return
	 */
		private String toCSVIndexEntry(IndexEntry dataItem){
		// Assuming CSV format to be
		// KEY,X,Y,ID,DATA,dGUID,TYPE_NAME,HOST_IP,HOST_PORT,hGUID
		String csvEntry;
		String CSV_DELIMITER = ",";
		String CSV_DELIMITER_MODIFIER = "###";
		String KEY = "";
	//	Long X = 0L;
	//	Long Y = 0L;
		Long ID = 0L;
	//	String DATA = "";
		String dGUID = "";
	//	String TYPE_NAME = "";
		String HOST_IP = "";
//		int HOST_PORT = 0;
		String hGUID = "";
		

		sb.setLength(0);

		try {
			KEY = dataItem.getKey().toString();
		//	DATA = dataItem.getData().toString().replace(CSV_DELIMITER, CSV_DELIMITER_MODIFIER);
			dGUID = dataItem.getGUID().toString();
		//	TYPE_NAME = dataItem.getTypeString();
			HOST_IP = ((PGridHost)dataItem.getPeer()).getAddressString();
		//	HOST_PORT = ((PGridHost)dataItem.getPeer()).getPort();
			hGUID = ((PGridHost)dataItem.getPeer()).getGUID().toString();
		//	X = dataItem.getPoint().x;
		//	Y = dataItem.getPoint().y;
			ID = dataItem.getPoint().id;
		} catch (Exception e) {
			csvEntry = sb.append(KEY).append(CSV_DELIMITER)
		//	.append(X).append(CSV_DELIMITER)
		//	.append(Y).append(CSV_DELIMITER)
			.append(ID).append(CSV_DELIMITER)
		//	.append(DATA).append(CSV_DELIMITER)
			.append(dGUID).append(CSV_DELIMITER)
		//	.append(TYPE_NAME).append(CSV_DELIMITER)
			.append(HOST_IP).append(CSV_DELIMITER)
		//	.append(HOST_PORT).append(CSV_DELIMITER)
			.append(hGUID).toString().replace("\r", "@###").replace("\n", "@@##");

			Constants.LOGGER.warning("Misformed IndexEntry in file("+justFileName+"): -> " + csvEntry +"\n"+e.getMessage());
			printThreads(justFileName);
			if(dataItem.getPeer() == null) System.err.println("peer is null");
		}finally{
			csvEntry = sb.append(KEY).append(CSV_DELIMITER)
		//	.append(X).append(CSV_DELIMITER)
	//		.append(Y).append(CSV_DELIMITER)
			.append(ID).append(CSV_DELIMITER)
	//		.append(DATA).append(CSV_DELIMITER)
			.append(dGUID).append(CSV_DELIMITER)
		//	.append(TYPE_NAME).append(CSV_DELIMITER)
			.append(HOST_IP).append(CSV_DELIMITER)
	//		.append(HOST_PORT).append(CSV_DELIMITER)
			.append(hGUID).toString().replace("\r", "@###").replace("\n", "@@##");
		}
		return csvEntry;
	}



	public synchronized  p2p.index.IndexEntry getNextIndexEntry(){
		String line = getNextLine();
		return stringToIndexEntry(line);
	}

	public synchronized String getNextLine(){
		if(!mCSVFile.exists()) return null;
		String line=null;
		try {
			try {
				line = mReader.readLine();;
			} catch (Exception e) {
				Constants.LOGGER.warning("Problem in reading the file: ("+justFileName+ ").Ignoring and continuing");
				e.printStackTrace();
				return null;
			}
			if(line == null) return null;
			if(line.split(",").length!=5){
				System.out.println("line length ="+line.split(",").length);
				Constants.LOGGER.warning("Ignoring Wrongly formed Entry getNextLine()("+justFileName+") : "+line);
				printThreads(justFileName);
				return getNextLine();
			}

			return line;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public synchronized String getNextLineNoCheck(){
		if(!mCSVFile.exists()) return null;
		String line;
		try {
			line = mReader.readLine();
			if(line == null) return null;
			return line;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private p2p.index.IndexEntry stringToIndexEntry(String s){
		p2p.index.IndexEntry ie = null;
		try {
			if(s == null) return null;
			String[] tkns = s.split(",");
			GUID dGuid = GUID.getGUID(tkns[2]);
			Type type = (Type) PGridIndexFactory.sharedInstance().getTypeByString(TYPE_NAME);
			PGridKey key = new PGridKey(tkns[0]);
			PGridHost host = PGridHost.getHost(tkns[4], tkns[3], String.valueOf(PORT_NUMBER));
			BitVector[]  xy = MGridUtils.HilbertInverseConvertor(Long.parseLong(key.toString(), 2));
			Long x = xy[0].toExactLong();
			Long y = xy[1].toExactLong();
			Long id = Long.parseLong(tkns[1]);
			Point data = new Point(x, y, id);
			ie = IndexManager.getInstance().createIndexEntry(dGuid, type, key, host, data);
		} catch (Exception e) {
			Constants.LOGGER.warning("Misformed IndexEntry: -> " + s);
			printThreads(justFileName);
			e.printStackTrace();
		}

		return ie;
	}
	public Signature getSignature() {
		if(mSignature == null){
			mSignature = new Signature();
		}
		return mSignature;
	}

	public void setSignature(Signature signature) {
		mSignature = signature;
	}

	public void delete(){
		Constants.LOGGER.finest("Deleting CSVFile: "+justFileName+" ("+count+")");
		count = 0;
		if(mCSVFile.exists()){
			mCSVFile.delete();
		}
	}

	public boolean exists(){
		return mCSVFile.exists();
	}

	/**
	 * Returns random subset formed from the CSVIndexTable
	 * @param length
	 * @return
	 */
	public DBIndexTable randomSubSet(int length){
		CSVIndexTable csvTable = new CSVIndexTable("RSSet_"+PGridP2P.sharedInstance().getLocalHost().toHostString().replace(':', '_')+".csv",false);
		TempDBIndexTable table = new TempDBIndexTable();
		try {
			openFileForReading();
			csvTable.openFileForWriting();
			p2p.index.IndexEntry ie = null;
			int count = 0;
			while(((ie=getNextIndexEntry())!=null) && ++count<=length){
				table.sequentialAdd((IndexEntry)ie);
				csvTable.addIndexEntryNoCheck(ie.getGUID().toString()+","+ie.getKey().toString());
			}
			table.flushInsert();
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			csvTable.closeFileForWriting();
			closeFileOnReading();
		}
		return table;
	}

	/**
	 * Returns random subset formed from the CSVIndexTable
	 * @param length
	 * @return
	 */
	public CSVIndexTable randomSubSetCSV(int length){
		CSVIndexTable csvTable = new CSVIndexTable("RSSet_"+PGridP2P.sharedInstance().getLocalHost().toHostString().replace(':', '_')+".csv",true);
		return csvTable;
	}

	/**
	 * This function will change the contents of (this) file to
	 * be same as the contents of 
	 * @param srcCsv
	 */
	public void changeTo(CSVIndexTable srcCsv){
		count = srcCsv.count();
		mCSVFile.delete();
		if(!srcCsv.getCSVFile().renameTo(mCSVFile)){
			Constants.LOGGER.severe(srcCsv.getCSVFile()+":Renaming of File failed:"+mCSVFile);
		}else{
			Constants.LOGGER.finest(srcCsv.getCSVFile()+":Renaming of File success:"+mCSVFile);
		}
	}

	private static Map<String,Set<String>> threads = new HashMap<String, Set<String>>();

	private static void putThread(String fName){
		if(!threads.containsKey(fName)){
			threads.put(fName, new TreeSet<String>());
		}

		threads.get(fName).add(Thread.currentThread().getName());
	}

	private static void removeThread(String fName){
		threads.get(fName).remove(Thread.currentThread().getName());
	}

	private static void printThreads(String fName){
		System.err.println("*****************");
		System.err.println(fName + threads.get(fName));
		System.err.println("-----------------");
	}
	
	public String getJustFileName() {
		return justFileName;
	}
	public String getIndexEntriesAsXML() {

		StringBuffer resultset = new StringBuffer(256);
		try {
			openFileForReading();
			IndexEntry ie = null;
			while((ie = (IndexEntry)getNextIndexEntry()) != null){
				resultset.append(XMLSimpleIndexEntry.toXMLString("", "\n", ie.getGUID().toString(), ie.getTypeString(), ie.getKey().toString(), (PGridHost)ie.getPeer(), ie.getKey().toString(), ie.getPoint() ));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			closeFileOnReading();
		}
		return resultset.toString();
	}

}
