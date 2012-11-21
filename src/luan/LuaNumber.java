package luan;


public final class LuaNumber implements Comparable<LuaNumber> {
	final double n;

	public LuaNumber(double n) {
		this.n = n;
	}

	public double value() {
		return n;
	}

	// convert to Lua format
	@Override public String toString() {
		String s = Double.toString(n);
		int iE = s.indexOf('E');
		String ending  = null;
		if( iE != -1 ) {
			ending = s.substring(iE);
			s = s.substring(0,iE);
		}
		if( s.endsWith(".0") )
			s = s.substring(0,s.length()-2);
		if( ending != null )
			s += ending;
		return s;
	}

	@Override public boolean equals(Object obj) {
		if( !(obj instanceof LuaNumber) )
			return false;
		LuaNumber ln = (LuaNumber)obj;
		return n == ln.n;
	}

	@Override public int hashCode() {
		return Double.valueOf(n).hashCode();
	}

	@Override public int compareTo(LuaNumber ln) {
		return Double.compare(n,ln.n);
	}

}
