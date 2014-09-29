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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * $Id: createFiles.java,v 1.1 2005/11/07 16:56:40 rschmidt Exp $
 * <p/>
 * Copyright (c) 2002 The P-Grid Team,
 * All Rights Reserved.
 * <p/>
 * This file is part of the P-Grid package.
 * P-Grid homepage: http://www.p-grid.org/
 * <p/>
 * The P-Grid package is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 * <p/>
 * This package is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file LICENSE.
 * If not you can find the GPL at http://www.gnu.org/copyleft/gpl.html
 */
public class createFiles {

	private static final char[] FILENAME_CHARS = new char[26 * 2 + 10];
	private static final int FILENAME_LENGTH = 20;
	private static final int FILES = 25000;
	private static final String FILENAME = "bin/Files.dat";

	private static final int IN_MAX = 66;
	private static final String IN_PREFIX = "in1sun";
	private static SecureRandom rnd = null;

	static {
		rnd = new SecureRandom();
		int charIndex = 0;
		for (int i = (int)'A'; i <= 'Z'; i++) {
			FILENAME_CHARS[charIndex++] = (char)i;
		}
		for (int i = (int)'a'; i <= 'z'; i++) {
			FILENAME_CHARS[charIndex++] = (char)i;
		}
		for (int i = (int)'0'; i <= '9'; i++) {
			FILENAME_CHARS[charIndex++] = (char)i;
		}
	}

	static public void atLCA(String pFile) {
		// open and read peer file
		Vector peers = new Vector();
		try {
			File peerFile = new File(pFile);
			BufferedReader reader = new BufferedReader(new FileReader(peerFile));
			char[] content = new char[(int)peerFile.length()];
			reader.read(content);
			String peerString = String.valueOf(content);
			StringTokenizer strTokenizer = new StringTokenizer(peerString, "\r\n");
			while (strTokenizer.hasMoreElements()) {
				String peer = (String)strTokenizer.nextElement();
				if (!peer.startsWith("#")) {
					peers.add(peer);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			System.exit(-1);
		}

		// create shared files
		for (Iterator it = peers.iterator(); it.hasNext();) {
			String peer = (String)it.next();
			for (int i = 1; i <= 2; i++) {
				File dir = new File(peer + "-" + i + "/Shared");
				System.err.println("dir: " + dir.getAbsolutePath());
				dir.mkdirs();
				File[] files = dir.listFiles();
				// delete existing files
				if (files != null) {
					for (int j = 0; j < files.length; j++) {
						files[j].delete();
					}
				}
				// create files
				try {
					for (int j = 0; j < FILES; j++) {
						if (((i % 2) + 1) == 1)
							new File(dir, peer + "-" + Integer.toHexString(i + j) + ".pdf").createNewFile();
						else if (((i % 2) + 1) == 2)
							new File(dir, peer + "-" + Integer.toHexString(i + j) + ".ps").createNewFile();
					}
				} catch (IOException e) {
					e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
				}
			}
		}
	}

	static public void atIN() {
		// create shared files
		for (int i = 1; i <= IN_MAX; i++) {
			File dir = new File(IN_PREFIX + i + "/Shared");
			System.err.println("dir: " + dir.getAbsolutePath());
			dir.mkdirs();
			File[] files = dir.listFiles();
			// delete existing files
			if (files != null) {
				for (int j = 0; j < files.length; j++) {
					files[j].delete();
				}
			}
			// create files
			int fileTypes = 6;
			try {
				for (int j = 0; j < FILES; j++) {
					if ((i % fileTypes) == 1)
						new File(dir, randomFilename("", ".pdf", FILENAME_LENGTH)).createNewFile();
					else if ((i % fileTypes) == 2)
						new File(dir, randomFilename("", ".ps", FILENAME_LENGTH)).createNewFile();
					else if ((i % fileTypes) == 3)
						new File(dir, randomFilename("", ".txt", FILENAME_LENGTH)).createNewFile();
					else if ((i % fileTypes) == 4)
						new File(dir, randomFilename("", ".zip", FILENAME_LENGTH)).createNewFile();
					else if ((i % fileTypes) == 5)
						new File(dir, randomFilename("", ".gif", FILENAME_LENGTH)).createNewFile();
					else if ((i % fileTypes) == 0)
						new File(dir, randomFilename("", ".jpg", FILENAME_LENGTH)).createNewFile();
				}
			} catch (IOException e) {
				e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			}
		}
	}

	static void forPlanetLab() {
		File file = new File(FILENAME);
		try {
			if (!file.createNewFile()) {
				return;
			}
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));

			for (int i = 0; i < FILES; i++) {
				String name = randomFilename("", "", FILENAME_LENGTH) + "\n";
				out.write(name.getBytes());
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}

	}

	static public void main(String[] args) {
		forPlanetLab();
	}

	static private String randomFilename(String prefix, String postfix, int len) {
		String rndStr = "";

		for (int i = 0; i < len; i++)
			rndStr += Character.toString(FILENAME_CHARS[rnd.nextInt(FILENAME_CHARS.length)]);
		return prefix + rndStr + postfix;
	}

}
