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
package pgrid.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * This class represents the application properties.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 * @deprecated 
 */
public abstract class Properties {

	/**
	 * The system line seperator.
	 */
	private static final String NEW_LINE = System.getProperty("line.separator");

	/**
	 * The default property values.
	 */
	protected String[] mDefaults = {};

	/**
	 * The property file.
	 */
	protected File mFile = null;

	/**
	 * The properties.
	 */
	protected Hashtable mProperties = new Hashtable();

	/**
	 * Constructs the application properties.
	 */
	public Properties() {
		this(null);
	}

	/**
	 * Constructs the application properties.
	 *
	 * @param defaults the default values.
	 */
	protected Properties(String[] defaults) {
		if (defaults != null) {
			mDefaults = defaults;
		}
		// create defaults
		for (int i = 0; i < mDefaults.length; i = i + 2)
			if ((!mDefaults[i].equals("")) && (!mDefaults[i].equals("#")))
				mProperties.put(mDefaults[i], mDefaults[i + 1]);
	}

	/**
	 * Initializes the properties with the given property file and properties.
	 *
	 * @param file the property file.
	 */
	synchronized public void init(String file) {
		init(file, null);
	}

	/**
	 * Initializes the properties with the given property file and properties.
	 *
	 * @param file       the property file.
	 * @param properties further initialization properties.
	 */
	synchronized public void init(String file, java.util.Properties properties) {
		_init(file);
		if (properties != null) {
			for (Enumeration enumeration = properties.propertyNames(); enumeration.hasMoreElements();) {
				String key = (String)enumeration.nextElement();
				if (mProperties.containsKey(key)) {
					mProperties.put(key, properties.getProperty(key));
				}
			}
		}
		store();
	}

	/**
	 * This really initializes the properties.
	 *
	 * @param file the property file.
	 */
	synchronized private void _init(String file) {
		mFile = new File(file);
		try {
			if (!mFile.exists()) {
				mFile.createNewFile();
				store();
			} else {
				load();
			}
		} catch (FileNotFoundException e) {
			System.err.println("Property file '" + file + "' not found or could not be created - using default values!");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Could not read/write property file '" + file + "' - using default values!");
			e.printStackTrace();
		}
	}

	/**
	 * Returns the property value as boolean.
	 *
	 * @param key the key of the property.
	 * @return the value of the property.
	 */
	public boolean getBoolean(String key) {
		if (getString(key).equals("true"))
			return true;
		else
			return false;
	}

	/**
	 * Returns the default value as boolean.
	 *
	 * @param key the key of the property.
	 * @return the default value of the property.
	 */
	public boolean getDefaultBoolean(String key) {
		for (int i = 0; i < mDefaults.length; i = i + 2) {
			if (mDefaults[i].equals(key)) {
				if (mDefaults[i + 1].equals("true"))
					return true;
				else
					return false;
			}
		}
		throw new IllegalArgumentException("'" + key + "' not found!");
	}

	/**
	 * Returns the default value as integer.
	 *
	 * @param key the key of the property.
	 * @return the default value of the property.
	 */
	public int getDefaultInteger(String key) {
		for (int i = 0; i < mDefaults.length; i = i + 2) {
			if (mDefaults[i].equals(key)) {
				int val;
				val = Integer.parseInt(mDefaults[i + 1]);
				return val;
			}
		}
		throw new IllegalArgumentException("'" + key + "' not found!");
	}

	/**
	 * Returns the default value as string.
	 *
	 * @param key the key of the property.
	 * @return the default value of the property.
	 */
	public String getDefaultString(String key) {
		for (int i = 0; i < mDefaults.length; i = i + 2) {
			if (mDefaults[i].equals(key)) {
				return mDefaults[i + 1];
			}
		}
		throw new IllegalArgumentException("'" + key + "' not found!");
	}

	/**
	 * Returns the property value as integer.
	 *
	 * @param key the key of the property.
	 * @return the value of the property.
	 */
	public int getInteger(String key) {
		int val;
		val = Integer.parseInt(getString(key));
		return val;
	}

	/**
	 * Returns the property value as string.
	 *
	 * @param key the key of the property.
	 * @return the value of the property.
	 */
	public String getString(String key) {
		return (String)mProperties.get(key);
	}

	/**
	 * Sets the property value by the delivered string.
	 *
	 * @param key   the key of the property.
	 * @param value the value of the property.
	 */
	synchronized public void setString(String key, String value) {
		if (mProperties.containsKey(key)) {
			mProperties.put(key, value);
			store();
		}
	}

	/**
	 * Loads the properties from the defined file.
	 */
	synchronized private void load() {
		try {
			BufferedReader in = new BufferedReader(new FileReader(mFile));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				if (inputLine.trim().length() == 0)
					continue;
				if (inputLine.trim().startsWith("#"))
					continue;
				String[] tokens = Tokenizer.tokenize(inputLine, "=");
				mProperties.put(tokens[0], (tokens.length == 2 ? tokens[1] : ""));
			}
			in.close();
		} catch (IOException e) {
			System.err.println("Could not load property values from property file '" + mFile.getName() + "' - using default values!");
		}
	}

	/**
	 * Stores the properties to the defined file.
	 */
	synchronized private void store() {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(mFile));
			for (int i = 0; i < mDefaults.length; i = i + 2)
				if (mDefaults[i].equals(""))
					out.write(NEW_LINE);
				else if (mDefaults[i].equals("#"))
					out.write("# " + mDefaults[i + 1] + NEW_LINE);
				else
					out.write(mDefaults[i] + "=" + getString(mDefaults[i]) + NEW_LINE);
			out.flush();
			out.close();
		} catch (IOException e) {
			System.err.println("Could not store properties to property file '" + mFile.getName() + "'!");
			e.printStackTrace();
		}
	}

}