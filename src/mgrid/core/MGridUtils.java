package mgrid.core;

import java.util.List;

import pgrid.Properties;
import pgrid.interfaces.basic.PGridP2P;



import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.google.uzaygezen.core.BitVector;
import com.google.uzaygezen.core.BitVectorFactories;
import com.google.uzaygezen.core.CompactHilbertCurve;
import com.google.uzaygezen.core.MultiDimensionalSpec;
import com.google.uzaygezen.core.SpaceFillingCurve;




public class MGridUtils {
	
	
	/**
	 * The P-Grid facility.
	 */
	private static PGridP2P mPGridP2P = PGridP2P.sharedInstance();

	public static final int xBits = Integer.parseInt(mPGridP2P.propertyString(Properties.BITS_X_DIMENSION));
	
	public static final  int yBits = Integer.parseInt(mPGridP2P.propertyString(Properties.BITS_Y_DIMENSION));

	private static final 	MultiDimensionalSpec spec = new MultiDimensionalSpec(Ints.asList(
			xBits, yBits));

	/**
	 * Converts two long numbers into Hilbert Mapping.
	 * 
	 * @param x
	 *            : the x cordinate of the point
	 * @param y
	 *            : the y cordinate of the point
	 *            {@code BitVector bv = Utils.HilbertConvertor(h, j);
				    byte[] rowkey = bv.toBigEndianByteArray();}
	 * @return BitVector
	 */

	public static BitVector HilbertConvertor(Long x, Long y) {
		  int[] raw = new int[] {xBits,yBits};
		CompactHilbertCurve chc = new CompactHilbertCurve(raw);
		List<Integer> bitsPerDimension = chc.getSpec().getBitsPerDimension();
		BitVector[] p = new BitVector[bitsPerDimension.size()];
		for (int i = p.length; --i >= 0;) {
			p[i] = BitVectorFactories.OPTIMAL.apply(bitsPerDimension.get(i));
		}
		p[0].copyFrom(x);
		p[1].copyFrom(y);
		BitVector chi = BitVectorFactories.OPTIMAL.apply(chc.getSpec()
				.sumBitsPerDimension());
		chc.index(p, 0, chi);
		// System.out.println(String.format(Locale.ROOT, "index([%s, %s])=%s",
		// p[0], p[1], chi));
		return chi;
	}

	/**
	 * Converts a Hilbert Mapping into Inverse Hilbert Mapping
	 * 
	 * @param byte[] row key {@code  long x = point[0].toExactLong();
		            long y = point[1].toExactLong();}
	 * @return BitVector[]
	 */
	public static BitVector[] HilbertInverseConvertor(byte[] row) {
		SpaceFillingCurve sfc = new CompactHilbertCurve(spec);
		BitVector[] point = new BitVector[spec.getBitsPerDimension().size()];
		BitVector index = BitVectorFactories.OPTIMAL.apply(spec
				.sumBitsPerDimension());
		for (int j = 0; j < spec.getBitsPerDimension().size(); ++j) {
			point[j] = BitVectorFactories.OPTIMAL.apply(spec
					.getBitsPerDimension().get(j));
		}

		index.copyFromBigEndian(row);
		sfc.indexInverse(index, point);
		return point;
	}

	/**
	 * Converts a Hilbert Mapping into Inverse Hilbert Mapping
	 * 
	 * @param Long
	 *            row key {@code  long x = point[0].toExactLong();
		            long y = point[1].toExactLong();}
	 * @return BitVector[]
	 */

	public static BitVector[] HilbertInverseConvertor(Long orig) {
	
		SpaceFillingCurve sfc = new CompactHilbertCurve(spec);
		BitVector[] point = new BitVector[spec.getBitsPerDimension().size()];

		BitVector index = BitVectorFactories.OPTIMAL.apply(spec
				.sumBitsPerDimension());
		for (int j = 0; j < spec.getBitsPerDimension().size(); ++j) {
			point[j] = BitVectorFactories.OPTIMAL.apply(spec
					.getBitsPerDimension().get(j));
		}

		index.copyFrom(orig);

		sfc.indexInverse(index, point);
		return point;
	}

	public static int[][] calculateRanges(Long x, Long y, double delta) {

		double xmin = Math.floor(x.doubleValue() - delta);
		double xmax = Math.ceil(x.doubleValue() + delta);
		double ymin = Math.floor(y.doubleValue() - delta);
		double ymax = Math.ceil(y.doubleValue() + delta);
		// not allowing range query to grow in negative space
		if (xmin < 0) {
			xmin = 0;
		}
		if (ymin < 0) {
			ymin = 0;
		}
		int[][] window = new int[][] { { (int) xmin, (int) ymin },
				{ (int) xmax, (int) ymax } };
		// System.out.println(window[0][0]+" "+window[0][1]+" "+window[1][0]+" "+window[1][1]);
		return window;
	}

	public static double nextRange(double k, double totalPoints) {
		return( ((2 / Math.sqrt(Math.PI)) * (1 - Math
				.sqrt(1 - (k / totalPoints)))) * 100000000);
	}
}
