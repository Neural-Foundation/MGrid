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

import java.util.Scanner;

import p2p.index.Query;
import pgrid.core.PGridTree;
import pgrid.core.search.SearchManager;

public class HashTester {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PGridTree mHasher = new PGridTree();
		mHasher.init();
		
		String value = "";
		Scanner in = new Scanner(System.in);
		while(value!="0"){
			System.out.println("Enter Value to Hash : ");
			value = in.nextLine();
			// Create a query to retrieve all data entries starting with "Updated"
			String key = Integer.toBinaryString(value.hashCode())+"";//mHasher.findKey(value);
			System.out.println("Hash value is       : "+key);
		}

	}

}
