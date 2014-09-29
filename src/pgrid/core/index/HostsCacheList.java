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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import pgrid.core.DBManager;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.Constants;

public class HostsCacheList {
	private static HostsCacheList SHARED_INSTANCE;

	private static void init(){
		if(SHARED_INSTANCE == null){
			SHARED_INSTANCE = new HostsCacheList();
		}
	}

	private HostsCacheList(){

		String query = "SELECT HOST_ID,GUID FROM HOSTS";
			
		ResultSet rs = null;
		Connection rsCon = null;
		try {
			rsCon = DBManager.sharedInstance().getConnection();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		if (rsCon == null) {
			Constants.LOGGER.warning("No connection available !");
		}
		try {	
			Statement rsSt = rsCon.createStatement();
			rs = rsSt.executeQuery(DBManager.sharedInstance().preprocessSQL(query));
			rsSt.clearBatch();
		} catch (SQLException ex) {
			if (PGridP2P.sharedInstance().isInDebugMode())
				ex.printStackTrace();
			Constants.LOGGER.warning("problem " + ex + "\nexecuting query: " + query +"\nerror code: " + ex.getErrorCode());
		}
		
		try {
			while(rs.next()){
				int id = rs.getInt("HOST_ID");
				String guid = rs.getString("GUID");
				hostsMap.put(guid,id);
			}
			rsCon.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static Map<String, Integer> hostsMap = new HashMap<String, Integer>();
	
	public static synchronized boolean containsKey(String s){
		init();
		return hostsMap.containsKey(s);
	}
	
	public static synchronized void put(String k, Integer v){
		init();
		hostsMap.put(k, v);
	}
	
	public static synchronized Integer get(String k){
		init();
		return hostsMap.get(k);
	}


}
