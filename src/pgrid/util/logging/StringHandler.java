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
package pgrid.util.logging;

import java.util.Iterator;
import java.util.Vector;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * The string handler sends log messages to {@link pgrid.util.logging.StringHandler}
 * for further usage.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public class StringHandler extends Handler {

	/**
	 * The list of handlers.
	 */
	private Vector mHandlers = new Vector();

	/**
	 * If the header was already published.
	 */
	private boolean mHeaderFlag = false;

	/**
	 * Creates a new string handler.
	 *
	 * @param handler a handler for new log messages.
	 */
	public StringHandler(StringMessageHandler handler) {
		mHandlers.add(handler);
	}

	/**
	 * Adds the given message handler.
	 *
	 * @param handler the new message handler.
	 */
	public void addHandler(StringMessageHandler handler) {
		mHandlers.add(handler);
	}

	/**
	 * Close the <tt>Handler</tt> and free all associated resources.
	 * <p/>
	 * The close method will perform a <tt>flush</tt> and then close the
	 * <tt>Handler</tt>.   After close has been called this <tt>Handler</tt>
	 * should no longer be used.  Method calls may either be silently
	 * ignored or may throw runtime exceptions.
	 *
	 * @throws SecurityException if a security manager exists and if
	 *                           the caller does not have <tt>LoggingPermission("control")</tt>.
	 */
	public void close() throws SecurityException {
		// nothing to do
	}

	/**
	 * Flush any buffered output.
	 */
	public void flush() {
		// nothing to do
	}

	/**
	 * Publish a <tt>LogRecord</tt>.
	 * <p/>
	 * The logging request was made initially to a <tt>Logger</tt> object,
	 * which initialized the <tt>LogRecord</tt> and forwarded it here.
	 * <p/>
	 * The <tt>Handler</tt>  is responsible for formatting the message, when and
	 * if necessary.  The formatting should include localization.
	 *
	 * @param record description of the log event
	 */
	public void publish(LogRecord record) {
		if (!isLoggable(record)) {
			return;
		}
		String msg;
		try {
			msg = getFormatter().format(record);
		} catch (Exception ex) {
			// We don't want to throw an exception here, but we
			// report the exception to any registered ErrorManager.
			reportError(null, ex, ErrorManager.FORMAT_FAILURE);
			return;
		}

		try {
			if (!mHeaderFlag) {
				for (Iterator it = mHandlers.iterator(); it.hasNext();) {
					((StringMessageHandler)it.next()).newMessage(getFormatter().getHead(this));
				}
				mHeaderFlag = true;
			}
			for (Iterator it = mHandlers.iterator(); it.hasNext();) {
				((StringMessageHandler)it.next()).newMessage(msg);
			}
		} catch (Exception ex) {
			// We don't want to throw an exception here, but we
			// report the exception to any registered ErrorManager.
			reportError(null, ex, ErrorManager.WRITE_FAILURE);
		}
	}

	/**
	 * Removes the given message handler.
	 *
	 * @param handler the message handler to remove.
	 */
	public void removeHandler(StringMessageHandler handler) {
		mHandlers.remove(handler);
	}

}
