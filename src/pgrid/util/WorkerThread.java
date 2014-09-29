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

/**
*  Abstract class providing basic framework for long-running tasks.
*
 * @version 1.0 2003/03/14
*/
public abstract class WorkerThread implements Runnable {

	/**
	 * The default timeout.
	 */
	private final long DEFAULT_TIMEOUT = 1000;

	/**
	 * The internal lock Object.
	 */
	private Object lock = new Object();

	/**
	 * The running flag.
	 */ 
	private boolean isRunning = true;

	/**
	 * The timeout.
	 */
	private long timeout = DEFAULT_TIMEOUT;

	/**
	 * Creates a new worker thread.
	 */
	public WorkerThread() {
	}

	/**
	 * Notifies the internal lock Object.
	 */
	public void broadcast() {
		synchronized (getLock()) {
			getLock().notifyAll();
		}
	}

	/**
	 * Checks if the task can proceed.
	 *
	 * @return <tt>true</tt> if this task can proceed, for example after been waiting on the internal lock.
	 */
	protected abstract boolean isCondition();

	/**
	 * Stops this worker: change isRunning() to <tt>false</tt> and notifies the internal lock.
	 */
	public void halt() {
		isRunning = false;
		broadcast();
	}

	/**
	 * Handles an occured exception.
	 *
	 * @param t error to be handled
	 */
	protected abstract void handleError(Throwable t);

	/**
	 * Returns the internal lock Object.
	 *
	 * @return the internal lock Object.
	 */
	protected Object getLock() {
		return lock;
	}

	/**
	 * Called just after run() method starts
	 */
	protected abstract void prepareWorker() throws Exception;

	/**
	 * Called just before run() method stops
	 */
	protected abstract void releaseWorker() throws Exception;

	/**
	 * Concreate implementation of Runnable interface
	 */
	public void run() {
		try {
			prepareWorker();
			while (isRunning()) {
				try {
					while (true) {
						if (!isRunning()) return;
						synchronized (getLock()) {
							if (isCondition()) break;
							getLock().wait(getTimeout());
						}
					}
					work();
				} catch (InterruptedException ie) {
					handleError(ie);
					halt();
				} catch (Throwable t) {
					handleError(t);
				}
			}
		} catch (Exception we) {
			handleError(we);
		} finally {
			try {
				releaseWorker();
			} catch (Exception we) {
				handleError(we);
			}
		}
	}

	/**
	 * Checks if the thread is running.
	 *
	 * @return <tt>true</tt> iff this worker is running.
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * Returns the timeout for wait() operations.
	 *
	 * @return timeout for <tt>wait</tt> operation on internal lock Object.
	 */
	public long getTimeout() {
		return timeout;
	}

	/**
	 * Sets the timout for wait() operations.
	 *
	 * @param timeout the timeout for for <tt>wait</tt> operation on internal lock Object.
	 */
	public void setTimeout(long timeout) {
		if (timeout < 0)
			throw new IllegalArgumentException("Wrong timeout: " + timeout);
		this.timeout = timeout;
	}

	/**
	 * Does a task's job; can wait on the internal lock Object or any other
	 * Objects during execution
	 *
	 * @throws Exception: including InterruptedException if the task was
	 *                    interrupted by thread's interrupt()
	 */
	protected abstract void work() throws Exception;

}
