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

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * This Filter can be used to provide fine grain control over what is logged, beyond the control provided by log levels.
 * <p/>
 * The Filter is created with an array of {@link java.util.logging.Level}s to publish. A
 * {@link java.util.logging.Logger} using this Filter will only publish {@link java.util.logging.LogRecord}s with
 * {@link java.util.logging.Level}s included in the delivered array.
 * <p/>
 * The <code>LogLevelFilter</code> could be used to log different levels at different handlers.<p>
 * <b>Example:</b>
 * <pre>
 * Logger logger = Logger.getLogger("Example");
 * logger.setUseParentHandlers(false);
 * StreamHandler errorHandler = new StreamHandler(System.err, new LogFormatter());
 * errorHandler.setLevel(Level.WARNING);
 * StreamHandler infoHandler = new StreamHandler(System.out, new LogFormatter());
 * infoHandler.setLevel(Level.ALL);
 * Level[] levels = new Level[5];
 * levels[0] = Level.FINEST;
 * levels[1] = Level.FINER;
 * levels[2] = Level.FINE;
 * levels[3] = Level.CONFIG;
 * levels[4] = Level.INFO;
 * infoHandler.setFilter(new LogLevelFilter(levels));
 * logger.addHandler(errorHandler);
 * logger.addHandler(infoHandler);
 * </pre>
 * This example publishes <code>Level.WARNING</code> and <code>Level.SEVERE</code> LogRecords at <code>System.err</code> and uses
 * the <code>LogLevelFilter</code> to publish all other levels (excluding <code>Level.WARNING</code> and <code>Level.SEVERE</code>)
 * at <code>System.out</code>.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0 2003/02/28
 * @see java.util.logging.Filter
 */
public class LogLevelFilter implements Filter {

	/**
	 * The AND/OR flag.
	 */
	private boolean mAndFlag = false;

	/**
	 * The log levels to publish.
	 */
	private Level[] mLevels = null;

	/**
	 * The logger.
	 */
	private String[] mLogger = null;

	/**
	 * Creates a new LogLevelFilter with the given LogLevels.
	 *
	 * @param levels  the levels to publish.
	 * @param logger  the loggers to log.
	 * @param andFlag if levels AND loggers or levels OR loggers.
	 */
	public LogLevelFilter(Level[] levels, String[] logger, boolean andFlag) {
		mLevels = levels;
		mLogger = logger;
		mAndFlag = andFlag;
	}

	/**
	 * Check if a given log record should be published.
	 *
	 * @param record a LogRecord.
	 * @return <code>true</code> if the log record should be published.
	 */
	public boolean isLoggable(LogRecord record) {
		boolean loggableLevel = false;
		if (mLevels != null) {
			for (int i = mLevels.length - 1; i >= 0; i--) {
				if (mLevels[i].equals(record.getLevel())) {
					loggableLevel = true;
					break;
				}
			}
		} else {
			loggableLevel = true;
		}
		boolean loggableLogger = false;
		if (mLogger != null) {
			for (int i = mLogger.length - 1; i >= 0; i--) {
				if (mLogger[i].equals(record.getLoggerName())) {
					loggableLogger = true;
					break;
				}
			}
		} else {
			loggableLogger = true;
		}
		if (mAndFlag)
			return loggableLevel & loggableLogger;
		else
			return loggableLevel | loggableLogger;
	}

}