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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Formats a {@link java.util.logging.LogRecord} into a plain text format, defined by a format pattern.
 * <p/>
 * The format patterns can include all defined pattern place holders and any characters.<p>
 * <b>Example:</b>
 * <pre>
 * Logger logger = Logger.getLogger("Example");
 * LogFormatter formatter = new LogFormatter();
 * StreamHandler handler = new StreamHandler(System.err, formatter);
 * handler.setLevel(Level.WARNING);
 * logger.addHandler(handler);
 * </pre>
 * This example uses the the default values of <code>LogFormatter</code> to publish only <code>Level.WARNING</code> and
 * <code>Level.SEVERE</code> <code>LogRecord</code>s at <code>System.err</code>.
 * A log line would look like this:
 * <pre>
 * Feb 28 16:51:57 0 Example Warning: This is a warning!
 * Feb 28 16:52:43 1 Example Warning: This is another warning!
 * Feb 28 16:55:16 2 Example Error: This is an error!
 * </pre>
 * When setting a different format pattern and date format
 * <pre>
 * formatter.setDateFormat("HH:mm:ss");
 * formatter.setFormatPattern(LogFormatter.DATE + " " + LogFormatter.LEVEL + ": " + MESSAGE + System.getProperty("line.separator"));
 * </pre>
 * the log lines would look like this:
 * <pre>
 * 16:51:57 Warning: This is a warning!
 * 16:52:43 Warning: This is another warning!
 * 16:55:16 Error: This is an error!
 * </pre>
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0 2003/03/14
 * @see java.util.logging.Formatter
 */
public class LogFormatter extends Formatter {

	/**
	 * The pattern place holder for the log date.<br>
	 * The date can be formatted by the <code>setDateFormat(String)</code> method.
	 */
	public static final String DATE = "$DATE$";

	/**
	 * The pattern place holder for the LogRecord level.
	 */
	public static final String LEVEL = "$LEVEL$";

	/**
	 * The pattern place holder for the LogRecord Logger name.
	 */
	public static final String LOGGER_NAME = "$LOGGER_NAME$";

	/**
	 * The pattern place holder for the LogRecord message.
	 */
	public static final String MESSAGE = "$MESSAGE$";

	/**
	 * The pattern place holder for the log date.<br>
	 * The date can be formatted by the <code>setDateFormat(String)</code> method.
	 */
	public static final String MILLISECONDS = "$MILLIS$";

	/**
	 * The pattern place holder for new line.
	 */
	public static final String NEW_LINE = System.getProperty("line.separator");

	/**
	 * The pattern place holder for the LogRecord sequence number.
	 */
	public static final String SEQUENCE_NUMBER = "$SEQUENCE_NUMBER$";

	/**
	 * The pattern place holder for the LogRecord source class.
	 */
	public static final String SOURCE_CLASS = "$SOURCE_CLASS$";

	/**
	 * The pattern place holder for the originating source file.
	 */
	public static final String SOURCE_FILE = "$SOURCE_FILE$";

	/**
	 * The pattern place holder for the originating line in the source file.
	 */
	public static final String SOURCE_LINE = "$SOURCE_LINE$";

	/**
	 * The pattern place holder for the source method.
	 */
	public static final String SOURCE_METHOD = "$SOURCE_METHOD$";

	/**
	 * The pattern place holder for the originating thread id.
	 */
	public static final String THREAD_ID = "$THREAD_ID$";

	/**
	 * The pattern place holder for the Throwable.
	 */
	public static final String THROWABLE = "$THROWABLE$";

	/**
	 * The Resource key for the unknown Logger name.
	 */
	private static final String UNKNOWN = "UNKNOWN";

	/**
	 * The pattern used to format a LogRecord publishing all informations.<br>
	 * DATE SEQUENCE_NUMBER LOGGER_NAME LEVEL: MESSAGE [SOURCE_CLASS.SOURCE_METHOD@Thread:THREAD_ID (SOURCE_FILE:SOURCE_LINE)]<br>
	 * THROWABLE
	 */
	public static final String ALL_PATTERN = DATE + " " + SEQUENCE_NUMBER + " " + LOGGER_NAME + " " + LEVEL + ": " + MESSAGE + " [" + SOURCE_CLASS + "." + SOURCE_METHOD + "@Thread:" + THREAD_ID + " (" + SOURCE_FILE + ":" + SOURCE_LINE + ")]" + NEW_LINE + THROWABLE;

