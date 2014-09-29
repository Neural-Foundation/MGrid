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
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.hadoop.hbase.client.Scan;

import mgrid.core.MGridUtils;
import mgrid.core.Point;

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
public class DataInsert implements SearchListener, P2PListener {

	private P2PFactory p2pFactory;
	private P2P p2p;

	private IndexFactory indexFactory;
	private Index index;
	
	private long startTime = 0L;
	private long endTime = 0L;
	/**
	 * Create an instance of sample receiving peer.
	 *
	 */
	public DataInsert() {
	}


	/**
	 * Wait indefinitely for messages.
	 *
	 * @param bootIP     address of host to bootstrap from
	 * @param bootPort   service port of the boostrapping host
	 * @param properties additional properties needed to initialize the peer-to-peer layer
	 */
	public void run(InetAddress bootIP, int bootPort, Properties properties) {
		/** P2P INITIALIZATION **/

		// Set the debug mode to the minimum. Debug can be set to a number between 0-3
		properties.setProperty(PGridP2P.PROP_DEBUG_LEVEL, "3");
		// Use a verbose mode
		properties.setProperty(PGridP2P.PROP_VERBOSE_MODE, "false");

		// Get an instance of the P2PFactory
		p2pFactory = PGridP2PFactory.sharedInstance();
		System.out.println("Acquired P-Grid factory reference. ");

		// Get an instance of the P2P object, aka P-Grid
		p2p = p2pFactory.createP2P(properties);
		System.out.println("Created a P2P instance. ");

		// Create an instance of the bootstrap host that will be use to bootstrap the
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


		// Get an instance of the Storage object, aka PGridStorage
		index = indexFactory.createIndex(p2p);
		System.out.println("Storage instance acquired. ");

	/*	// creating and registering data type.
		Type type = indexFactory.createType("SimpleType");
		TypeHandler handler = new DefaultTypeHandler(type);
		indexFactory.registerTypeHandler(type, handler); */
		
		// creating and registering data type.
		Type type = indexFactory.createType("SimpleType");
		TypeHandler handler = new DefaultTypeHandler(type);
		indexFactory.registerTypeHandler(type, handler); 
		
		// Try to join the network
		p2p.join(bootstrap);
		System.out.println("Network joined. Current key range (path) is: " + ((PGridHost)p2p.getLocalPeer()).getPath());


		// add this object as a p2p listener
		p2p.addP2PListener(this);

		/** Storage Demo **/
		// In this small demo program we want to insert some data inside
		// the network, update some of them and perform
		// a query and a range query.
		// This demo comes first since the network is build upon data items
		// inserted in the network
		try {
			storageDemo(type);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// wait for the result set
		System.out.println("Demo over, will shutdown in 30s.");
		test.WaitingArea.waitTillSignal(1000 * 30);

		// shuting down the p2p. This is not a mandatory phase,
		// but it can avoid some manager to throw exceptions
		System.out.println("shutdown ...");
		p2p.leave();
		index.shutdown();

	}

	/**
	 * In this small demo program we want to insert some data inside
	 * the network, update some of them and perform
	 * a query and a range query.
	 */
	protected void storageDemo(Type type) throws IOException{


		// creating data entries.
	Vector<IndexEntry> entries = new Vector<IndexEntry>();

		int count = 0;
	
	 for ( int i = 400; i <=600;i=i+1) {
		for (int j=400 ; j <=600 ; j=j+1) {

					
		Long id = System.nanoTime();
		Point p = new Point(i, j, id);
		
		IndexEntry entry = indexFactory.createIndexEntry(type, p);
		count++;
		entries.add(entry);
		/*if (count % 100 == 0) {
			System.out.println("Inserted "+count+" entries");
			index.insert(entries);
			entries.clear();
		} */
	}
	}
		index.insert(entries);
		WaitingArea.waitTillSignal(1000 * 5);
		

		
		System.out.println("Inserted "+count+" data entries.");

		// ensure that PGrid will build the network (this is optional, by default P-Grid build the network)
		// you can have a look at PGrid.ini at the root of you home directory to see if initExchange is set
		// to true.
		((PGridP2P)p2p).setInitExchanges(true);


		// wait for the network to be created.
		System.out.println("Waiting while structuring the network.");
		WaitingArea.waitTillSignal(1000 * 60 );


		System.out.println("Network joined. Current key range (path) is: "
				+ ((PGridHost)p2p.getLocalPeer()).getPath());

	}

	/**
	 * In this demo, we want to send a custom message to an other
	 * peer to illustrate the routing facilities of the common interface.
	 */
	protected void p2pDemo() {


		String s = "Welcome to P-Grid! I'm peer "+p2p.getLocalPeer().getIP().getCanonicalHostName()+
				" on port "+p2p.getLocalPeer().getPort()+".";

		// create a custom message
		Message message = null;
		String str="";
		try {
			message = p2pFactory.createMessage(s.getBytes("ISO-8859-1"));
			str = new String(s.getBytes("ISO-8859-1"), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
		System.out.println("Message created: " + str);
		// generate the key that will be used to route the message
		Key key = p2pFactory.generateKey(s);
		System.out.println("Destination key: " + key);
		// route the message
		p2p.route(key, message);
		System.out.println("Message sent. ");

	}

	/**
	 * Let the sample peer send a message.
	 *
	 * @param args the command line arguments to be passed on to the peer-to-peer layer
	 * @see test.CommandLineArgs usage
	 */
	public static void main(String[] args) throws IOException {
		test.CommandLineArgs cla = new test.CommandLineArgs(args);
		DataInsert storage = new DataInsert();
		storage.run(cla.getAddress(), cla.getPort(), cla.getOtherProperties());
	}

	/**
	 * Implementation of the P2PListener interface. Is invoked
	 * when a new message is received and this node is responsible
	 * for the message's destination key. Reassembles the message
	 * text and displays it on the command line.
	 */
	public void newMessage(Message message, Peer origin) {
		String str="";
		try {
			str = new String(message.getData(), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		System.out.println("Received message: " + str + " from " + origin.toString());
	}


	/**
	 * Invoked when a new search result is available
	 *
	 * @param guid    the GUID of the original query
	 * @param results a Collection of DataItems matching the original query
	 */
	public void newSearchResult(GUID guid, Collection results) {
	//	System.out.println("Search result received:");
		this.endTime = System.nanoTime();
		for (Iterator it = results.iterator(); it.hasNext();) {
			IndexEntry entry = (IndexEntry)it.next();
//			BitVector[] xy = MGridUtils.HilbertInverseConvertor(Long.parseLong(entry.getKey().toString(),2));
			//	Long x = xy[0].toExactLong();
			//	Long y = xy[1].toExactLong();
			//	System.out.println("key: "+entry.getKey()+" x: "+x+" y: "+y+" id: " + entry.getmPointID()+
			//			" in time "+(endTime-startTime)+" ms");		
		}
	}

	/**
	 * Invoked when a search resulted in no results.
	 *
	 * @param guid the GUID of the original query
	 */
	public void noResultsFound(GUID guid) {
		//System.out.println("No results found for search.");
	}

	/**
	 * Invoked when a search failed.
	 *
	 * @param guid the GUID of the original query
	 */
	public void searchFailed(GUID guid) {
		System.out.println("Search failed.");
	}

	/**
	 * Invoked when a search finished.
	 *
	 * @param guid the GUID of the original query
	 */
	public void searchFinished(GUID guid) {
		//System.out.println("Search finished.");
	}

	/**
	 * Invoked when a search started (reached a responsible peer).
	 *
	 * @param guid the GUID of the original query
	 * @param message the explanation message.
	 */
	public void searchStarted(GUID guid, String message) {
		System.out.println("Search started.");
	}
	
	 private static int[][] generateRanges(MultiDimensionalSpec spec, int[] maxLengthPerDimension, Long xmin,
			 Long ymin, Long xmax, Long ymax) {
			    int[][] ranges = new int[spec.getBitsPerDimension().size()][2];
			    // create a range query as (xmin , ymin) (xmax, ymax) = (1,1) (4,4), both ends inclusive		
			    ranges[0][0] = Ints.checkedCast(xmin);
			    ranges[0][1] = Ints.checkedCast(xmax);;
			    ranges[1][0] = Ints.checkedCast(ymin);
			    ranges[1][1] = Ints.checkedCast(ymax);
			    return ranges;
			  }
	 private static List<LongRange> rangesToQueryRegion(int[][] ranges) {
		    List<LongRange> region = new ArrayList<>();
		    for (int j = 0; j < ranges.length; ++j) {
		      // scan is exclusive of range , can also set InclusiveStopFilter in scan
		    	region.add(LongRange.of(ranges[j][0], ranges[j][1]+1));
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
	
			    RegionInspector<Object, LongContent> simpleRegionInspector = SimpleRegionInspector.create(
			      x, one, Functions.constant(filter), LongRangeHome.INSTANCE, zero);
			  
			    // Not using using sub-ranges here.
			    PlainFilterCombiner<Object,Long, LongContent, LongRange> combiner = new PlainFilterCombiner<>(filter);
			    QueryBuilder<Object, LongRange> queryBuilder = BacktrackingQueryBuilder.create(simpleRegionInspector, combiner, maxRanges, true, LongRangeHome.INSTANCE, zero);
			    sfc.accept(new ZoomingSpaceVisitorAdapter(sfc, queryBuilder));
			    Query<Object, LongRange> query = queryBuilder.get();
			    return query.getFilteredIndexRanges();
			  }
	  
	 private void hilbertRangeQuery(Long xmin, Long xmax, Long ymin, Long ymax,  IndexFactory indexFactory, Type type, Index index, SearchListener listner) {
		 MultiDimensionalSpec spec = new MultiDimensionalSpec(Ints.asList(MGridUtils.xBits, MGridUtils.yBits));
		 SpaceFillingCurve sfc = new CompactHilbertCurve(spec);
		 int[] maxLengthPerDimension = new int[spec.getBitsPerDimension().size()];
		 int m = 32;
		 Arrays.fill(maxLengthPerDimension, m);
		int[][] ranges = generateRanges(spec, maxLengthPerDimension, xmin, ymin, xmax, ymax);
		 final int maxFilteredRanges = 20 ;
		 List<LongRange> region = rangesToQueryRegion(ranges); 
		  List<FilteredIndexRange<Object, LongRange>> indexRanges = queryto(region, sfc, maxFilteredRanges, null);
		  BitVector start = BitVectorFactories.OPTIMAL.apply(spec.sumBitsPerDimension());
		    BitVector end = BitVectorFactories.OPTIMAL.apply(spec.sumBitsPerDimension());
		    for (int i = 0; i < indexRanges.size(); ++i) {
			      FilteredIndexRange<Object, LongRange> indexRange = indexRanges.get(i);
			      Long startLong = indexRange.getIndexRange().getStart();
			      start.copyFrom(startLong);
			      Long endLong = indexRange.getIndexRange().getEnd();
			      end.copyFrom(endLong);
			      p2p.index.Query rangeQuery = indexFactory.createQuery(type, start.toString(), end.toString());
			      index.search(rangeQuery, listner);
			    }
	 }


	@Override
	public void newSearchResult(GUID guid, int resultSize) {
		// TODO Auto-generated method stub
		
	}
}
