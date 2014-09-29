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

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * This class provides functions to create objects on runtime and invoke its methods on runtime.
 * <b>Example:</b>
 * <pre>
 * Class partypes[] = new Class[1];
 * partypes[0] = String.class;
 * Object args[] = new Object[1];
 * args[0] = "Hello";
 * String retval = (String)Reflector.invokeShared("test.helloWorld", "echo", partypes, args);
 * System.out.println(retval);
 * </pre>
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0 2005/06/01
 */
public class Reflector {

	/**
	 * The method name to get a shared instance.
	 */
	private static final String SHARED_INSTANCE_METHOD = "sharedInstance";

	/**
	 * Creates a new <code>Reflector</code>.
	 */
	protected Reflector() {
		// do nothing
	}

	/**
	 * Invokes a method of a new instance.
	 *
	 * @param name the class name.
	 * @param method the method to invoke.
	 * @param partypes the parameter types.
	 * @param args the arguments.
	 * @return the return value of the invoked method.
	 * @throws ClassNotFoundException if the class was not found.
	 */
	public static Object invokeNew(String name, String method, Class[] partypes, Object[] args) throws ClassNotFoundException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
		Class cls = Class.forName(name);
		Object obj = cls.newInstance();
		Method meth = cls.getMethod(method, partypes);
		return meth.invoke(obj, args);
	}

	/**
	 * Invokes a method of a shared instance.
	 *
	 * @param name the class name.
	 * @param method the method to invoke.
	 * @param partypes the parameter types.
	 * @param args the arguments.
	 * @return the return value of the invoked method.
	 * @throws ClassNotFoundException if the class was not found.
	 */
	public static Object invokeShared(String name, String method, Class[] partypes, Object[] args) throws ClassNotFoundException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		return invokeShared(name, SHARED_INSTANCE_METHOD, method, partypes, args);
	}

	/**
	 * Invokes a method of a shared instance.
	 *
	 * @param name the class name.
	 * @param shared the method to retrieve the shared instance.
	 * @param method the method to invoke.
	 * @param partypes the parameter types.
	 * @param args the arguments.
	 * @return the return value of the invoked method.
	 * @throws ClassNotFoundException if the class was not found.
	 */
	public static Object invokeShared(String name, String shared, String method, Class[] partypes, Object[] args) throws ClassNotFoundException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		Class cls = Class.forName(name);
		Method con = cls.getMethod(shared, null);
		Object obj = con.invoke(cls, null);
		Method meth = cls.getMethod(method, partypes);
		return meth.invoke(obj, args);
	}

	/**
	 * Returns a new instance of a class.
	 *
	 * @param name the class name.
	 * @return the new instance of the class.
	 * @throws ClassNotFoundException if the class was not found.
	 */
	public static Object newInstance(String name) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		Class cls = Class.forName(name);
		return cls.newInstance();
	}

	/**
	 * Returns the shared instance of a class.
	 *
	 * @param name the class name.
	 * @return the shared instance of the class.
	 * @throws ClassNotFoundException if the class was not found.
	 */
	public static Object sharedInstance(String name) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		return sharedInstance(name, SHARED_INSTANCE_METHOD);
	}

	/**
	 * Returns the shared instance of a class.
	 *
	 * @param name the class name.
	 * @param method the method to retrieve the shared instance.
	 * @return the shared instance of the class.
	 * @throws ClassNotFoundException if the class was not found.
	 */
	public static Object sharedInstance(String name, String method) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Class cls = Class.forName(name);
		Method con = cls.getMethod(method, null);
		return con.invoke(cls, null);
	}

}