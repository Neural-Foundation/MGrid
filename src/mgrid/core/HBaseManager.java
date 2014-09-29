package mgrid.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import p2p.index.IDBDataTypeHandler;
import pgrid.Constants;
import pgrid.GUID;
import pgrid.IndexEntry;
import pgrid.PGridHost;
import pgrid.PGridKey;
import pgrid.Properties;
import pgrid.Type;
import pgrid.core.DBManager;
import pgrid.core.index.DBIndexTable;
import pgrid.core.index.HostsCacheList;
import pgrid.core.index.IndexManager;
import pgrid.interfaces.basic.PGridP2P;
import pgrid.interfaces.index.PGridIndexFactory;

import com.google.common.io.Closeables;
import com.google.common.primitives.Ints;
import com.google.uzaygezen.core.BacktrackingQueryBuilder;
import com.google.uzaygezen.core.BigIntegerContent;
import com.google.uzaygezen.core.BitVector;
import com.google.uzaygezen.core.BitVectorFactories;
import com.google.uzaygezen.core.CompactHilbertCurve;
import com.google.uzaygezen.core.FilteredIndexRange;
import com.google.uzaygezen.core.HilbertIndexMasks;
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
import com.google.uzaygezen.core.ranges.RangeUtil;


/**
 * <p>Title: HBaseManagerr</p>
 * <p/>
 * <p>Description: This class manages the HBase DataBase connection and
 * transactions</p>
 *
 * @author Shashank Kumar
 * @email sk2z6@mst.edu
 */
/**
 * When I wrote this, only God and I understood what was I doing
 * 
 * 
 * Now, only God knows!
 */

public class HBaseManager implements Closeable {

	/**
	 * The DataBase manager
	 */
	protected DBManager mDBManager = DBManager.sharedInstance();

	/**
	 * The Index Manager
	 */
	private IndexManager mIndexManager = IndexManager.getInstance();
	/**
	 * The data item manager.
	 */
	protected DBIndexTable mDBIndexTable = IndexManager.getInstance()
			.getIndexTable();

	/**
	 * The reference to the only instance of this class (Singleton pattern).
	 * This differs from the C++ standard implementation by Gamma
	 * 
	 * et.al. since Java ensures the order of static initialization at runtime.
	 * 
	 * @see <a
	 *      href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */

	/**
	 * Constants for data table
	 */
	public static final byte[] DATA_FAMILY = Bytes.toBytes("data");
	
	private static Configuration conf = HBaseConfiguration.create();

	/**
	 * The P-Grid facility.
	 */
	private static PGridP2P mPGridP2P = PGridP2P.sharedInstance();
	private static String datatableName ;
	
	private static final 	String stype = mPGridP2P.propertyString(Properties.TYPE_NAME);
	private static final 	String sPort = mPGridP2P.propertyString(Properties.PORT_NUMBER);
	private static final MultiDimensionalSpec spec = new MultiDimensionalSpec(Ints.asList(
			MGridUtils.xBits, MGridUtils.yBits));
	
	// stores the data items
	public final HTable dataTable;

	private final HBaseAdmin admin; 

	private static final HBaseManager SHARED_INSTANCE = buildInstance();

	protected HBaseManager() throws IOException {
		if (mPGridP2P.propertyString(Properties.HTABLE_NAME).equalsIgnoreCase("%")) {
			datatableName = mPGridP2P.getLocalHost().getIP().getCanonicalHostName();
		} else {
			datatableName = mPGridP2P.propertyString(Properties.HTABLE_NAME);
		}

		this.admin = new HBaseAdmin(conf);

		if (!admin.tableExists(Bytes.toBytes(datatableName))) {
			System.out.println(datatableName
					+ " does not exists. Creating table");
			HTableDescriptor tdesc = new HTableDescriptor(datatableName);
			HColumnDescriptor cdesc = new HColumnDescriptor(DATA_FAMILY);
			cdesc.setMaxVersions(1);
			tdesc.addFamily(cdesc);
			admin.createTable(tdesc);
			System.out.println(datatableName + " created.");
		}
		this.dataTable = new HTable(conf, datatableName);
		this.dataTable.setAutoFlush(false);
		this.dataTable.setWriteBufferSize(1024*1024*12);

	}