	/**
	 * The debug pattern used to format a LogRecord.<br>
	 * DATE LOGGER_NAME LEVEL: MESSAGE (SOURCE_FILE:SOURCE_LINE)<br>
	 * THROWABLE
	 */
	public static final String DEBUG_PATTERN = DATE + " " + LOGGER_NAME + " " + LEVEL + ": " + MESSAGE + " (" + SOURCE_FILE + ":" + SOURCE_LINE + ")" + NEW_LINE + THROWABLE;

	/**
	 * The default pattern used to format a LogRecord.<br>
	 * DATE SEQUENCE_NUMBER LOGGER_NAME LEVEL: MESSAGE<br>
	 * THROWABLE
	 */
	public static final String DEFAULT_PATTERN = DATE + " " + LEVEL + ": " + MESSAGE + NEW_LINE + THROWABLE;

	/**
	 * The default date format (MMM dd HH:mm:ss).<br>
	 *
	 * @see java.text.SimpleDateFormat
	 */
	public static final String DEFAULT_DATE_FORMAT = "MMM dd HH:mm:ss";

	/**
	 * The used pattern to format a LogRecord.
	 */
	private String mFormatPattern = DEFAULT_PATTERN;

	/**
	 * The string used as head.
	 */
	private String mHeadString = null;

	/**
	 * The Resource used by this Formatter.
	 */
	private ResourceBundle mResource = null;

	/**
	 * The simple date formatter used to format the time.
	 */
	private SimpleDateFormat mSimpleDateFormatter = new SimpleDateFormat(DEFAULT_DATE_FORMAT);

	/**
	 * The string used as tail.
	 */
	private String mTailString = null;

	/**
	 * Creates a new LogFormatter with default values (Resources, date format, format pattern).
	 */
	public LogFormatter() {
		this(ResourceBundle.getBundle("pgrid.util.logging.LogResources"), null, null);
	}

	/**
	 * Creates a new LogFormatter with default values (Resources, date format) and the given format pattern.
	 *
	 * @param format the format pattern.
	 */
	public LogFormatter(String format) {
		this(ResourceBundle.getBundle("pgrid.util.logging.LogResources"), null, null);
		mFormatPattern = format;
	}

	/**
	 * Creates a new LogFormatter with the given values.
	 *
	 * @param resource the ResourceBundle.
	 * @param head     a string used as log head.
	 * @param tail     a string used as log tail.
	 */
	public LogFormatter(ResourceBundle resource, String head, String tail) {
		mHeadString = head;
		mTailString = tail;
		if (resource != null)
			mResource = resource;
		else
			mResource = ResourceBundle.getBundle("pgrid.util.logging.LogResources");
	}

