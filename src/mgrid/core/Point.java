package mgrid.core;

import static com.google.common.base.Preconditions.checkArgument;

public class Point {

	public long x;
	public long y;
	public long id;
	
	public Point(long x, long y, long id) {
		
		checkArgument(0 <= x);
		checkArgument(0 <= y);
		
		this.x = x;
		this.y = y;
		this.id = id;
	}
	
	@Override
	public String toString() {
		String str;
		str = "x :" + Long.toString(x) + " y:" + Long.toString(y) + " id:" + Long.toString(id);
		return (str);
	}
}
