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

package p2p.basic;

import p2p.basic.events.P2PListener;

import java.util.Properties;

/**
 * Defines the operations the peer-to-peer layer supports.
 * These include initialization and shutdown requests as
 * well as several ways to use the distributed indexing
 * structure to locate peers and message them.
 */
public interface P2P {

  /**
   * Register an object to be notified of new messages.
   *
   * @param listener the P2PListener implementation to register
   */
  public void addP2PListener(P2PListener listener);

  /**
   * Get information on the local peer.
   *
   * @return the local peer
   */
  public Peer getLocalPeer();

  /**
   * Get information on the neighbors known to this peer.
   *
   * @return an array of neighboring peers
   */
  public Peer[] getNeighbors();

	/**
	 * Initializes the P2P facility with the given properties.
	 *
	 * @param properties further initialization properties.
	 */
	public void init(Properties properties);

  /**
   * Checks if the local peer is responsible for the given key.
   *
   * @param key the key to check.
   * @return <code>true</code> if the local peer is responsible, <code>false</code> otherwise.
   */
  public boolean isLocalPeerResponsible(Key key);

	/**
	 * Initialize the local routing table after connecting to network.
	 *
	 * @param peer a bootstrap peer to contact
	 */
	public void join(Peer peer);

	/**
	 * Prepare for shutdown by announcing departure from network.
	 */
	public void leave();

	/**
	 * Get information on a peer.
	 *
	 * @param key a key that the distant peer is responsible for
	 * @param timeout for the reply to arrive
	 * @return a Peer instance
	 */
	public Peer lookup(Key key, long timeout);

  /**
   * Remove registration of a current listener of new messages.
   *
   * @param listener the P2PListener implementation to unregister
   */
  public void removeP2PListener(P2PListener listener);

	/**
	 * Send a message to a peer. Peer-to-peer layer is responsible
	 * for routing the message across as many peers as necessary.
	 * Message delivry is subject to the existance of a path between
	 * the origin and the destination. Confirmation receipt is not
	 * provided.
	 *
	 * @param key     a key that the distant peer is reponsible for
	 * @param message the message to send
	 */
	public void route(Key key, Message message);

	/**
	 * Send a message to peers based on several keys. In some cases
	 * only one peer will be the destination if all keys are under
	 * its responsiblity.
	 *
	 * @param keys    an array of destination keys
	 * @param message the message to send
	 */
	public void route(Key[] keys, Message[] message);

	/**
	 * Send a message to a set of peers responsible for a range of keys.
	 *
	 * @param range   the range of keys
	 * @param message the message to send
	 */
	public void route(KeyRange range, Message message);

  /**
   * Send a message to peers responsible for the same part of the
   * keyspace as the local node. This operation might not be
   * supported by all implementations.
   *
   * @param message the message to send
   */
  public void routeToReplicas(Message message);

	/**
	 * Send a message directly to a peer (no routing is done)
	 *
	 * @param peer    the peer
	 * @param message the message to send
	 */
	public void send(Peer peer, Message message);

}
