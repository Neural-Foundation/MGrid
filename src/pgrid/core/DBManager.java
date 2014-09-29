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
package pgrid.core;

import p2p.index.IDBDataTypeHandler;
import pgrid.Constants;
import pgrid.Type;
import pgrid.interfaces.basic.PGridP2P;
import org.logicalcobwebs.proxool.ConnectionListenerIF;
import org.logicalcobwebs.proxool.ProxoolException;
import org.logicalcobwebs.proxool.StateListenerIF;
import org.logicalcobwebs.proxool.admin.StatisticsIF;
import org.logicalcobwebs.proxool.admin.StatisticsListenerIF;
import org.logicalcobwebs.proxool.configuration.PropertyConfigurator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.WeakHashMap;

import javax.sql.rowset.serial.SerialBlob;

/**
 * <p>Title: DBManager</p>
 * <p/>
 * <p>Description: This class manages the DataBase connection and
 * transactions</p>
 *
 * @author Mark Kornfilt
 */
public class DBManager{
	public Object mLock = new Object();

	/**
	 * Batch number
	 */
	public static final int BATCH = 100000;

	/**
	 * Connection caches
	 */
	private WeakHashMap<Thread, Connection> mConnections = new WeakHashMap<Thread, Connection>();


	/**
	 * the shutdown flag for the DBIndexTable
	 */
	private boolean dbDataTableShutdownFlag = false;

	/**
	 * the shutdown flaf for the DBRoutingTable
	 */
	private boolean dbRoutingTableShutdownFlag = false;

	/**
	 * The DataBase tables
	 *
	 * @todo remove? See DBIndexTable
	 */
	public static final String INDEX_ITEMS_TABLE = "INDEX_ITEMS";

	public static final String INDEX_TABLES_TABLE = "INDEX_TABLES";

	public static final String HOSTS_TABLE = "HOSTS";

	public static final String FIDGETS_TABLE = "FIDGET_LIST";

	public static final String CONFIG_TABLE = "CONFIG";

	public static final String SIGNATURE_CACHE = "INDEX_SIGNATURE_CACHE";

	public static final String MESSAGES_TABLE = "MESSAGES_TABLE";
	
	private static int hostsTableIncId = 0;


	/**
	 * The reference to the only instance of this class (Singleton
	 * pattern). This differs from the C++ standard implementation by Gamma
	 * 
	 * et.al. since Java ensures the order of static initialization at runtime.
	 *
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */

	private static final DBManager SHARED_INSTANCE = new DBManager();
		
	protected DBManager() {
		Properties p = new Properties();
		try {
			try {
				InputStream is = ClassLoader.getSystemResourceAsStream("proxool.properties");
				if (is == null){
					// Not running from the jar.
					String path = "resources" + System.getProperty("file.separator") + "proxool.properties";
					File f = new File(path);
					if (!f.canRead()){
						Constants.LOGGER.warning(path + " not found !");
					} else {
						Constants.LOGGER.finest("DB-Connection Pool properties set from FILE:"+"resources" + System.getProperty("file.separator") + "proxool.properties");
						is = new FileInputStream(f);
					}
				}else{
					Constants.LOGGER.finest("DB-Connection Pool properties set from FILE in the JAR");
				}
				p.load(is);
				p.setProperty("jdbc-0.proxool.driver-url","jdbc:h2:file:"+Constants.DATA_DIR+"/PGridDB;DB_CLOSE_ON_EXIT=FALSE;MAX_MEMORY_UNDO=10;CACHE_SIZE=10;LOG=0;MAX_LOG_SIZE=10;TRACE_MAX_FILE_SIZE=2;TRACE_LEVEL_SYSTEM_OUT=0");
				PropertyConfigurator.configure(p);
				is.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Constants.LOGGER.finest("DB-Connection Pool properties = DEFAULT values");

				p.setProperty("jdbc-0.proxool.alias","PGridDB");
				p.setProperty("jdbc-0.proxool.driver-url","jdbc:h2:file:"+Constants.DATA_DIR+"/PGridDB;DB_CLOSE_ON_EXIT=FALSE;MAX_MEMORY_UNDO=10;CACHE_SIZE=10;LOG=0;MAX_LOG_SIZE=10;TRACE_MAX_FILE_SIZE=2;TRACE_LEVEL_SYSTEM_OUT=0");
				p.setProperty("jdbc-0.proxool.driver-class","org.h2.Driver");
				p.setProperty("jdbc-0.user","sa");
				p.setProperty("jdbc-0.password","");
				p.setProperty("jdbc-0.proxool.maximum-connection-count","100");
				p.setProperty("jdbc-0.proxool.maximum-active-time","3600000");
				p.setProperty("jdbc-0.proxool.house-keeping-test-sql","select CURRENT_DATE");
				
				PropertyConfigurator.configure(p);
			}
		} catch (ProxoolException e) {
			e.printStackTrace();
		} catch (Exception e) {
			
			Constants.LOGGER.warning("Error in setting Proxool Properties.");
			e.printStackTrace();
		} 

	}
	
