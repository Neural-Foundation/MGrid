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

import pgrid.GUID;

import java.util.zip.Adler32;

/**
 * This class represents a Data item signature.
 *
 * @author @author <a href="mailto:Renault John <Renault.John@epfl.ch>">Renault JOHN</a>
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class Signature {
	/**
	 * The signature
	 */
	protected String mSignature = null;

	/**
	 * Creates an empty signature.
	 */
	public Signature() {
		mSignature = GUID.getGUID().toString();
	}

	/**
	 * Creates a new signature with given string as the signature internal string.
	 *
	 * @param signature the signature string.
	 */
	public Signature(String signature) {
		mSignature = signature;
	}

	/**
	 * Tests if the given object equals this one.
	 *
	 * @param o the object to compare.
	 * @return <TT>true</TT> if equal, <TT>false</TT> otherwise.
	 */
	public boolean equals(Object o) {
		if (o == null)
			return false;
		
		if (o.getClass() != this.getClass())
			return false;
		Signature sig = (Signature)o;
		
		if (sig.toString() == null)
			return false;
		
		if (sig.toString().equals(toString()))
			return true;
		else
			return false;
	}

	/**
	 * Returns a hash code value for the object. This method is
	 * supported for the benefit of hashtables such as those provided by
	 * <code>java.util.Hashtable</code>.
	 * <p/>
	 * The general contract of <code>hashCode</code> is:
	 * <ul>
	 * <li>Whenever it is invoked on the same object more than once during
	 * an execution of a Java application, the <tt>hashCode</tt> method
	 * must consistently return the same integer, provided no information
	 * used in <tt>equals</tt> comparisons on the object is modified.
	 * This integer need not remain consistent from one execution of an
	 * application to another execution of the same application.
	 * <li>If two objects are equal according to the <tt>equals(Object)</tt>
	 * method, then calling the <code>hashCode</code> method on each of
	 * the two objects must produce the same integer result.
	 * <li>It is <em>not</em> required that if two objects are unequal
	 * according to the {@link Object#equals(Object)}
	 * method, then calling the <tt>hashCode</tt> method on each of the
	 * two objects must produce distinct integer results.  However, the
	 * programmer should be aware that producing distinct integer results
	 * for unequal objects may improve the performance of hashtables.
	 * </ul>
	 * <p/>
	 * As much as is reasonably practical, the hashCode method defined by
	 * class <tt>Object</tt> does return distinct integers for distinct
	 * objects. (This is typically implemented by converting the internal
	 * address of the object into an integer, but this implementation
	 * technique is not required by the
	 * Java<font size="-2"><sup>TM</sup></font> programming language.)
	 *
	 * @return a hash code value for this object.
	 * @see Object#equals(Object)
	 * @see java.util.Hashtable
	 */
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Returns a string represantation of the signature.
	 *
	 * @return a string represantation of the signature.
	 */
	public String toString() {
		return mSignature;
	}
}
