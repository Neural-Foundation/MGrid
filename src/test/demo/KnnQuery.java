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
package test.demo;

import p2p.basic.*;
import p2p.basic.events.P2PListener;
import p2p.index.events.SearchListener;
import p2p.index.*;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.interfaces.basic.PGridP2PFactory;
import pgrid.interfaces.index.PGridIndexFactory;
import pgrid.interfaces.index.DefaultTypeHandler;
import pgrid.PGridHost;
import pgrid.core.search.SearchManager;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import mgrid.core.MGridUtils;
import mgrid.core.Point;
import mgrid.core.PQSort;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.google.uzaygezen.core.BacktrackingQueryBuilder;
import com.google.uzaygezen.core.BigIntegerContent;
import com.google.uzaygezen.core.BitVector;
import com.google.uzaygezen.core.BitVectorFactories;
import com.google.uzaygezen.core.CompactHilbertCurve;
import com.google.uzaygezen.core.FilteredIndexRange;
import com.google.uzaygezen.core.LongContent;
import com.google.uzaygezen.core.MultiDimensionalSpec;
import com.google.uzaygezen.core.NodeValue;
import com.google.uzaygezen.core.PlainFilterCombiner;
import com.google.uzaygezen.core.Pow2LengthBitSetRange;
import com.google.uzaygezen.core.Query;
import com.google.uzaygezen.core.QueryBuilder;
import com.google.uzaygezen.core.RegionInspector;
import com.google.uzaygezen.core.SimpleRegionInspector;
import com.google.uzaygezen.core.SpaceFillingCurve;
import com.google.uzaygezen.core.ZoomingSpaceVisitorAdapter;
import com.google.uzaygezen.core.ranges.LongRange;
import com.google.uzaygezen.core.ranges.LongRangeHome;

import test.WaitingArea;

/**
 * Sample peer listens for messages and displays them on the command line.
 * 
 * @author A. Nevski and Renault JOHN
 */
public class KnnQuery implements SearchListener, P2PListener {

	private static P2PFactory p2pFactory;
	private static P2P p2p;

	private static IndexFactory indexFactory;
	private static Index index;

	private static Type type;

	private static long startTime = 0L;
	private static long endTime = 0L;

	private static PriorityBlockingQueue<Point> nnQueue;

	// private static List<Point> actual ;

	public static Object lock = new Object();

	private static boolean allResults;
	
	private static Long queryx, queryy;
	
	private static int k ;
	
	public static volatile int requestCount = 0;
	
	public static volatile int responseCount = 0;
	
	public static double delta;
	

	/**
	 * The P2P facility.
	 */
	protected PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	/**
	 * Create an instance of sample receiving peer.
	 * 
	 */
	public KnnQuery() {		
		allResults = false;
	}

