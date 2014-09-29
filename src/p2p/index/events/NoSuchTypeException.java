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

package p2p.index.events;

/**
 * The NoSuchTypeException is thrown when a provided type is not know to P-Grid.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class NoSuchTypeException extends RuntimeException {

	/**
	 * Constructs a NoSuchTypeException without any detail message.
	 */
	public NoSuchTypeException() {
		super();
	}

	/**
	 * Constructs a NoSuchTypeException with the specified detail message.
	 *
	 * @param errorMessage string that should be shown in the exception's output.
	 */
	public NoSuchTypeException(String errorMessage) {
		super(errorMessage);
	}

}