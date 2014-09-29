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

package p2p.index;

import mgrid.core.Point;
import p2p.basic.GUID;
import p2p.basic.Key;
import p2p.basic.Peer;

/**
 * Defines the operations that all data items stored on the network support.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0.0
 */
public interface IndexEntry extends Comparable {

	/**
	 * Get the key index of the item.
	 *
	 * @return the Key index
	 */
	public Key getKey();

	/**
	 * Get the ID of the item.
	 *
	 * @return the Key index
	 */
	public GUID getGUID();

	/**
	 * Get the item's type of content.
	 *
	 * @return the Type of the item's content
	 */
	public Type getType();

	/**
	 * Get the item's content.
	 *
	 * @return an Object reference to the data
	 */
	public Object getData();

	/**
	 * Set the item's content.
	 *
	 * @param newData the new data
	 */
	public void setData(Object newData);

	/**
	 * Get the host responsible for this item.
	 *
	 * @return a Peer reference to the host
	 */
	public Peer getPeer();
	
	/**
	 * Get the Point object
	 *
	 * @return a Peer reference to the host
	 */
	public Point getPoint();
	
	/**
	 * Set the Point object
	 *
	 * @return a Peer reference to the host
	 */
	public void setPoint(Point point);
	
	/**
	 * Get the Point.ID object
	 *
	 * @return Point.ID
	 */
	public Long getmPointID() ;

	/**
	 * Set the Point.ID object
	 */
	public void setmPointID(Long mPointID) ;
	
	//public Long setHits(Long hits);
	
//	public Long getHits();

}