	public static int getNextHostsTableIncId(){
		return hostsTableIncId++;
	}
	

	/**
	 * This creates the only instance of this class. This differs from the C++ standard implementation by Gamma et.al.
	 * since Java ensures the order of static initialization at runtime.
	 *
	 * @return the shared instance of this class.
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	public static DBManager sharedInstance() {
		return SHARED_INSTANCE;
	}

	/**
	 * Compacts the DB and commits.
	 */
	public void compactDB() {
		execSQL("CHECKPOINT");
	}

	static final int MAX_RE_INIT = 4;
	int reinit = 0;
	/**
	 * Perform the initialization of the data base manager.
	 * @param inMemory true if P-Grid should use an in memory data base
	 */
	public void init(boolean inMemory) {
		try {
			long t = System.currentTimeMillis();
			if (!checkTables()) {
				createTables("P-Grid.ddl");
			} else {
				cleanTables();
			}
			Constants.LOGGER.finest("DB_Init took :" +(System.currentTimeMillis() -t)+"ms");
		} catch (Exception e) {
			if(++reinit < MAX_RE_INIT){
				Constants.LOGGER.info(e.getMessage());
				Constants.LOGGER.info("The DB is corrupt. Starting fresh.");
				Constants.LOGGER.info("Deleting old DB files");
				cleanUp();
				init(inMemory);
			}else{
				Constants.LOGGER.severe("Error in creating the DB. "+ e.getMessage());
				PGridP2P.sharedInstance().gracefulShutdown("PGrid shutdown!! Error in creating the DB. "+ e.getMessage());
			}
		}

	}
	
	private void cleanUp(){
		File dbDir = new File(Constants.DATA_DIR);
		if ((dbDir == null) || !dbDir.isDirectory()) return;	  	      
		  final File[] files = dbDir.listFiles();
		  final int size = files.length; 
		  for (int i = 0; i < size; i++) {
		    if(!files[i].isDirectory()) {
		      if(files[i].getName().startsWith("PGridDB")){
		    	  files[i].delete();
		      }
		    } 	    
		  }	     
	}

	/**
	 * Sets the auto-commit parameter of the driver
	 *
	 * @param autoCommit the auto-commit flag
	 */
	public void setAutoCommit(boolean autoCommit) {
		try {
			Connection con = getConnection();
			con.setAutoCommit(autoCommit);
			con.close();
		} catch (SQLException ex) {
			System.err.println("problem " + ex + "\nexecuting set auto commit.\n error code: " + ex.getErrorCode());
		}
	}

