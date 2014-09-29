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

import pgrid.util.TimerListener;

import java.util.*;

/**
 * This class is a timer manager. It use a single thread to manager all timers
 *
 * @author <a href="mailto:Renault John <renault.john@epfl.ch>">Renault John</a>
 * @version 1.0.0
 */

public class TimerManager {
	/**
	 * Lock object
	 */
	private final Object mLock = new Object();

	/**
	 * A sorted list of timer listener
	 */
	private SortedSet mTimerListner;

	/**
	 * Hashtable with id->TimerElement
	 */
	private Hashtable<Object, TimerElement> mIDTimer = new Hashtable<Object, TimerElement>();

	/**
	 * The timer thread
	 */
	private Thread mTimerThread;

	/**
	 * The reference to the only instance of this class (Singleton
	 * pattern). This differs from the C++ standard implementation by Gamma
	 * et.al. since Java ensures the order of static initialization at runtime.
	 *
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	private static final TimerManager SHARED_INSTANCE = new TimerManager();

	protected TimerManager() {
		mTimerListner = Collections.synchronizedSortedSet(new TreeSet(new Comparator() {
			public int compare(Object o1, Object o2) {
				return (int)(((TimerElement)o1).mNextTimeout - ((TimerElement)o2).mNextTimeout);
			}
		}));

		mTimerThread = new Thread(new TimerManager.Timer(), "Timer thread");
		mTimerThread.setDaemon(true);
		mTimerThread.setPriority(Thread.NORM_PRIORITY-1);
		mTimerThread.start();
	}

	/**
	 * This creates the only instance of this class. This differs from the C++ standard implementation by Gamma et.al.
	 * since Java ensures the order of static initialization at runtime.
	 *
	 * @return the shared instance of this class.
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	public static TimerManager sharedInstance() {
		return SHARED_INSTANCE;
	}

	/**
	 * Register a timer. As soons as the the time out occurs, the listener
	 * will be informed. The object id will be given in parameter.
	 *
	 * @param timeout 	before the listener will be informed in milliseconds
	 * @param id		User dependent ID. It could be null
	 * @param listener  to inform
	 * @param periodic	if true the timer will be reconducted automatically
	 */
	public void register(long timeout, Object id, TimerListener listener, boolean periodic) {
		TimerElement te = new TimerElement(timeout, id, listener, periodic);
		synchronized(mLock) {
			//Add the new timer element
			mTimerListner.add(te);
			if (id!=null) mIDTimer.put(id, te);
			if (mTimerListner.first().equals(te)) {
				// The new TE should be the next triggered element
				mLock.notifyAll();
			}
		}

	}

	/**
	 * Reset the timer for a specific timer.
	 * @param id
	 */
	public void reset(Object id) {
		synchronized(mLock) {
			TimerElement te = mIDTimer.get(id);
			if (te != null) {
				mTimerListner.remove(te);
				te.mNextTimeout = te.mTimeout + System.currentTimeMillis();
				mTimerListner.add(te);
			}

		}
	}

	/**
	 * Remove a timer
	 * @param id
	 */
	public void remove(Object id) {
		synchronized(mLock) {
			TimerElement te = mIDTimer.remove(id);
			if (te != null) {
				mTimerListner.remove(te);
			}
		}
	}

	/**
	 * Check if a specific timer is known to the system
	 * @param id
	 * @return true if id is known
	 */
	public boolean contains(Object id) {
		return mIDTimer.contains(id);
	}

	/**
	 * a timer object
	 */
	class TimerElement {
		public long mTimeout;
		public long mNextTimeout;
		public Object mID;
		public pgrid.util.TimerListener mListener;
		public boolean mPeriodic;

		public TimerElement(long timout, Object id, TimerListener listener, boolean periodic) {
			mTimeout = timout;
			mNextTimeout = mTimeout + System.currentTimeMillis();
			mID = id;
			mListener = listener;
			mPeriodic = periodic;
		}
	}

	/**
	 * the timer thread
	 */
	class Timer implements Runnable {

		/**
		 * When an object implementing interface <code>Runnable</code> is used
		 * to create a thread, starting the thread causes the object's
		 * <code>run</code> method to be called in that separately executing
		 * thread.
		 * <p/>
		 * The general contract of the method <code>run</code> is that it may
		 * take any action whatsoever.
		 *
		 * @see Thread#run()
		 */
		public void run() {
			Vector<TimerElement> toCall = new Vector<TimerElement>();

			long currentTime;
			long tmp;
			TimerElement te;

			while (true) {
				toCall.clear();
				try {
					synchronized(mLock) {
						if (mTimerListner.isEmpty()) mLock.wait();
						else {
							tmp = ((TimerElement)mTimerListner.first()).mNextTimeout-System.currentTimeMillis();
							if (tmp > 0)
								mLock.wait(tmp);
						}

						// trigger function that should be triggered
						currentTime = System.currentTimeMillis();
						while (!mTimerListner.isEmpty() && (te = ((TimerElement)mTimerListner.first())).mNextTimeout <= currentTime) {
							mTimerListner.remove(te);
							if (te.mID != null && !te.mPeriodic) mIDTimer.remove(te.mID);

							toCall.add(te);

							if (te.mPeriodic) {
								te.mNextTimeout = currentTime+te.mTimeout;
								mTimerListner.add(te);
							}
						}
					}

				} catch (InterruptedException e) {
					System.out.println("Timer thread released.");
					break;
				}

				// call all needed listener
				for (TimerElement l: toCall) {
					l.mListener.timerTriggered(l.mID);
				}
			}

		}
	}

	/**
	 * Shutdown the timer manager
	 */
	public void shutdown() {
		if (mTimerThread != null)
			mTimerThread.interrupt();
	}


}
