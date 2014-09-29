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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * This class makes deep hard copy of any {@link java.io.Serializable} object and all of its
 * {@link java.io.Serializable} member objects.<p>
 * <p/>
 * <b>Example:</b>
 * <pre>
 * Object obj = new Object();
 * Object clone = (Object)Cloner.clone(obj);
 * </pre>
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0 2003/04/15
 * @see java.io.Serializable
 */
public class Cloner {

	/**
	 * Creates a new <code>Cloner</code>.
	 */
	protected Cloner() {
		// do nothing
	}

	/**
	 * Clones the given object.
	 *
	 * @param o the object to clone.
	 * @return the clone.
	 * @throws Exception if any Exception occures
	 */
	public static Object clone(Object o) throws Exception {
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bo);
		out.writeObject(o);
		out.close();
		ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
		ObjectInputStream in = new ObjectInputStream(bi);
		Object clone = in.readObject();
		return clone;
	}

}