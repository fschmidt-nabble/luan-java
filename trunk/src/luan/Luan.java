package luan;


public final class Luan {
	public static final String version = "Luan 0.0";

	public static String type(Object obj) {
		if( obj == null )
			return "nil";
		if( obj instanceof String )
			return "string";
		if( obj instanceof Boolean )
			return "boolean";
		if( obj instanceof Number )
			return "number";
		if( obj instanceof LuanTable )
			return "table";
		if( obj instanceof LuanFunction )
			return "function";
		return "userdata";
	}

	public static boolean toBoolean(Object obj) {
		return obj != null && !Boolean.FALSE.equals(obj);
	}

	public static String asString(Object obj) {
		if( obj instanceof String )
			return (String)obj;
		if( obj instanceof Number )
			return toString((Number)obj);
		return null;
	}

	public static Number toNumber(Object obj) {
		return toNumber(obj,null);
	}

	public static Number toNumber(Object obj,Integer base) {
		if( obj instanceof Number )
			return (Number)obj;
		if( obj instanceof String ) {
			String s = (String)obj;
			try {
				if( base==null )
					return Double.valueOf(s);
				else
					return Long.valueOf(s,base);
			} catch(NumberFormatException e) {}
		}
		return null;
	}

	public static Object first(Object[] a) {
		return a.length==0 ? null : a[0];
	}

	public static String toString(Number n) {
		if( n instanceof Integer )
			return n.toString();
		String s = n.toString();
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

	public static Integer asInteger(Object obj) {
		if( obj instanceof Integer )
			return (Integer)obj;
		if( !(obj instanceof Number) )
			return null;
		Number n = (Number)obj;
		int i = n.intValue();
		return i==n.doubleValue() ? Integer.valueOf(i) : null;
	}

	public static String toString(Object obj) {
		if( obj == null )
			return "nil";
		if( obj instanceof Number )
			return Luan.toString((Number)obj);
		if( obj instanceof LuanException ) {
			LuanException le = (LuanException)obj;
			return le.getMessage();
		}
		return obj.toString();
	}

	private Luan() {}  // never
}