	/**
	 * Format the given log record and return the formatted string.
	 * The resulting formatted String will normally include a localized and formated version of the LogRecord's message
	 * field. The Formatter.formatMessage convenience method can (optionally) be used to localize and format the message
	 * field.
	 *
	 * @param record the log record to be formatted.
	 * @return a formatted log record.
	 */
	public String format(LogRecord record) {
		StringBuffer sb = new StringBuffer(500);
		String sourceFile = null;
		int sourceLine = -1;
		// start at the first character of the format pattern
		int pos = 0;
		while (pos < mFormatPattern.length()) {
			// find first $ beginning at the current position
			int ch = mFormatPattern.indexOf("$", pos);
			// no $ found => append the rest of the pattern and break
			if (ch < 0) {
				sb.append(mFormatPattern.substring(pos));
				break;
			}
			// a $ was found => try to find the next
			int nextCh = mFormatPattern.indexOf("$", ch + 1);
			// no further $ found => append the rest of the pattern and break
			if (nextCh < 0) {
				sb.append(mFormatPattern.substring(pos));
				break;
			}
			// a further $ found => append everything before the first $
			sb.append(mFormatPattern.substring(pos, ch));
			pos = ch;
			// a further $ found => check if it is one of the place holders
			String chStr = mFormatPattern.substring(ch, nextCh + 1);
			if (chStr.equals(DATE)) {
				sb.append(mSimpleDateFormatter.format(new Date(record.getMillis())));
				pos = nextCh + 1;
			} else if (chStr.equals(LEVEL)) {
				sb.append(mResource.getString(record.getLevel().getName()));
				pos = nextCh + 1;
			} else if (chStr.equals(LOGGER_NAME)) {
				sb.append((record.getLoggerName() != null ? record.getLoggerName() : mResource.getString(UNKNOWN)));
				pos = nextCh + 1;
			} else if (chStr.equals(MESSAGE)) {
				if (record.getMessage() != null)
					sb.append(formatMessage(record));
				pos = nextCh + 1;
			} else if (chStr.equals(MILLISECONDS)) {
				if (record.getMessage() != null)
					sb.append(record.getMillis());
				pos = nextCh + 1;
			} else if (chStr.equals(SEQUENCE_NUMBER)) {
				sb.append(record.getSequenceNumber());
				pos = nextCh + 1;
			} else if (chStr.equals(SOURCE_CLASS)) {
				if (record.getSourceClassName() != null)
					sb.append(record.getSourceClassName());
				pos = nextCh + 1;
			} else if (chStr.equals(SOURCE_FILE)) {
				if (sourceFile == null) {
					try {
						StackTraceElement trace[] = new Throwable().getStackTrace();
						for (int i = 0; i < trace.length; i++) {
							if (record.getSourceClassName().equals(trace[i].getClassName()))
								if (record.getSourceMethodName().equals(trace[i].getMethodName())) {
									sourceFile = trace[i].getFileName();
									sourceLine = trace[i].getLineNumber();
								}
						}
					} catch (Exception e) {
						// do nothing
					}
				}
				sb.append(sourceFile);
				pos = nextCh + 1;
			} else if (chStr.equals(SOURCE_LINE)) {
				if (sourceLine < 0) {
					try {
						StackTraceElement trace[] = new Throwable().getStackTrace();
						for (int i = 0; i < trace.length; i++) {
							if (record.getSourceClassName().equals(trace[i].getClassName()))
								if (record.getSourceMethodName().equals(trace[i].getMethodName())) {
									sourceFile = trace[i].getFileName();
									sourceLine = trace[i].getLineNumber();
								}
						}
					} catch (Exception e) {
						// do nothing
					}
				}
				sb.append(sourceLine);
				pos = nextCh + 1;
			} else if (chStr.equals(SOURCE_METHOD)) {
				if (record.getSourceMethodName() != null)
					sb.append(record.getSourceMethodName());
				pos = nextCh + 1;
			} else if (chStr.equals(THREAD_ID)) {
				sb.append(record.getThreadID());
				pos = nextCh + 1;
			} else if (chStr.equals(THROWABLE)) {
				if (record.getThrown() != null) {
					try {
						StringWriter sw = new StringWriter();
						PrintWriter pw = new PrintWriter(sw);
						record.getThrown().printStackTrace(pw);
						pw.close();
						sb.append(sw.toString());
					} catch (Exception e) {
						// do nothing
					}
				}
				pos = nextCh + 1;
			} else {
				sb.append(mFormatPattern.substring(pos, nextCh));
				pos = nextCh;
			}
		}
		return sb.toString();
	}

	/**
	 * Return the header string for a set of formatted records.
	 *
	 * @param h the target handler.
	 * @return header string,
	 */
	public String getHead(Handler h) {
		if (mHeadString != null)
			return mSimpleDateFormatter.format(new Date()) + " " + mHeadString + NEW_LINE;
		else
			return "";
	}

	/**
	 * Return the tail string for a set of formatted records.
	 *
	 * @param h the target handler.
	 * @return tail string.
	 */
	public String getTail(Handler h) {
		if (mTailString != null)
			return mSimpleDateFormatter.format(new Date()) + " " + mTailString + NEW_LINE;
		else
			return "";
	}

	/**
	 * Sets the date format.
	 *
	 * @param dateFormat the date format.
	 * @throws java.lang.IllegalArgumentException
	 *          if an illegal date format is given.
	 * @see java.text.SimpleDateFormat
	 */
	synchronized public void setDateFormat(String dateFormat) throws IllegalArgumentException {
		mSimpleDateFormatter = new SimpleDateFormat(dateFormat);
	}

	/**
	 * Sets the format pattern used to publish a LogRecord.
	 *
	 * @param format the format pattern.
	 * @throws java.lang.IllegalArgumentException
	 *          if an illegal format pattern is given.
	 */
	synchronized public void setFormatPattern(String format) throws IllegalArgumentException {
		mFormatPattern = format;
	}

}