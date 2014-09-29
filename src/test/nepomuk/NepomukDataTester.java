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
package test.nepomuk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;

public class NepomukDataTester {

	
//	private static final String DATA_FILE = "nepomuk-testdata.dat";
//	private static final String DATA_FILE = "nepomuk-rdf-bib.dat";
	public static final String DATA_FILE = "data.dat";

	private int fileSize = -1;
	private final SecureRandom rnd = new SecureRandom();
	private String filename;
	
	public NepomukDataTester(){
		this(DATA_FILE);
	}
	
	public NepomukDataTester(String filename){
		rnd.setSeed(System.currentTimeMillis());
		this.filename = filename;
		if (!(new File(filename)).canRead()){
			throw new Error(new File(filename).getAbsolutePath() + " is not readable. Stopping now.");
		}
		
		this.fileSize = this.getFileSize(filename);
		System.out.println("NepomukDataTester.NepomukDataTester(): " + "File: " + filename);
		System.out.println("NepomukDataTester.NepomukDataTester(): " + "Size: " + fileSize);
	}
	
	
	private int getFileSize(String filename) {
		int counter = 0;
		try {
			// Get file length
			//InputStream inStream = ClassLoader.getSystemResourceAsStream(filename);
			InputStream inStream = new FileInputStream(filename);
			BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
			String inputLine;
		
			while ((inputLine = in.readLine()) != null) counter++;
			in.close();
			inStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return counter;
	}
	
	public ArrayList<Pair> getKeyValuePairs(int amount){
		
		if (amount == 0) return new ArrayList<Pair>();
		
		int[] index = new int[amount];
			for (int i=0; i<amount; i++ ){
				index[i] = rnd.nextInt(fileSize-1);
			}
			Arrays.sort(index);
			
		return getPairLines(index);
	}
	
	
	public ArrayList<Triple> getRandomTriples (int amount){
		if (amount == 0) return new ArrayList<Triple>();
		
		int[] index = new int[amount];
			for (int i=0; i<amount; i++ ){
				index[i] = rnd.nextInt(fileSize-1);
			}
			Arrays.sort(index);
			
		return getTripleLines(index);
	}
	
	private ArrayList<Triple> getTripleLines(int[] randomLine){
		
		if (randomLine.length == 0) return null;
		
		
		String inputLine = null;
		int counter = 0;

		ArrayList<Triple> triple = new ArrayList<Triple>();
		
		int nextIndex = 0;
		int nextLine = randomLine[nextIndex];
		try {
			InputStream inStream = new FileInputStream(this.filename);
			BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
			
			while ((inputLine = in.readLine()) != null) {

				while (counter == nextLine){
					String[] parts = inputLine.split("##SEP##");
					
					if (parts.length != 3) {
						throw new Error("Bad file format. Expecting one triple per line: <subject>##SEP##<predicate>##SEP##<object>");
					}
					
					triple.add(new Triple(parts[0], parts[1], parts[2]));
					if (nextIndex == (randomLine.length -1)) {
						break;
					}
					nextLine = randomLine[++nextIndex];

				}
				counter++;
			}

			in.close();
			inStream.close();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		return triple;
	}
	
	
	private ArrayList<Pair> getPairLines(int[] randomLine){
		
		if (randomLine.length == 0) return null;
		

		String inputLine = null;
		int counter = 0;

		ArrayList<Pair> pair = new ArrayList<Pair>();
		
		int nextIndex = 0;
		int nextLine = randomLine[nextIndex];
		try {
			
			//InputStream inStream = ClassLoader.getSystemResourceAsStream(this.filename);
			InputStream inStream = new FileInputStream(this.filename);
			BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
		
			while ((inputLine = in.readLine()) != null) {

				
				while (counter == nextLine){
					//pair.add(new Pair(inputLine.split(" ", 2)[0], inputLine.split(" ", 2)[1]));
					pair.add(new Pair(inputLine, inputLine));
					if (nextIndex == (randomLine.length -1)) {
						break;
					}
					nextLine = randomLine[++nextIndex];

				}
				counter++;
			}

			in.close();
			inStream.close();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		return pair;
	}
	
	
	public static void main (String args[]){
		NepomukDataTester tester = new NepomukDataTester("/tmp/Files.dat");
		ArrayList<Triple> triples = new ArrayList<Triple>();
		triples = tester.getRandomTriples(2);
		
		for (Triple t : triples){
			System.err.println("s: " + t.subject + ", p: " + t.predicate + ", o: " + t.object);
		}
	}
	
}