	/**
	 * Gets the configuration value corresponding to the given key
	 *
	 * @param key The configuration key
	 * @return the configuration value
	 */
	public String getConfig(String key) {
		try {
			
			String query = "SELECT VALUE FROM " + CONFIG_TABLE +
			" WHERE KEY = '" + key + "'";
			
			ResultSet rs = null;
			Connection rsCon = getConnection();
			if (rsCon == null) {
				Constants.LOGGER.warning("No connection available !");
			}
			try {	
				Statement rsSt = rsCon.createStatement();
				rs = rsSt.executeQuery(preprocessSQL(query));
				rsSt.clearBatch();
			} catch (SQLException ex) {
				if (PGridP2P.sharedInstance().isInDebugMode())
					ex.printStackTrace();
				Constants.LOGGER.warning("problem " + ex + "\nexecuting query: " + query +"\nerror code: " + ex.getErrorCode());
			}
			if (rs != null && rs.next()){
				String res = rs.getString("VALUE");
				rsCon.close();
				return res;
			}
			else
				return null;
		} catch (SQLException ex) {
			return null;
		}
	}

	/**
	 * Save the configuration
	 * @param key
	 * @param value
	 */
	public void setConfig(String key, String value) {
		try {
			Connection con = getConnection();
			Statement st = con.createStatement();
			String query = "SELECT KEY FROM " + CONFIG_TABLE + " WHERE KEY='" + key + "'";
			
			ResultSet rs = null;
			Connection rsCon = getConnection();
			if (rsCon == null) {
				Constants.LOGGER.warning("No connection available !");
			}
			try {	
				Statement rsSt = rsCon.createStatement();
				rs = rsSt.executeQuery(preprocessSQL(query));
				rsSt.clearBatch();
			} catch (SQLException ex) {
				if (PGridP2P.sharedInstance().isInDebugMode())
					ex.printStackTrace();
				Constants.LOGGER.warning("problem " + ex + "\nexecuting query: " + query +"\nerror code: " + ex.getErrorCode());
			}
			
			if (rs.next()) {
				st.executeUpdate("UPDATE " + CONFIG_TABLE + " SET VALUE='" + value +
						"' WHERE KEY='" + key + "'");
			} else {
				st.executeUpdate("INSERT INTO " + CONFIG_TABLE + " VALUES('" + key + "','" + value + "')");
			}
			st.close();
			con.close();
			rsCon.close();
		} catch (SQLException ex) {
			System.err.println("problem " + ex + "\nexecuting set conf.\n error code: " + ex.getErrorCode());
			return;
		}
	}

	/**
	 * Counts the number of items returned by this SQL query
	 *
	 * @param attribute
	 *@param tableQuery The SQL query @return The number of items
	 */
	public int count(String attribute, String tableQuery) {
		String query = "select count("+attribute+") as NUM_ITEMS from " + tableQuery;
		Constants.LOGGER.finest(query +"("+Thread.currentThread().getName()+")");
		
		ResultSet rs = null;
		try {
			Connection con = getConnection();
			if (con == null) {
				Constants.LOGGER.warning("No connection available !");
			}
			Statement st = con.createStatement();
			rs = st.executeQuery(preprocessSQL(query));
			
			rs.next();
			int num_items = rs.getInt("NUM_ITEMS");
			Constants.LOGGER.finest("Count: "+ num_items);

			st.clearBatch();
			con.close();
			return num_items;
		} catch (SQLException ex) {
			if (PGridP2P.sharedInstance().isInDebugMode())
				ex.printStackTrace();
			Constants.LOGGER.warning("problem " + ex + "\nexecuting query: " + query +"\nerror code: " + ex.getErrorCode());
			return -1;
		}
	}

	/**
	 * Drops all the tables in the DB
	 */
	public void dropAll() {
		try {
			Connection con = getConnection();
			if (con == null) return;

			String[] type = {"TABLE"};
			ResultSet rs = con.getMetaData().getTables(null, null, null, type);
			while (rs.next()) {
				execSQL("DROP TABLE " + rs.getString("TABLE_NAME"));
			}
			con.close();
		} catch (SQLException ex) {
			System.err.println("problem " + ex + "\nexecuting drop all.\n error code: " + ex.getErrorCode());
		}

	}

