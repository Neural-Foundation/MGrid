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

import java.util.ListResourceBundle;

/**
 * The Resources used by the {@link pgrid.util.logging.LogFormatter}.
 * <p/>
 * The Resources are:
 * <ul>
 * <li><b>SEVERE</b> ... the published string (Error) for {@link java.util.logging.Level}<code>.SEVERE</code>.</li>
 * <li><b>WARNING</b> ... the published string (Warning) for {@link java.util.logging.Level}<code>.WARNING</code>.</li>
 * <li><b>INFO</b> ... the published string (Info) for {@link java.util.logging.Level}<code>.INFO</code>.</li>
 * <li><b>CONFIG</b> ... the published string (Config) for {@link java.util.logging.Level}<code>.CONFIG</code>.</li>
 * <li><b>FINE</b> ... the published string (Trace) for {@link java.util.logging.Level}<code>.FINE</code>.</li>
 * <li><b>FINER</b> ... the published string (Debug) for {@link java.util.logging.Level}<code>.FINER</code>.</li>
 * <li><b>FINEST</b> ... the published string (Finest) for {@link java.util.logging.Level}<code>.FINEST</code>.</li>
 * <li><b>UNKNOWN</b> ... the published string (unknown) for an unknown <code>LoggerName</code>.</li>
 * </ul>
 *
 * @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0 2003/03/14
 * @see java.util.ListResourceBundle
 * @see pgrid.util.logging.LogFormatter
 */
public class LogResources extends ListResourceBundle {

	/**
	 * The association list.
	 */
	static final Object[][] contents = {{"SEVERE", "Error"},
			{"WARNING", "Warning"},
			{"INFO", "Info"},
			{"CONFIG", "Config"},
			{"FINE", "Trace"},
			{"FINER", "Debug"},
			{"FINEST", "Finest"},
			{"UNKNOWN", "unknown"}};

	/**
	 * Get the association list.
	 *
	 * @return the association list.
	 */
	public Object[][] getContents() {
		return contents;
	}

}