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

/**
 * Utility class to suspend threads.
 *
 * @author A. Nevski
 */
public class WaitingArea {

	/**
	 * Suspends current thread indefinitely or until jvm shutdown,
	 * for example following a Ctrl-C signals.
	 */
	public static void waitTillSignal(int time) {
		Object sync = new Object();
		synchronized (sync) {
			try {
				if (time < 0)
					sync.wait();
				else
					sync.wait(time);
			} catch (InterruptedException exc) {
			}
		}
	}
}