	/**
	 * Executes this SQL query
	 *
	 * @param query The SQL query
	 * @return returns 0 if no problem or the sql error code.
	 */
	public int execSQL(String query) {

		int returnValue = 0;

		Statement st;
		try {
			Connection con = getConnection();
			
			st = con.createStatement();
			st.execute(query);
			st.clearBatch();
			st.close();
			con.close();
		
		} catch (SQLException ex) {
			returnValue = ex.getErrorCode();
		//	if (returnValue != -104) 
		//		ex.printStackTrace();
		} finally{
		}

		return returnValue;
	}

	/**
	 * Executes this DELETE SQL statement and throws the exception if deletion failed
	 *
	 * @param query The DELETE SQL statement
	 */
	public int execDeleteSQL(String query) {

		Statement st;
		int result = -1;

		try {
			Connection con = getConnection();
			if (con == null) return -401;

			st = con.createStatement();
			result = st.executeUpdate(query);
			st.clearBatch();
			st.close();
			con.close();
		} catch (SQLException ex) {
			if (PGridP2P.sharedInstance().isInDebugMode())
				Constants.LOGGER.warning("problem " + ex + "\nexecuting query: " + query + " \nerror code: " + ex.getErrorCode());
			else
				Constants.LOGGER.warning("problem " + ex + "\nexecuting query.\nerror code: " + ex.getErrorCode());
		}
		return result;
	}

