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

import pgrid.PGridHost;

import java.util.Comparator;

/**
 * This class implement Comparator and compare two path or two
 * hosts.
 *
 * @author Renault JOHN
 */
public class PathComparator implements Comparator {

	public int compare(Object o1, Object o2) {
		String path1 = ((PGridHost)o1).getPath();
		String path2 = ((PGridHost)o2).getPath();

		return compare(path1, path2);
	}

	public int compare(String path1, String path2) {
		if (path1.equals("") && path2.equals(""))
			return 0;
		else if (path1.equals(""))
			return 1;
		else if (path2.equals("")) return -1;

		int length = Math.min(path1.length(), path2.length());

		path1 = path1.substring(0, length);
		path2 = path2.substring(0, length);
		Integer p1 = Integer.valueOf(path1, 2);
		Integer p2 = Integer.valueOf(path2, 2);

		return p1.compareTo(p2);
	}

	public boolean equals(Object obj) {
		return false;
	}
}
