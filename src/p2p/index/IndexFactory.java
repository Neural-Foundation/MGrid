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

package p2p.index;

import p2p.index.events.NoSuchTypeException;
import p2p.basic.P2P;
import p2p.basic.GUID;

import java.util.Collection;

import mgrid.core.Point;

/**
 * Abstract Factory (GoF) that defines the operations that create various
 * objects of the Index subsystem. It is recommended to instantiate
 * such types only through a concrete implementation of this factory
 * to avoid hard-coding direct references to them.
 * This class provides static methods to find concrete factories
 * using the reflection API to further decouple the subsystem from its
 * client.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public abstract class IndexFactory {

	/**
	 * Create a IndexEntry instance compatible with the Index implementation.
	 *
	 * @param type the data item's type
	 * @param data the encapsulated data
	 * @return a IndexEntry instance
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public abstract IndexEntry createIndexEntry(Type type, Object data) throws NoSuchTypeException;
	/**
	 * Create a Query instance compatible with the Index implementation.
	 *
	 * @param type        the Type of items the query is for
	 * @param queryString the string object that defines the query
	 * @return a Query instance
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public abstract Query createQuery(Type type, String queryString) throws NoSuchTypeException;

	/**
	 * Create a Query instance compatible with the Index implementation.
	 *
	 * @param type        the Type of items the query is for
	 * @param queryString the string object that defines the query
	 * @param references  To which other queries this query will refer. If no linked queries are needed,
	 * use <code>createQuery(Type type, String queryString)</code> instead or leave <code>references</code> null
	 * @return a Query instance
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public abstract Query createQuery(Type type, String queryString, Collection<GUID> references) throws NoSuchTypeException;

	/**
	 * Create a Query instance compatible with the Storage implementation.
	 *
	 * @param type        	the Type of items the query is for
	 * @param lowerBound	the string object that defines the lower bound of the query
	 * @param higherBound	the string object that defines the higher bound of the query
	 * @return a Query instance
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public abstract Query createQuery(Type type, String lowerBound, String higherBound) throws NoSuchTypeException;

	/**
	 * Create a Query instance compatible with the Storage implementation.
	 *
	 * @param type        	the Type of items the query is for
	 * @param lowerBound	the string object that defines the lower bound of the query
	 * @param higherBound	the string object that defines the higher bound of the query
	 * @param references  To which other queries this query will refer. If no linked queries are needed,
	 * use <code>createQuery(Type type, String lowerBound, String higherBound)</code> instead or leave
	 * <code>references</code> null.
	 * @return a Query instance
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public abstract Query createQuery(Type type, String lowerBound, String higherBound, Collection<GUID> references) throws NoSuchTypeException;

	/**
	 * Create a Query instance compatible with the Storage implementation includes the original co-ordinates
	 *
	 * @param type        	the Type of items the query is for
	 * @param lowerBound	the string object that defines the lower bound of the query
	 * @param higherBound	the string object that defines the higher bound of the query
	 * @param references  To which other queries this query will refer. If no linked queries are needed,
	 * use <code>createQuery(Type type, String lowerBound, String higherBound)</code> instead or leave
	 * <code>references</code> null.
	 * @param origxMin
	 * @param origxMax
	 * @param origyMin
	 * @param origyMax
	 * @return a Query instance
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public abstract Query createQuery(Type type, String lowerBound,
			String upperBound, Long xmin, Long xmax, Long ymin, Long ymax) throws NoSuchTypeException;
	
	/**
	 * Create the concrete Storage implementation.
	 *
	 * @param p2p the P2P implementation.
	 * @return the Storage implementation.
	 */
	public abstract Index createIndex(P2P p2p);

	/**
	 * Creates a Type instance compatible with the Storage implementation.
	 *
	 * @param type an application-specific type to encapsulate
	 * @return a Type instance
	 */
	public abstract Type createType(String type);

	/**
	 * Registers a Type Handler instance for a given Type.
	 *
	 * @param type an application-specific type to encapsulate
	 * @param handler an application-specific type handler to encapsulate
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public abstract void registerTypeHandler(Type type, TypeHandler handler) throws NoSuchTypeException;


	

}
