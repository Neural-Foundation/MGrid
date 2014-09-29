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


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.h2.api.DatabaseEventListener;
import org.h2.jdbc.JdbcConnection;

public class H2ShowProgress implements DatabaseEventListener {
    
    private long last, start;
    
    public H2ShowProgress() {
        start = last = System.currentTimeMillis();
    }

    public static void main(String[] args) throws Exception {
        new H2ShowProgress().test();
    }
    
    void test() throws Exception {
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection("jdbc:h2:test;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;LOG=2", "sa", "");
        Statement stat = conn.createStatement();
        stat.execute("DROP TABLE IF EXISTS TEST");
        stat.execute("CREATE TABLE TEST(ID INT PRIMARY KEY, NAME VARCHAR)");
        PreparedStatement prep = conn.prepareStatement("INSERT INTO TEST VALUES(?, 'Test' || SPACE(100))");
        long time;
        time = System.currentTimeMillis();
        int len = 1000;
        for(int i=0; i<len; i++) {
            long last = System.currentTimeMillis();
            if(last > time+1000) {
                time = last;
                System.out.println("Inserting " + (100L*i/len) + "%");
            }
            prep.setInt(1, i);
            prep.execute();
        }
        boolean abnormalTermination = true;
        if(abnormalTermination) {
            ((JdbcConnection)conn).setPowerOffCount(1);
            try {
                stat.execute("INSERT INTO TEST VALUES(-1, 'Test' || SPACE(100))");
            } catch(SQLException e) {
            }
        } else {
            conn.close();
        }
        
        System.out.println("Open connection...");
        time = System.currentTimeMillis();
        conn = DriverManager.getConnection("jdbc:h2:test;LOG=2;DATABASE_EVENT_LISTENER='" + getClass().getName() + "'", "sa", "");
        time = System.currentTimeMillis() - time;
        System.out.println("Done after " + time + " ms");
        conn.close();
        
    }

    public void diskSpaceIsLow(long stillAvailable) throws SQLException {
        System.out.println("diskSpaceIsLow stillAvailable="+stillAvailable);
    }

    public void exceptionThrown(SQLException e) {
        e.printStackTrace();
    }

    public void setProgress(int state, String name, int current, int max) {
        long time = System.currentTimeMillis();
        if(time < last+5000) {
            return;
        }
        last = time;
        String stateName = Thread.currentThread().getName()+" : ";
        switch(state) {
        case STATE_SCAN_FILE:
            stateName += "Scan " + name;
            break;
        case STATE_CREATE_INDEX:
            stateName += "Create Index " + name;
            break;
        case STATE_RECOVER:
            stateName += "Recover";
            break;
        }
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
        }
        System.out.println("State: " + stateName + " " + (100*current/max) + "% (" + current+" of " + max + ") " + (time-start)+" ms");
    }

    public void closingDatabase() {
        System.out.println(System.currentTimeMillis()+": Closing the database");
    }

    public void init(String url) {
        System.out.println(Thread.currentThread().getName()+": Initializing the event listener for database " + url);
    }

	public void exceptionThrown(SQLException arg0, String arg1) {
		// TODO Auto-generated method stub
        System.out.println(Thread.currentThread().getName()+": SQLException: " + arg0.getMessage()+" : INFO: "+ arg1);
	}

	public void opened() {
		// TODO Auto-generated method stub
		
	}

}
