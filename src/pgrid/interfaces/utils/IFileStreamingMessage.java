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
package pgrid.interfaces.utils;
/**
 *
 * This interface should be implemented by P-Grid messages if the message to be communicated 
 * across peers should be handled using file streams. It is advisable to exchange using file
 * streams rather than exchanging bulky xml-messages.
 * 
 * 
 * @author yerva (surenderreddy.yerva AT epfl.ch)
 *
 */
public interface IFileStreamingMessage {
	/**
	 * @return The name of the file which is being sent across the network to the other peer.
	 * 
	 */
	public String getFileName();
	
	/*
	 * @return The location of the file which is being sent across the network to the other peer.
	 */
	public String getFilePath();
	
	/**
	 * 
	 * @return the size of the file which is being sent across the network.
	 */
	public long getFileSize();
	
	/**
	 *  This method is called once the file is downloaded from the other peer.
	 *  FinishUpWork can be done in this method
	 */
	public void notifyEnd();

}
