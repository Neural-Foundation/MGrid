package test.demo.mapreduce;

import java.io.IOException;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapreduce.Mapper;

import p2p.index.IndexEntry;

import com.google.common.primitives.Ints;
import com.google.uzaygezen.core.BitVector;
import com.google.uzaygezen.core.BitVectorFactories;
import com.google.uzaygezen.core.CompactHilbertCurve;
import com.google.uzaygezen.core.MultiDimensionalSpec;
import com.google.uzaygezen.core.SpaceFillingCurve;

import mgrid.core.*;



public class DataInsertMapper extends  TableMapper<ImmutableBytesWritable,Put>{

	
	@Override
public void map(ImmutableBytesWritable row, Result value, Context context) throws IOException, InterruptedException {
		MultiDimensionalSpec spec = new MultiDimensionalSpec(Ints.asList(17, 17));
		BitVector[] point = new BitVector[spec.getBitsPerDimension().size()];
		BitVector bindex = BitVectorFactories.OPTIMAL.apply(spec
				.sumBitsPerDimension());
		for (int j = 0; j < spec.getBitsPerDimension().size(); ++j) {
			point[j] = BitVectorFactories.OPTIMAL.apply(spec
					.getBitsPerDimension().get(j));
		}
		bindex.copyFromBigEndian(row.get());
		String rowString = bindex.toString();
		boolean inRange = rowString.startsWith("011");
		if (inRange) {
		Put put = new Put(row.get());
		for (KeyValue kv : value.raw()) {
			put.add(kv);
		}
		context.write(row, put);
		}
	
}

}
