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

import java.util.Collection;
import java.util.Vector;

/**
 * The Bounded Vector is a FIFO queue bounded to a given amount of elements.
 * A full queue will remove the first element to add the new element.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class BoundedVector extends Vector {

	/**
	 * The default capacity.
	 */
	public static final int DEFAULT_CAPACITY = 100;

	/**
	 * The capacity.
	 */
	private int mCapacity = DEFAULT_CAPACITY;

	/**
	 * The add history.
	 */
	private Vector mHistory = new Vector();

	/**
	 * Creates a new bounded queue with the default capacity.
	 */
	public BoundedVector() {
		super(DEFAULT_CAPACITY);
	}

	/**
	 * Creates a new bounded queue with the given capacity.
	 *
	 * @param capacity the capacity.
	 */
	public BoundedVector(int capacity) {
		super(capacity);
		mCapacity = capacity;
	}

	/**
	 * Inserts the specified element at the specified position in this Vector.
	 * Shifts the element currently at that position (if any) and any subsequent elements to the right (adds one to their indices).
	 *
	 * @param index   index at which the specified element is to be inserted.
	 * @param element element to be inserted.
	 * @throws java.lang.ArrayIndexOutOfBoundsException
	 *          index is out of range (index < 0 || index > size()).
	 */
	public synchronized void add(int index, Object element) throws ArrayIndexOutOfBoundsException {
		if (super.size() == mCapacity)
			super.remove(mHistory.remove(0));
		mHistory.add(element);
		super.add(index, element);
	}

	/**
	 * Appends the specified element to the end of this Vector.
	 *
	 * @param o element to be appended to this Vector.
	 * @return true (as per the general contract of Collection.add).
	 */
	public synchronized boolean add(Object o) {
		try {
			if (super.size() == mCapacity)
				super.remove(mHistory.remove(0));
			mHistory.add(o);
			return super.add(o);
		} catch (Exception e) {
			System.err.println("BoundedVectorException: VectorSize: " + super.size() + ", HistorySize: " + mHistory.size() + ", Capacity: " + mCapacity);
			return false;
		}
	}

	/**
	 * Appends all of the elements in the specified Collection to the end of this Vector, in the order that they are returned by the specified Collection's Iterator.
	 * The behavior of this operation is undefined if the specified Collection is modified while the operation is in progress.
	 * (This implies that the behavior of this call is undefined if the specified Collection is this Vector, and this Vector is nonempty.)
	 *
	 * @param c elements to be inserted into this Vector.
	 * @return true if this Vector changed as a result of the call.
	 * @throws java.lang.ArrayIndexOutOfBoundsException
	 *                                        index out of range (index < 0 || index > size()).
	 * @throws java.lang.NullPointerException if the specified collection is null.
	 */
	public synchronized boolean addAll(Collection c) throws ArrayIndexOutOfBoundsException, NullPointerException {
		if ((super.size() + c.size()) >= mCapacity) {
			Collection col = mHistory.subList(0, super.size() + c.size() - mCapacity);
			super.removeAll(col);
			mHistory.removeAll(col);
		}
		mHistory.addAll(c);
		return super.addAll(c);
	}

	/**
	 * Inserts all of the elements in in the specified Collection into this Vector at the specified position.
	 * Shifts the element currently at that position (if any) and any subsequent elements to the right (increases their indices).
	 * The new elements will appear in the Vector in the order that they are returned by the specified Collection's iterator.
	 *
	 * @param index index index at which to insert first element from the specified collection.
	 * @param c     elements to be inserted into this Vector.
	 * @return true if this Vector changed as a result of the call.
	 * @throws java.lang.ArrayIndexOutOfBoundsException
	 *                                        index out of range (index < 0 || index > size()).
	 * @throws java.lang.NullPointerException if the specified collection is null.
	 */
	public synchronized boolean addAll(int index, Collection c) throws ArrayIndexOutOfBoundsException, NullPointerException {
		if ((super.size() + c.size()) >= mCapacity) {
			Collection col = mHistory.subList(0, super.size() + c.size() - mCapacity);
			super.removeAll(col);
			mHistory.removeAll(col);
		}
		mHistory.addAll(c);
		return super.addAll(index, c);
	}

	/**
	 * Adds the specified component to the end of this vector, increasing its size by one.
	 * This method is identical in functionality to the add(Object) method (which is part of the List interface).
	 *
	 * @param obj the component to be added.
	 */
	public synchronized void addElement(Object obj) {
		add(obj);
	}

	/**
	 * Returns the capacity of the bounded Cector.
	 *
	 * @return the capacity.
	 */
	public int capacity() {
		return mCapacity;
	}

	/**
	 * Removes all of the elements from this Vector.
	 * The Vector will be empty after this call returns (unless it throws an exception).
	 */
	public synchronized void clear() {
		super.clear();
		mHistory.clear();
	}

	/**
	 * Returns a clone of this vector. The copy will contain a
	 * reference to a clone of the internal data array, not a reference
	 * to the original internal data array of this <tt>Vector</tt> object.
	 *
	 * @return a clone of this vector.
	 */
	public synchronized Object clone() {
		try {
			return Cloner.clone(this);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Copies the components of this vector into the specified array. The
	 * item at index <tt>k</tt> in this vector is copied into component
	 * <tt>k</tt> of <tt>anArray</tt>. The array must be big enough to hold
	 * all the objects in this vector, else an
	 * <tt>IndexOutOfBoundsException</tt> is thrown.
	 *
	 * @param anArray the array into which the components get copied.
	 * @throws java.lang.NullPointerException if the given array is null.
	 */
	public synchronized void copyInto(Object anArray[]) {
		if ((super.size() + anArray.length) >= mCapacity) {
			Collection col = mHistory.subList(0, super.size() + anArray.length - mCapacity);
			super.removeAll(col);
			mHistory.removeAll(col);
		}
		mHistory.copyInto(anArray);
		super.copyInto(anArray);
	}

	/**
	 * Inserts the specified object as a component in this vector at the
	 * specified <code>index</code>. Each component in this vector with
	 * an index greater or equal to the specified <code>index</code> is
	 * shifted upward to have an index one greater than the value it had
	 * previously. <p>
	 * <p/>
	 * The index must be a value greater than or equal to <code>0</code>
	 * and less than or equal to the current size of the vector. (If the
	 * index is equal to the current size of the vector, the new element
	 * is appended to the Vector.)<p>
	 * <p/>
	 * This method is identical in functionality to the add(Object, int) method
	 * (which is part of the List interface). Note that the add method reverses
	 * the order of the parameters, to more closely match array usage.
	 *
	 * @param obj   the component to insert.
	 * @param index where to insert the new component.
	 * @throws java.lang.ArrayIndexOutOfBoundsException
	 *          if the index was invalid.
	 * @see	 #add(int, java.lang.Object)
	 * @see	 java.util.List
	 * @see #size()
	 */
	public synchronized void insertElementAt(Object obj, int index) {
		add(index, obj);
	}

	/**
	 * Removes the first occurrence of the specified element in this Vector
	 * If the Vector does not contain the element, it is unchanged.  More
	 * formally, removes the element with the lowest index i such that
	 * <code>(o==null ? get(i)==null : o.equals(get(i)))</code> (if such
	 * an element exists).
	 *
	 * @param o element to be removed from this Vector, if present.
	 * @return true if the Vector contained the specified element.
	 * @since 1.2
	 */
	public boolean remove(Object o) {
		mHistory.remove(o);
		return super.remove(o);
	}

	/**
	 * Removes from this Vector all of its elements that are contained in the
	 * specified Collection.
	 *
	 * @param c a collection of elements to be removed from the Vector
	 * @return true if this Vector changed as a result of the call.
	 * @throws java.lang.NullPointerException if the specified collection is null.
	 * @since 1.2
	 */
	public synchronized boolean removeAll(Collection c) {
		mHistory.removeAll(c);
		return super.removeAll(c);
	}

	/**
	 * Removes all components from this vector and sets its size to zero.<p>
	 * <p/>
	 * This method is identical in functionality to the clear method
	 * (which is part of the List interface).
	 *
	 * @see	#clear
	 * @see	java.util.List
	 */
	public synchronized void removeAllElements() {
		clear();
	}

	/**
	 * Removes the first (lowest-indexed) occurrence of the argument
	 * from this vector. If the object is found in this vector, each
	 * component in the vector with an index greater or equal to the
	 * object's index is shifted downward to have an index one smaller
	 * than the value it had previously.<p>
	 * <p/>
	 * This method is identical in functionality to the remove(Object)
	 * method (which is part of the List interface).
	 *
	 * @param obj the component to be removed.
	 * @return <code>true</code> if the argument was a component of this
	 *         vector; <code>false</code> otherwise.
	 * @see	java.util.List#remove(java.lang.Object)
	 * @see	java.util.List
	 */
	public synchronized boolean removeElement(Object obj) {
		return remove(obj);
	}

	/**
	 * Deletes the component at the specified index. Each component in
	 * this vector with an index greater or equal to the specified
	 * <code>index</code> is shifted downward to have an index one
	 * smaller than the value it had previously. The size of this vector
	 * is decreased by <tt>1</tt>.<p>
	 * <p/>
	 * The index must be a value greater than or equal to <code>0</code>
	 * and less than the current size of the vector. <p>
	 * <p/>
	 * This method is identical in functionality to the remove method
	 * (which is part of the List interface).  Note that the remove method
	 * returns the old value that was stored at the specified position.
	 *
	 * @param index the index of the object to remove.
	 * @throws java.lang.ArrayIndexOutOfBoundsException
	 *          if the index was invalid.
	 * @see	 #remove(int)
	 * @see	 java.util.List
	 * @see #size()
	 */
	public synchronized void removeElementAt(int index) {
		remove(index);
	}

	/**
	 * Removes from this List all of the elements whose index is between
	 * fromIndex, inclusive and toIndex, exclusive.  Shifts any succeeding
	 * elements to the left (reduces their index).
	 * This call shortens the ArrayList by (toIndex - fromIndex) elements.  (If
	 * toIndex==fromIndex, this operation has no effect.)
	 *
	 * @param fromIndex index of first element to be removed.
	 * @param toIndex   index after last element to be removed.
	 */
	protected void removeRange(int fromIndex, int toIndex) {
		Collection col = super.subList(fromIndex, toIndex);
		mHistory.removeAll(col);
		super.removeRange(fromIndex, toIndex);
	}

	/**
	 * Retains only the elements in this Vector that are contained in the
	 * specified Collection.  In other words, removes from this Vector all
	 * of its elements that are not contained in the specified Collection.
	 *
	 * @param c a collection of elements to be retained in this Vector
	 *          (all other elements are removed)
	 * @return true if this Vector changed as a result of the call.
	 * @throws java.lang.NullPointerException if the specified collection is null.
	 * @since 1.2
	 */
	public synchronized boolean retainAll(Collection c) {
		mHistory.retainAll(c);
		return super.retainAll(c);
	}

	/**
	 * Replaces the element at the specified position in this Vector with the
	 * specified element.
	 *
	 * @param index   index of element to replace.
	 * @param element element to be stored at the specified position.
	 * @return the element previously at the specified position.
	 * @throws java.lang.ArrayIndexOutOfBoundsException
	 *          index out of range
	 *          (index &lt; 0 || index &gt;= size()).
	 * @since 1.2
	 */
	public synchronized Object set(int index, Object element) {
		int idx = mHistory.indexOf(super.get(index));
		mHistory.set(idx, element);
		return super.set(index, element);
	}

	/**
	 * Sets the component at the specified <code>index</code> of this
	 * vector to be the specified object. The previous component at that
	 * position is discarded.<p>
	 * <p/>
	 * The index must be a value greater than or equal to <code>0</code>
	 * and less than the current size of the vector. <p>
	 * <p/>
	 * This method is identical in functionality to the set method
	 * (which is part of the List interface). Note that the set method reverses
	 * the order of the parameters, to more closely match array usage.  Note
	 * also that the set method returns the old value that was stored at the
	 * specified position.
	 *
	 * @param obj   what the component is to be set to.
	 * @param index the specified index.
	 * @throws java.lang.ArrayIndexOutOfBoundsException
	 *          if the index was invalid.
	 * @see	 #set(int, java.lang.Object)
	 * @see #size()
	 * @see java.util.List
	 */
	public synchronized void setElementAt(Object obj, int index) {
		set(index, obj);
	}

}