	public void dropHBaseDataTable() {

		/*
		 * try { System.out.println("deleting "+this.datatableName+".");
		 * admin.disableTable(datatableName); admin.deleteTable(datatableName);
		 * admin.close(); } catch (IOException e) { e.printStackTrace(); }
		 */
	}

	@Override
	public void close() throws IOException {
		Closeables.closeQuietly(dataTable);
	}

	private static HBaseManager buildInstance() {
		try {
			HBaseManager SHARED_INSTANCE = new HBaseManager();
			System.out.println("HBaseManager instance acquired.");
			return SHARED_INSTANCE;

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}

	public static HBaseManager sharedInstance() {
		return SHARED_INSTANCE;
	}

	public void addIndexEntry(IndexEntry entry) {

		// not storing the co-ordinates
		Long id = entry.getPoint().id;
		String row = entry.getKey().toString();


		// Extract the original points and covert them to bigEndienByteArray
		BitVector[] b = MGridUtils.HilbertInverseConvertor(Long.parseLong(row,
				2));
		Long tempx = b[0].toExactLong();
		Long tempy = b[1].toExactLong();
		BitVector hkey = MGridUtils.HilbertConvertor(tempx, tempy);

		
			try {
			Put put = new Put(hkey.toBigEndianByteArray());
			// KEY is ROW
			put.add(DATA_FAMILY, Bytes.toBytes(id), Bytes.toBytes(id) );
			dataTable.put(put);
				Constants.LOGGER.info("Inserted in datatable" + " key:"
					+ row + " x:" + tempx + " y:" + tempy  +
					 " ID:"+ id);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Collection getSingleRecord(String row) {

		// data is KEY which can be ignored
		// convert string row to bigEndienbyteArray
		BitVector[] b = MGridUtils.HilbertInverseConvertor(Long.parseLong(row,
				2));
		Long tempx = b[0].toExactLong();
		Long tempy = b[1].toExactLong();
		BitVector hkey = MGridUtils.HilbertConvertor(tempx, tempy);
		Vector dataitems = new Vector();
		p2p.index.IndexEntry entry;
		String guid = "33";
		Long id = 0L;
		try {
			
			Get get = new Get(hkey.toBigEndianByteArray());
			Result result = this.dataTable.get(get);
			for (KeyValue kv : result.raw()) {
				 id = Bytes.toLong(kv.getQualifier());
			}
		
			GUID dGuid = GUID.getGUID(guid);
			Type type = (Type) PGridIndexFactory.sharedInstance().getTypeByString(stype);
			PGridKey key = new PGridKey(row);
			PGridHost host = PGridHost.getHost("33","localhost",	sPort);
			Point point = new Point(tempx, tempy, id);
			entry = mIndexManager.createIndexEntry(dGuid, type, key, host,point);
			dataitems.add(entry);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dataitems;

	}

	
	public Collection getRangeRecords(String ldataPrefix, String hdataPrefix, Long origxMin, Long origxMax, Long origyMin, Long origyMax) {
		Long count = 0L;
		Vector dataitems = new Vector();
		p2p.index.IndexEntry entry = null;
		SpaceFillingCurve sfc = new CompactHilbertCurve(spec);
		Long min = Long.parseLong(ldataPrefix, 2);
		BitVector bvMin = BitVectorFactories.OPTIMAL.apply(spec
				.sumBitsPerDimension());
		bvMin.copyFrom(min);

		Long max = Long.parseLong(hdataPrefix, 2);
		BitVector bvMax = BitVectorFactories.OPTIMAL.apply(spec
				.sumBitsPerDimension());
		bvMax.copyFrom(max);		
		int[][] ranges = generateRanges(spec,origxMin, origxMax, origyMin, origyMax);
		List<LongRange> region = rangesToQueryRegion(ranges);
		Scan scan = new Scan(bvMin.toBigEndianByteArray(),
				bvMax.toBigEndianByteArray());
		scan.setCacheBlocks(false);
		scan.setCaching(2000);
		
		BitVector[] point = new BitVector[spec.getBitsPerDimension().size()];
		BitVector index = BitVectorFactories.OPTIMAL.apply(spec
				.sumBitsPerDimension());
		for (int j = 0; j < spec.getBitsPerDimension().size(); ++j) {
			point[j] = BitVectorFactories.OPTIMAL.apply(spec
					.getBitsPerDimension().get(j));
		}
		scan.addFamily(DATA_FAMILY);
		ResultScanner scanner = null;
		final String guid = "600";
		final GUID dGuid = GUID.getGUID(guid);
		final Type type = (Type) PGridIndexFactory.sharedInstance()
				.getTypeByString(stype);
		final 	PGridHost host = PGridHost.getHost("9f1a","loclhost"	,"1805");

		
		try {
			 scanner = this.dataTable.getScanner(scan);
			for (Result result : scanner) {
				byte[] row = result.getRow();
				index.copyFromBigEndian(row);
				sfc.indexInverse(index, point);
			 boolean isContained = RangeUtil.contains(region, Arrays.asList(bitVectorPointToLongPoint(point)));
				if (isContained) {
				count +=result.list().size();
				}
				}
			PGridKey key = new PGridKey(count.toString());
			Point	 p = new Point(0L, 0L, 0L);
			 entry = mIndexManager.createIndexEntry(dGuid, type, key,host, p);
			dataitems.add(entry);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			scanner.close();
		}
		BitVector[] ol = MGridUtils.HilbertInverseConvertor(bvMin.toBigEndianByteArray());
		BitVector[] oh = MGridUtils.HilbertInverseConvertor(bvMax.toBigEndianByteArray());
		Constants.LOGGER.info("Searching for : "+ol[0].toExactLong()+" "+ol[1].toExactLong()+" "+
		oh[0].toExactLong()+" "+oh[1].toExactLong()+" returned "+count+" entries");
	//	System.out.println("Searching for : "+ldataPrefix+" "+hdataPrefix+" returned "+count+" entries");
		return dataitems;
	}

	

	private static Long[] bitVectorPointToLongPoint(BitVector[] point) {

		Long[] a = new Long[point.length];
		for (int i = 0; i < a.length; ++i) {
			a[i] = point[i].toExactLong();
		}
		return a;
	}
	
	/**
	 * Adds a Host to the helper table.
	 * 
	 * @param host
	 *            the host to add.
	 * @return The host ID if the host has been inserted, -1 otherwise.
	 */

	public int addHost(PGridHost host) {

		int id = HostsCacheList.containsKey(host.getGUID().toString()) ? HostsCacheList
				.get(host.getGUID().toString()) : -1;
		if (id >= 0)
			return id;
		// add new host
		String hostGUID = host.getGUID().toString();
		String hostAddress = host.getAddressString();
		int hostPort = host.getPort();
		int hostQOS = host.getSpeed();
		String hostPath = host.getPath();
		int hostID = mDBManager.mergeSQL(DBManager.HOSTS_TABLE, "GUID",
				"null,'" + hostGUID + "','" + hostAddress + "'," + hostPort
						+ ",'" + hostQOS + "','" + hostPath + "'," + 0);
		HostsCacheList.put(hostGUID, hostID);
		return hostID;

	}
	 private static List<LongRange> rangesToQueryRegion(int[][] ranges) {
		    List<LongRange> region = new ArrayList<>();
		    for (int j = 0; j < ranges.length; ++j) {
		      // scan is exclusive of range
		    	region.add(LongRange.of(ranges[j][0], ranges[j][1]+1));
		    }
		    return region;
		  }
	 
	 private static int[][] generateRanges( MultiDimensionalSpec spec, Long xmin, Long xmax, Long ymin, Long ymax) {
		 	int[][] ranges = new int[spec.getBitsPerDimension().size()][2];
			    // create a range query as (xmin , xmax) (ymin, ymax) = (1,1) (4,4), both ends inclusive		
			    ranges[0][0] = Ints.checkedCast(xmin);
			    ranges[0][1] = Ints.checkedCast(xmax);
			    ranges[1][0] = Ints.checkedCast(ymin);
			    ranges[1][1] = Ints.checkedCast(ymax);
			    return ranges;
			  }
	 
	 public void  flushTable() {
		try {
			this.dataTable.flushCommits();
		} catch (IOException e) {
			e.printStackTrace();
		}
		 
	 }

}
