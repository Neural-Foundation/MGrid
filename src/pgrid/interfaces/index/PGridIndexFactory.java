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

package pgrid.interfaces.index;

import p2p.basic.P2P;
import p2p.basic.GUID;
import p2p.index.IDBDataTypeHandler;
import p2p.index.IndexEntry;
import p2p.index.Query;
import p2p.index.Index;
import p2p.index.IndexFactory;
import p2p.index.Type;
import p2p.index.TypeHandler;
import p2p.index.events.NoSuchTypeException;
import p2p.index.events.NoSuchTypeHandlerException;
import pgrid.core.DBManager;
import pgrid.core.index.IndexManager;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.PGridHost;
import pgrid.Constants;

import java.util.Hashtable;
import java.util.Collection;

import mgrid.core.Point;

/**
 * Factory that defines the operations that create various
 * objects of the Storage subsystem. It is recommended to instantiate
 * such types only through a concrete implementation of this factory
 * to avoid hard-coding direct references to them.
 * This class provides static methods to find concrete factories
 * using the reflection API to further decouple the subsystem from its
 * client.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class PGridIndexFactory extends IndexFactory {

	/**
	 * The reference to the only instance of this class (Singleton
	 * pattern). This differs from the C++ standard implementation by Gamma
	 * et.al. since Java ensures the order of static initialization at runtime.
	 *
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	protected static final PGridIndexFactory SHARED_INSTANCE = new PGridIndexFactory();

	/**
	 * The hashtable of all registered data type handlers.
	 */
	protected Hashtable mDataTypeHandlers = new Hashtable();

	/**
	 * The hashtable of all created data types.
	 */
	protected Hashtable mDataTypes = new Hashtable();


	/**
	 * The data item manager.
	 */
	private IndexManager mIndexManager = null;

	/**
	 * The local  host.
	 */
	private PGridHost mLocalHost = null;
	

	/**
	 * The constructor must be protected to ensure that only subclasses can
	 * call it and that only one instance can ever get created. A client that
	 * tries to instantiate PGridIndexManager directly will get an error at compile-time.
	 */
	protected PGridIndexFactory() {
	}

	/**
	 * This creates the only instance of this class. This differs from the C++ standard implementation by Gamma et.al.
	 * since Java ensures the order of static initialization at runtime.
	 *
	 * @return the shared instance of this class.
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	public static PGridIndexFactory sharedInstance() {
		return SHARED_INSTANCE;
	}

	/**
	 * Create the concrete Storage implementation.
	 *
	 * @return the Storage implementation
	 */
	public Index createIndex(P2P p2p) {
		// create P2P layer
		PGridIndex storage = PGridIndex.sharedInstance();
		storage.init(p2p);

		mIndexManager = PGridP2P.sharedInstance().getIndexManager();

		// set the local host
		mLocalHost = (PGridHost)p2p.getLocalPeer();
		
		return storage;
	}

	/**
	 * Create a Query instance compatible with the Storage implementation.
	 *
	 * @param type        the Type of items the query is for
	 * @param query the object that defines the query
	 * @return a Query instance
	 */
	public Query createQuery(Type type, String query) throws NoSuchTypeException {
		if ((type == null) || (query == null))
			throw new NullPointerException();

		return mIndexManager.createQuery(type, mLocalHost, query);
	}

	/**
	 * Create a Query instance compatible with the Index implementation.
	 *
	 * @param type		the Type of items the query is for
	 * @param queryString the string object that defines the query
	 * @param references  To which other queries this query will refer. If no linked queries are needed,
	 *                    use <code>createQuery(Type type, String queryString)</code> instead or leave <code>references</code> null
	 * @return a Query instance
	 * @throws p2p.index.events.NoSuchTypeException
	 *          if the provided Type is unknown.
	 */
	public Query createQuery(Type type, String queryString, Collection<GUID> references) throws NoSuchTypeException {
		if ((type == null) || (queryString == null))
			throw new NullPointerException();

		return mIndexManager.createQuery(type, references, mLocalHost, queryString);
	}

	/**
	 * Create a Query instance compatible with the Storage implementation.
	 *
	 * @param type        	the Type of items the query is for
	 * @param lowerBound	the string object that defines the lower bound of the query
	 * @param higherBound	the string object that defines the higher bound of the query
	 * @return a Query instance
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public Query createQuery(Type type, String lowerBound, String higherBound) {
		if ((type == null) || (lowerBound == null) || (higherBound == null))
			throw new NullPointerException();

		return mIndexManager.createQuery(type, mLocalHost, lowerBound, higherBound);
	}
	
	/**
	 * Create a Query instance compatible with the Storage implementation which sends the original query co-ordinates.
	 *
	 * @param type        	the Type of items the query is for
	 * @param lowerBound	the string object that defines the lower bound of the query
	 * @param higherBound	the string object that defines the higher bound of the query
	 * @return a Query instance
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public Query createQuery(Type type, String lowerBound, String higherBound, Long origxMin, Long origxMax, Long origyMin, Long origyMax) {
		if ((type == null) || (lowerBound == null) || (higherBound == null))
			throw new NullPointerException();
		
		return mIndexManager.createQuery(type, mLocalHost, lowerBound, higherBound, origxMin, origxMax, origyMin, origyMax);
	}
	/**
	 * Create a Query instance compatible with the Storage implementation.
	 *
	 * @param type        	the Type of items the query is for
	 * @param lowerBound	the string object that defines the lower bound of the query
	 * @param higherBound	the string object that defines the higher bound of the query
	 * @param originalMin the string object that defines the original lower bound of the query
	 * @param originalMax the string object that defines the original higher bound of the query
	 * @return a Query instance
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public Query createQuery(Type type, String lowerBound, String higherBound, String originalMin, String originalMax) {
		if ((type == null) || (lowerBound == null) || (higherBound == null) || (originalMin == null ) || (originalMax==null))
			throw new NullPointerException();

		return mIndexManager.createQuery(type, mLocalHost, lowerBound, higherBound);
	}

	/**
	 * Create a Query instance compatible with the Storage implementation.
	 *
	 * @param type		the Type of items the query is for
	 * @param lowerBound  the string object that defines the lower bound of the query
	 * @param higherBound the string object that defines the higher bound of the query
	 * @param references  To which other queries this query will refer. If no linked queries are needed,
	 *                    use <code>createQuery(Type type, String lowerBound, String higherBound)</code> instead or
	 * 						leave <code>references</code> null
	 * @return a Query instance
	 * @throws p2p.index.events.NoSuchTypeException
	 *          if the provided Type is unknown.
	 */
	public Query createQuery(Type type, String lowerBound, String higherBound, Collection<GUID> references) throws NoSuchTypeException {
		if ((type == null) || (lowerBound == null) || (higherBound == null))
			throw new NullPointerException();

		return mIndexManager.createQuery(type, references, mLocalHost, lowerBound, higherBound);
	}

	/**
	 * Create a IndexEntry instance compatible with the Storage implementation.
	 *
	 * @param type the data item's type
	 * @param data the encapsulated data
	 * @return a IndexEntry instance
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public IndexEntry createIndexEntry(Type type, Object data) throws NoSuchTypeException {
		if ((type == null) || (data == null))
			throw new NullPointerException();

		return mIndexManager.createIndexEntry(type, data);
	}

	/**
	 * Registers a Type Handler instance for a given Type.
	 *
	 * @param type an application-specific type to encapsulate
	 * @param handler an application-specific type handler to encapsulate
	 * @throws NoSuchTypeException if the provided Type is unknown.
	 */
	public void registerTypeHandler(Type type, TypeHandler handler) throws NoSuchTypeException {
		if ((type == null) || (handler == null))
			throw new NullPointerException();

		checkType(type);
		mDataTypeHandlers.put(type.toString(), handler);
		
		if(handler instanceof IDBDataTypeHandler){
			DBManager.sharedInstance().registerDBTypeHandler((pgrid.Type)type, (IDBDataTypeHandler)handler);
		}
	}

	/**
	 * Checks whether a given types exists already in PGridP2P.
	 * @param type the type to check.
	 */
	private void checkType(Type type) {
		Type t = (pgrid.Type)mDataTypes.get(type.toString());
		if (t == null) {
			Constants.LOGGER.warning("Type \""+type+"\" is unknown. It will be registered.");
			mDataTypes.put(type.toString(), t);
		}
	}

	/**
	 * Checks whether a type handler is already registered for a given type.
	 * @param type the type to check.
	 */
	public TypeHandler getTypeHandler(Type type) {
		TypeHandler h = (TypeHandler)mDataTypeHandlers.get(type.toString());
		if (h == null) {
			Constants.LOGGER.warning("Type \""+type+"\" has no registered type handler. A default type hander will be registrated for it.");
			h = new DefaultTypeHandler(type);
			registerTypeHandler(type, h);
		}

		return h;
	}


	/**
	 * Creates a Type instance compatible with the Storage implementation.
	 *
	 * @param type an application-specific type to encapsulate
	 * @return a Type instance
	 */
	public pgrid.Type createType(String type) {
		if (type == null)
			throw new NullPointerException();

		pgrid.Type t = (pgrid.Type)mDataTypes.get(type.toString());
		if (t == null) {
			t = new pgrid.Type(type);
			mDataTypes.put(type.toString(), t);
		}
		return t;
	}

	/**
	 * Returns the type of an entry represented by a string.
	 *
	 * @param type the string represeting a type (case-sensitiv).
	 * @return the type or null if no type has been registered for this string
	 */
	public Type getTypeByString(String type) {
		Type t = (Type)mDataTypes.get(type);

		if (t == null) {
		//	Constants.LOGGER.info("Type \""+type+"\" is unknown. It will be registered with a default type handler.");
			t = createType(type);
			registerTypeHandler(t, new DefaultTypeHandler(t));
		}

		return t;
	}
}