	/**
	 * Let the sample peer send a message.
	 * 
	 * @param args
	 *            the command line arguments to be passed on to the peer-to-peer
	 *            layer
	 * @see test.CommandLineArgs usage
	 */
	public static void main(String[] args) throws IOException {
		test.CommandLineArgs cla = new test.CommandLineArgs(args);
 	KnnQuery storage = new KnnQuery();

		Properties properties = cla.getOtherProperties();
		InetAddress bootIP = cla.getAddress();
		int bootPort = cla.getPort();
		// Set the debug mode to the minimum. Debug can be set to a number
		// between 0-3
		properties.setProperty(PGridP2P.PROP_DEBUG_LEVEL, "3");
		// Use a verbose mode
		properties.setProperty(PGridP2P.PROP_VERBOSE_MODE, "false");

		// Get an instance of the P2PFactory
		p2pFactory = PGridP2PFactory.sharedInstance();
		System.out.println("Acquired P-Grid factory reference. ");

		// Get an instance of the P2P object, aka P-Grid
		p2p = p2pFactory.createP2P(properties);
		System.out.println("Created a P2P instance. ");

		// Create an instance of the bootstrap host that will be use to
		// bootstrap the
		// network
		Peer bootstrap = null;
		try {
			bootstrap = p2pFactory.createPeer(bootIP, bootPort);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		System.out.println("Bootstrap Peer instance Created.");

		// Get an instance of the StorageFactory
		indexFactory = PGridIndexFactory.sharedInstance();
		System.out.println("Storage factory reference acquired. ");

		// Get an instance of the Storage object, aka MGridStorage
		index = indexFactory.createIndex(p2p);
		System.out.println("Storage instance acquired. ");

		// creating and registering data type.
		type = indexFactory.createType("SimpleType");
		TypeHandler handler = new DefaultTypeHandler(type);
		indexFactory.registerTypeHandler(type, handler);

		// Try to join the network
		p2p.join(bootstrap);
		System.out.println("Network joined. Current key range (path) is: "
				+ ((PGridHost) p2p.getLocalPeer()).getPath());

		// add this object as a p2p listener
		p2p.addP2PListener(storage.returnThis());

		// wait for the network to be created.
		System.out.println("Waiting while structuring the network.");

		WaitingArea.waitTillSignal(1000 * 2);

		// calculate k-nearest neighbours

		queryx = 66400L;
		queryy = 2000L;

		 k = 1000000;
		double totalPoints = 400000000;
		double D = MGridUtils.nextRange(k, totalPoints);
		delta = Math.ceil((D / k));
		 System.out.println("Creating nearest neighbour query with k=" + k	
				 + " and point (" + queryx + "," + queryy + ")"+" delta "+delta);
		
		
		boolean sizeCheck = false;
	
		 while (sizeCheck == false ) {
			 PQSort sort = new PQSort(queryx, queryy);
				nnQueue = new PriorityBlockingQueue<Point>(k, sort);
		startTime = System.currentTimeMillis();
		storage.hilbertknnQuery(storage.returnThis(), queryx, queryy, delta);

		Runnable runA = new Runnable() {
			@Override
			public void run() {
				synchronized (lock) {			
			//		System.out.println("starting "+Thread.currentThread().getName());	
					while (responseCount < requestCount) {
							allResults = false;
						}
						allResults = true;
		//		System.out.println("Notifying main thread");
						lock.notifyAll();
			
			} //  end of sync block
			}
		};
		
		Thread threadA = new Thread(runA, "HelperThread");
		threadA.start();
		
		synchronized (lock) {
			while (allResults == false) {
				try {
		//	System.out.println(Thread.currentThread().getName()+" is waiting for the resultSet and going into sleep");		
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}// end of while 
		} // end of sync
		if (nnQueue.size() < k)	{
		System.out.println("queue size "+nnQueue.size()+" is less than k "+k);
	//	System.out.println("resetting all ");
			requestCount = 0;
			responseCount = 0;
			delta += delta;
			allResults = false;
			nnQueue.clear();
	//	System.out.println("queue size "+nnQueue.size()+" request "+requestCount+" response "+responseCount+" delta "+delta);;
		} else {
			sizeCheck = true;
		}
	}
	//	System.out.println(Thread.currentThread().getName()+" is not waiting this time");
	

		// show top k results 
		storage.showKnnResults();
		
		System.out.println("Demo over, will shutdown in 30s.");
		test.WaitingArea.waitTillSignal(1000 * 30);

		// shutting down the p2p. This is not a mandatory phase,
		// but it can avoid some manager to throw exceptions
		System.out.println("shutdown ...");
		p2p.leave();
		index.shutdown();

	}

	KnnQuery returnThis() {
		return this;
	}

	/**
	 * Implementation of the P2PListener interface. Is invoked when a new
	 * message is received and this node is responsible for the message's
	 * destination key. Reassembles the message text and displays it on the
	 * command line.
	 */
	public void newMessage(Message message, Peer origin) {
		String str = "";
		try {
			str = new String(message.getData(), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		System.out.println("Received message: " + str + " from "
				+ origin.toString());
	}

	/**
	 * Invoked when a new search result is available
	 * 
	 * @param guid
	 *            the GUID of the original query
	 * @param results
	 *            a Collection of DataItems matching the original query
	 */
	@SuppressWarnings("rawtypes")
	public void newSearchResult(GUID guid, Collection results) {

		for (Iterator it = results.iterator(); it.hasNext();) {
			IndexEntry entry = (IndexEntry) it.next();
			BitVector[] xy = MGridUtils.HilbertInverseConvertor(Long.parseLong(entry.getKey().toString(),2));
			Long x = xy[0].toExactLong();
			Long y = xy[1].toExactLong();
			if (!((queryx == x) & (queryy == y))) {
				Point point = new Point(x,y,entry.getmPointID());
		//	System.out.println("adding point :"+x+" "+y);
				nnQueue.add(point);
			}
		}

	}// end of new Search

	
	public void newSearchResult(GUID guid, int resultSize) {
//		System.out.println(" resultsize: "+resultSize);
		for ( int i=0; i<resultSize; i++) {
		Point point = new Point(0L,0L,0L);
		nnQueue.add(point);
	}
	}
	/**
	 * Invoked when a search resulted in no results.
	 * 
	 * @param guid
	 *            the GUID of the original query
	 */
	public void noResultsFound(GUID guid) {
		// System.out.println("No results found for search.");
	}

	/**
	 * Invoked when a search failed.
	 * 
	 * @param guid
	 *            the GUID of the original query
	 */
	public void searchFailed(GUID guid) {
	//	System.out.println("Search failed.");
	}

	/**
	 * Invoked when a search finished.
	 * 
	 * @param guid
	 *            the GUID of the original query
	 */
	public void searchFinished(GUID guid) {
		
		responseCount++;
//	System.out.println("request : "+requestCount +" response "+responseCount);
	//	System.out.println("Search finished.");
	}

	/**
	 * Invoked when a search started (reached a responsible peer).
	 * 
	 * @param guid
	 *            the GUID of the original query
	 * @param message
	 *            the explanation message.
	 */
	public void searchStarted(GUID guid, String message) {
		System.out.println("Search started.");
	}

	private static List<LongRange> rangesToQueryRegion(int[][] ranges) {
		List<LongRange> region = new ArrayList<>();
		for (int j = 0; j < ranges.length; ++j) {
			region.add(LongRange.of(ranges[j][0], ranges[j][1] + 1));
		}
		return region;
	}

	private static List<FilteredIndexRange<Object, LongRange>> queryto(
			List<LongRange> region, SpaceFillingCurve sfc, int maxRanges,
			Map<Pow2LengthBitSetRange, NodeValue<BigIntegerContent>> rolledupMap) {
		List<? extends List<LongRange>> x = ImmutableList.of(region);
		LongContent zero = new LongContent(0L);
		LongContent one = new LongContent(1L);
		Object filter = "";

		RegionInspector<Object, LongContent> simpleRegionInspector = SimpleRegionInspector
				.create(x, one, Functions.constant(filter),
						LongRangeHome.INSTANCE, zero);

		// Not using using sub-ranges here.
		PlainFilterCombiner<Object, Long, LongContent, LongRange> combiner = new PlainFilterCombiner<>(
				filter);
		QueryBuilder<Object, LongRange> queryBuilder = BacktrackingQueryBuilder
				.create(simpleRegionInspector, combiner, maxRanges, true,
						LongRangeHome.INSTANCE, zero);
		sfc.accept(new ZoomingSpaceVisitorAdapter(sfc, queryBuilder));
		Query<Object, LongRange> query = queryBuilder.get();
		return query.getFilteredIndexRanges();
	}

	private void hilbertknnQuery(SearchListener listner, Long queryx,
			Long queryy, double delta) {
		MultiDimensionalSpec spec = new MultiDimensionalSpec(Ints.asList(
				MGridUtils.xBits, MGridUtils.yBits));
		SpaceFillingCurve sfc = new CompactHilbertCurve(spec);
		int[] maxLengthPerDimension = new int[spec.getBitsPerDimension().size()];
		int m = 17;
		Arrays.fill(maxLengthPerDimension, m);

		int[][] window = MGridUtils.calculateRanges(queryx, queryy, delta);
		int[][] ranges = generateRanges(spec, maxLengthPerDimension, window);
		final int maxFilteredRanges = 20;

		List<LongRange> region = rangesToQueryRegion(ranges);
		List<FilteredIndexRange<Object, LongRange>> indexRanges = queryto(
				region, sfc, maxFilteredRanges, null);
		BitVector start = BitVectorFactories.OPTIMAL.apply(spec
				.sumBitsPerDimension());
		BitVector end = BitVectorFactories.OPTIMAL.apply(spec
				.sumBitsPerDimension());
		for (int i = 0; i < indexRanges.size(); ++i) {
			FilteredIndexRange<Object, LongRange> indexRange = indexRanges
					.get(i);
			Long startLong = indexRange.getIndexRange().getStart();
			start.copyFrom(startLong);
			Long endLong = indexRange.getIndexRange().getEnd();
			end.copyFrom(endLong);
			Long xmin = new Long(window[0][0]);
			Long xmax = new Long(window[1][0]);
			Long ymin = new Long(window[0][1]);
			Long ymax =  new Long(window[1][1]);
		 System.out.println("Searching in between "+start.toString()+" "+end.toString());
			p2p.index.Query rangeQuery = indexFactory.createQuery(type,	start.toString(), end.toString(), xmin, xmax, ymin, ymax);
			String localpath = mPGridP2P.getLocalPath();
			String tempLowerkey = start.toString().substring(0, localpath.length());
			Long lkey = Long.parseLong(tempLowerkey, 2);
			String tempHigherKey = end.toString().substring(0, localpath.length());
			Long hkey = Long.parseLong(tempHigherKey, 2);
			Long numPeers = (hkey - lkey)+1;	
			requestCount+=numPeers;
			System.out.println(" request count is : "+requestCount);
			index.search(rangeQuery, listner);

		}
	}

	private static int[][] generateRanges(MultiDimensionalSpec spec,
			int[] maxLengthPerDimension, int[][] window) {
		int[][] ranges = new int[spec.getBitsPerDimension().size()][2];
		// both ends inclusive xmin , xmax , ymin , ymax
		ranges[0][0] = window[0][0];
		ranges[0][1] = window[1][0];
		ranges[1][0] = window[0][1];
		ranges[1][1] = window[1][1];
		return ranges;
	}

	private void showKnnResults() {
		if (!nnQueue.isEmpty()) {
			endTime = System.currentTimeMillis();
			System.out.println(nnQueue.size()+" hits in time "+ ((endTime - startTime)) + " ms.");
			/*for (int i = 0; i < k; i++) {
				endTime = System.nanoTime();
				Point p = ((PriorityBlockingQueue<Point>) nnQueue).poll();
			//	System.out.println(p.x + " " + p.y + " " + p.id + " "
			//			+ ((endTime - startTime) / 1000000) + " ms.");
				System.out.println(nnQueue.size()+" hits in time "+ ((endTime - startTime) / 1000000) + " ms.");
			}*/

		}
	}

	
}
