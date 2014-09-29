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

public class Constants {

	/*
	 * Action time functions
	 */
	
	public static final int AT_EXCH = 1;
	
	public static final int AT_BOOTSTRAP = 2;
	
	public static final int AT_EXCH_INVIT = 3;
	
	public static final int AT_EXCH_REQ = 4;
	
	public static final int AT_EXCH_INVIT_DUPLICATE_PREDICTION_SUBSET = 5;
	
	public static final int AT_EXCH_INVIT_EXCH_PROCESS = 6;
	
	public static final int AT_EXCH_INVIT_DELETE_INDEX_TABLE = 7;
	
	public static final int AT_EXCH_INVIT_UPLOAD_DATA = 8;
	
	public static final int AT_EXCH_INVIT_READ_INDEX = 9;
	
	public static final int AT_EXCH_REQ_SEND_EXCH_REPLY = 10;
	
	public static final int AT_EXCH_REQ_WAIT_ACK = 11;
	
	public static final int AT_EXCH_REQ_INDEX_SIGNATURE = 12;
	
	public static final int AT_EXCH_REQ_DUPLICATE_DATA_TABLE = 13;
	
	public static final int AT_EXCH_REQ_EXCH_PROCESS = 14;
	
	public static final int AT_EXCH_REQ_UPLOAD_DATA = 15;
	
	public static final int AT_EXCH_REQ_READ_INDEX = 16;
	
	public static final int AT_WORK_EXCH_INVIT = 17;
	
	public static final int AT_WORK_EXCH_REQ = 18;
	
	public static final int AT_DISTR_INDEX_MODIF_PROCESS_GLOBAL= 19;
	
	public static final int AT_DISTR_INDEX_MODIF_PROCESS_LOCAL= 20;
	
	public static final int AT_DISTR_INDEX_MODIF_REMOTE_PROCESS_GLOBAL= 21;
	
	public static final int AT_DISTR_INDEX_MODIF_REMOTE_PROCESS_LOCAL= 22;
	
	public static final int AT_INDEX_MGR_INSERT_INDEX = 23;
	
	public static final int AT_MTNCE_MGR_BOOTSTRAP = 24;
	
	public static final int AT_MTNCE_FIDGET_EXCH = 25;
	
	public static final int AT_MTNCE_CLONE_RND_PEER = 26;
	
	public static final int AT_MTNCE_REPLICATE = 27;
	
	public static final int AT_MTNCE_POPULATE_DATA = 28;
}
