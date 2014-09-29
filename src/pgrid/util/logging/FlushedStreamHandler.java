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

import java.io.OutputStream;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * This class extends the {@link java.util.logging.StreamHandler} by flushing the stream after every published
 * {@link java.util.logging.LogRecord}.
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0 2003/03/14
 * @see java.util.logging.StreamHandler
 */
public class FlushedStreamHandler extends StreamHandler {

	/**
	 * Create a </code>FlushedStreamHandler</code>, with no current output stream.
	 */
	public FlushedStreamHandler() {
		super();
	}

	/**
	 * Create a </code>FlushedStreamHandler</code> with a given <code>Formatter</code> and output stream.
	 *
	 * @param out       the target output stream.
	 * @param formatter Formatter to be used to format output.
	 */
	public FlushedStreamHandler(OutputStream out, Formatter formatter) {
		super(out, formatter);
	}

	/**
	 * Format, publish, and flushes a <code>LogRecord</code>.
	 *
	 * @param record description of the log event.
	 * @see java.util.logging.StreamHandler
	 */
	public synchronized void publish(LogRecord record) {
		super.publish(record);
		super.flush();
	}

}
