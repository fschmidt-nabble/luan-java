package luan;

import java.util.List;
import luan.modules.BasicLuan;


public final class Luan {

	public static void main(String[] args) {
		LuanState luan = LuanState.newStandard();
		try {
			LuanFunction standalone = (LuanFunction)BasicLuan.load_file(luan,"classpath:luan/cmd_line.luan",null);
			luan.call(standalone,args);
		} catch(LuanException e) {
			System.err.println(e.getFullMessage());
//			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static Object first(Object obj) {
		if( !(obj instanceof Object[]) )
			return obj;
		Object[] a = (Object[])obj;
		return a.length==0 ? null : a[0];
	}

	public static Object[] array(Object obj) {
		return obj instanceof Object[] ? (Object[])obj : new Object[]{obj};
	}

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
		if( obj instanceof byte[] )
			return "binary";
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

	public static String toString(Number n) {
		if( n instanceof Integer )
			return n.toString();
		int i = n.intValue();
		if( i == n.doubleValue() )
			return Integer.toString(i);
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
			return le.getFullMessage();
		}
		if( obj instanceof byte[] )
			return "binary: " + Integer.toHexString(obj.hashCode());
		return obj.toString();
	}

	public static String stringEncode(String s) {
		s = s.replace("\\","\\\\");
		s = s.replace("\u0007","\\a");
		s = s.replace("\b","\\b");
		s = s.replace("\f","\\f");
		s = s.replace("\n","\\n");
		s = s.replace("\r","\\r");
		s = s.replace("\t","\\t");
		s = s.replace("\u000b","\\v");
		s = s.replace("\"","\\\"");
		return s;
	}

	public static String repr(Object obj) {
		if( obj == null )
			return "nil";
		if( obj instanceof Boolean )
			return Luan.toString((Boolean)obj);
		if( obj instanceof Number )
			return Luan.toString((Number)obj);
		if( obj instanceof String )
			return "\"" + stringEncode((String)obj) + "\"";
		if( obj instanceof LuanRepr )
			return ((LuanRepr)obj).repr();
		return null;
	}

	public static LuanTable newTable() {
		return new LuanTableImpl();
	}

	public static LuanTable newTable(List<Object> list) {
		return new LuanTableImpl(list);
	}

	public static LuanTable newPropertyTable() {
		return new LuanPropertyTable();
	}

	private Luan() {}  // never
}