	/**
	 * Executes this INSERT SQL statement and returns the auto-generated identity
	 * value for the inserted row
	 *
	 * @param table  The table in which we want to INSERT
	 * @param values The values to INSERT
	 * @return The ID of the inserted row or the SQL error
	 */
	public int insertSQL(String table, String values) {
		
		int lastId = -1;		
		try {
			Connection con = getConnection();
			
			Statement st = con.createStatement();
			st.execute("INSERT INTO " + table + " VALUES(" + values + ")");

			ResultSet rs = st.getGeneratedKeys(); 

			if (rs != null && rs.next()) 
			{ 
				lastId = rs.getInt(1); 
			}
			rs.close();
			st.clearBatch();
			st.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return lastId;
		
		
		
	}
	/**
	 * Executes this MERGE SQL statement and returns the auto-generated identity
	 * value for the inserted row
	 *
	 * @param table  The table in which we want to MERGE/UPSERT
	 * @param values The values to MERGE/UPSERT
	 * @return The ID of the inserted row or the SQL error
	 */
	public int mergeSQL(String table, String keyColumn, String values) {
		
		int lastId = -1;
		
		try {
			Connection con = getConnection();
			
			Statement st = con.createStatement();
			st.execute("MERGE INTO " + table + " KEY("+keyColumn+") VALUES(" + values + ")");

			ResultSet rs = st.getGeneratedKeys(); 

			if (rs != null && rs.next()) 
			{ 
				lastId = rs.getInt(1); 
			}
			rs.close();
			st.clearBatch();
			st.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return lastId;
	}

	/**
	 * Creates a PreparedStatement with the given Query
	 *
	 * @param preparedQuery The PreparedStatement SQL query
	 * @return The created PreparedStatement object
	 * @throws SQLException
	 */
	public PreparedStatement prepareStatement(String preparedQuery) throws SQLException {
		return getConnection().prepareStatement(preparedQuery);
	}

	/**
	 * Checks for the existence of all the required tables
	 *
	 * @return True if all the tables exist
	 */
	public boolean checkTables() {
		Vector<String> tables = new Vector<String>();
		tables.add(INDEX_ITEMS_TABLE);
		tables.add(INDEX_TABLES_TABLE);
		tables.add(HOSTS_TABLE);
		tables.add(FIDGETS_TABLE);
		tables.add(CONFIG_TABLE);
		tables.add(SIGNATURE_CACHE);
		tables.add(MESSAGES_TABLE);

		for (Iterator it = tables.iterator(); it.hasNext();) {
			if (!tableExists((String)it.next())) {
				dropAll();
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks for the existence of a specific table
	 *
	 * @param tableName The name of the table
	 * @return True if the table exists
	 */
	public boolean tableExists(String tableName) {

		try {
			Connection con = getConnection();
			if (con == null) return false;

			if (!con.getMetaData().getTables(null, null, tableName, null).next()) {
				con.close();
				return false;
			} else {
				con.close();
				return true;
			}
		} catch (SQLException ex) {
		System.err.println("Error: Table existence check: " + ex+"\n error code: " + ex.getErrorCode());
			return false;
		}
	}

	/**
	 * Creates the tables based on the schema defined in this DDL file
	 *
	 * @param ddlFileName The "Data Definition Language" file
	 */
	public void createTables(String ddlFileName) {
		InputStream inStream = getClass().getResourceAsStream("/" + ddlFileName);
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
			String line = null;
			String content = "";
			while ((line = in.readLine()) != null)
				content = content.concat(line + "\n");
			in.close();
			execSQL(String.valueOf(content));
		}	catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	protected void cleanTables() {
		execSQL("delete from HOSTS where TEMP = 1");
		 
	}

	/**
	 * Called by the DBIndexTable to shutdown the DB Server
	 */
	public void dbIndexTableShutdown() {
		dbDataTableShutdownFlag = true;
		shutdown();
	}

	/**
	 * Called by the DBRoutingTable to shutdown the DB Server
	 */
	public void dbRoutingTableShutdown() {
		dbRoutingTableShutdownFlag = true;
		shutdown();
	}

	/**
	 * Reset the db
	 */
	public void reset() {
		Vector<String> tables = new Vector<String>();
		tables.add(INDEX_ITEMS_TABLE);
		tables.add(INDEX_TABLES_TABLE);
		tables.add(HOSTS_TABLE);
		tables.add(FIDGETS_TABLE);
		tables.add(CONFIG_TABLE);
		tables.add(SIGNATURE_CACHE);
		tables.add(MESSAGES_TABLE);

		for (Iterator it = tables.iterator(); it.hasNext();) {
			execSQL("DELETE FROM " + (String)it.next());
		}

		compactDB();
	}

	/**
	 * Shuts down the Server and closes the connection
	 */
	public void shutdown() {
		dbDataTableShutdownFlag = true;
		Constants.LOGGER.info("Cleaning database and closing it.");
		try {
		execSQL("SHUTDOWN");
		} catch (Exception e) {
			Constants.LOGGER.info("Database has already shutdown.");
		}
		closeConnect();
	}

	/**
	 * Closes the connection to the DB
	 */
	private void closeConnect() {
		try {
			for (Connection con:mConnections.values()) {
				con.close();
				con = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Retruns the connection to the DB
	 * @return
	 */
	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection("proxool.PGridDB");
	}

	public void pushMessage(String GUID, byte[] msg) {
		try {
			Blob blob = new SerialBlob(msg);

			Connection con = getConnection();
			PreparedStatement st = con.prepareStatement("insert into "+MESSAGES_TABLE+" values (null, ?, ?);");

			st.setString(1, GUID);
			st.setBlob(2, blob);
			st.execute();
			st.close();
			con.close();
			compactDB();

		} catch (SQLException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}

	public byte[] popMessage(String GUID) {
		Statement st = null;
		try {
			Blob msg = null;
			Connection con = getConnection();
			st = con.createStatement();

			ResultSet rs = st.executeQuery("select MESSAGE, MESSAGE_ID from "+MESSAGES_TABLE+" where GUID = '"+GUID+"';");

			if (rs.next()) {
				byte[] result = null;
				msg = rs.getBlob("MESSAGE");
				result = msg.getBytes(0, (int) msg.length());
				
				st.execute("delete from "+MESSAGES_TABLE+" where MESSAGE_ID = "+rs.getInt("MESSAGE_ID")+";");
				st.close();
				con.close();
				compactDB();

				return result;
			} else {

				Constants.LOGGER.info("No message for GUID: "+GUID);
				return null;
			}

		} catch (SQLException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
		return null;
	}

	/**
	 * Filter the SQL string of Java SQL Escape clauses.
	 *
	 * Currently implemented Escape clauses are those mentioned in 11.3
	 * in the specification. Basically we look through the sql string for
	 * {d xxx}, {t xxx} or {ts xxx} in non-string sql code. When we find
	 * them, we just strip the escape part leaving only the xxx part.
	 * So, something like "select * from x where d={d '2001-10-09'}" would
	 * return "select * from x where d= '2001-10-09'".
	 * @param sql
	 */
	static public String escapeSQL(String sql)
	{
		StringBuffer sb = new StringBuffer(sql.length());
		char[] s = sql.toCharArray();
		for(int i = 0; i < sql.length(); i++){
			sb.append(s[i]);
			if(s[i] =='\'') sb.append('\'');

		}
		s = null;
		return new String(sb.toString());
	}
	
	public String preprocessSQL(String sql){
		StringBuffer sb = new StringBuffer(sql.length());
		char[] s = sql.toCharArray();
		for(int i = 0; i < sql.length(); i++){
			sb.append(s[i]);
			if(s[i] =='\\') sb.append('\\');

		}
		s = null;
		return new String(sb.toString());
	}

	public void dropMessages() {
		try {
			Connection con = getConnection();
			Statement st = con.createStatement();

			st.execute("TRUNCATE TABLE "+MESSAGES_TABLE+";");
			st.close();
			con.close();
			compactDB();

		} catch (SQLException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}
	/**
	 * The hashtable of all registered data type handlers.
	 */
	protected Map<String,IDBDataTypeHandler> mDBDataTypeHandlers = new Hashtable<String, IDBDataTypeHandler>();
	
	/**
	 * Returns the collection of registered dbDataTypeHandlers
	 * @return
	 */
	public Map getDBDataTypeHandlers(){
		return mDBDataTypeHandlers;
	}
	/**
	 * This function is used for registering an dbDataTypeHandler corresponding to a type;
	 * @param type
	 * @param handler
	 */
	public void registerDBTypeHandler(Type type, IDBDataTypeHandler handler ){
		if ((type == null) || (handler == null))
			throw new NullPointerException();

		if(mDBDataTypeHandlers.get(type.toString()) == null){
			mDBDataTypeHandlers.put(type.toString(), handler);
			handler.init();
		}
	}

	/**
	 * Give a type(string) this function returns the corresponding registered dbDataTypeHandler;
	 * @param typeString
	 * @return
	 */
	public IDBDataTypeHandler getDBTypeHandler(String typeString){
		if(mDBDataTypeHandlers.get(typeString) != null){
			return (IDBDataTypeHandler)mDBDataTypeHandlers.get(typeString);
		}else{
			return null;
		}
	}
	
}

class MyStateListener implements StateListenerIF, ConnectionListenerIF, StatisticsListenerIF{

	public void upStateChanged(int arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onBirth(Connection arg0) throws SQLException {
		// TODO Auto-generated method stub
		//System.out.println("MyStateListener.onBirth(): " + arg0);
	}

	public void onDeath(Connection arg0) throws SQLException {
		// TODO Auto-generated method stub
		//System.out.println("MyStateListener.onDeath(): " + arg0);
	}

	public void onExecute(String arg0, long arg1) {
		// TODO Auto-generated method stub
		//System.out.println("MyStateListener.onExecute(): " + arg0);
	}

	public void onFail(String arg0, Exception arg1) {
		// TODO Auto-generated method stub
		System.out.println("MyStateListener.onFail(): " + arg0 + " : " + arg1.getMessage());
		
	}

	public void statistics(String arg0, StatisticsIF arg1) {
		// TODO Auto-generated method stub
		//System.out.println("MyStateListener.statistics(): " + arg0);
		//System.out.println("MyStateListener.statistics(): AverageActiveCount: " + arg1.getAverageActiveCount());
		//System.out.println("MyStateListener.statistics(): ServedCount:" + arg1.getServedCount());
	}

	public void onDeath(Connection arg0, int arg1) throws SQLException {
		System.out.println("MyStateListener.onDeath(): " + arg0.toString());
	}
	
}
