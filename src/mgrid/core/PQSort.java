package mgrid.core;

import java.util.Comparator;

public class PQSort implements Comparator<Point> {

	public final Long pointX ;
	public Long pointY;
	
	public PQSort(Long x, Long y) {
		pointX = x;
		pointY = y;
	}

	
	private static double  distance(Long x, Long y , Point p) {
		double xdiff = p.x - x;
		double xpow = Math.pow(xdiff, 2);
		double ydiff = p.y - y;
		double ypow = Math.pow(ydiff, 2);
		double value = Math.sqrt(xpow+ypow);
		return value;
	}
	@Override
	public int compare(Point o1, Point o2) {
		double d1 = distance(pointX,pointY,o1);
		double d2 = distance(pointX,pointY,o2);
		if (d1 < d2) {
			return -1;
		}
		if (d1 > d2) {
			return 1;
		}
		return 0;
	}
}
