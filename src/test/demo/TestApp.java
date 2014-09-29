package test.demo;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import mgrid.core.MGridUtils;
import mgrid.core.Point;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import pgrid.GUID;
import pgrid.PGridHost;
import pgrid.PGridKey;
import pgrid.Type;
import pgrid.interfaces.index.PGridIndexFactory;

import com.google.common.primitives.Ints;
import com.google.uzaygezen.core.BitVector;
import com.google.uzaygezen.core.BitVectorFactories;
import com.google.uzaygezen.core.CompactHilbertCurve;
import com.google.uzaygezen.core.MultiDimensionalSpec;
import com.google.uzaygezen.core.SpaceFillingCurve;
import com.google.uzaygezen.core.ranges.RangeUtil;

public class TestApp {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Logger logger = Logger.getLogger("MyLog");
		FileHandler fh;
		try {
			fh = new FileHandler("/home/hduser/vol01/slave23.log");
			logger.addHandler(fh);
			logger.setUseParentHandlers(false);
			SimpleFormatter  sf = new SimpleFormatter();
			fh.setFormatter(sf);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Configuration conf = HBaseConfiguration.create();
		HBaseAdmin admin = new HBaseAdmin(conf);
	HTable dataTable = new HTable(conf, "slave23.mst.edu");
	Scan scan = new Scan();
	scan.setCacheBlocks(false);
	scan.setCaching(10000);
	MultiDimensionalSpec spec = new MultiDimensionalSpec(Ints.asList(26,26));
	SpaceFillingCurve sfc = new CompactHilbertCurve(spec);
	BitVector[] point = new BitVector[spec.getBitsPerDimension().size()];
	BitVector index = BitVectorFactories.OPTIMAL.apply(spec
			.sumBitsPerDimension());
	for (int j = 0; j < spec.getBitsPerDimension().size(); ++j) {
		point[j] = BitVectorFactories.OPTIMAL.apply(spec
				.getBitsPerDimension().get(j));
	}
	ResultScanner scanner = null;
int count = 0;
	try {
		 scanner = dataTable.getScanner(scan);
		for (Result result : scanner) {
			byte[] row = result.getRow();
			index.copyFromBigEndian(row);
			sfc.indexInverse(index, point);
		
				 Long[] xy = bitVectorPointToLongPoint(point);
					Long x = xy[0];
					Long y = xy[1];
					logger.info(x+" "+y);
		count ++;
		if (count > 4000000)
			System.exit(0);
		}
		
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	private static Long[] bitVectorPointToLongPoint(BitVector[] point) {

		Long[] a = new Long[point.length];
		for (int i = 0; i < a.length; ++i) {
			a[i] = point[i].toExactLong();
		}
		return a;
	}
}
