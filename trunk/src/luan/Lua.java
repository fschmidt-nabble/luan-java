package luan;


public class Lua {
	public static final String version = "Luan 0.0";

	public static String type(Object obj) {
		if( obj == null )
			return "nil";
		if( obj instanceof String )
			return "string";
		if( obj instanceof Boolean )
			return "boolean";
		if( obj instanceof LuaNumber )
			return "number";
		return "userdata";
	}

	public static boolean toBoolean(Object obj) {
		return obj != null && !Boolean.FALSE.equals(obj);
	}

	public static String asString(Object obj) {
		if( obj instanceof String || obj instanceof LuaNumber )
			return obj.toString();
		return null;
	}

	public static LuaNumber toNumber(Object obj) {
		return toNumber(obj,null);
	}

	public static LuaNumber toNumber(Object obj,Integer base) {
		if( obj instanceof LuaNumber )
			return (LuaNumber)obj;
		if( obj instanceof String ) {
			String s = (String)obj;
			try {
				if( base==null )
					return LuaNumber.of( Double.parseDouble(s) );
				else
					return LuaNumber.of( Long.parseLong(s,base) );
			} catch(NumberFormatException e) {}
		}
		return null;
	}

	public static Object first(Object[] a) {
		return a.length==0 ? null : a[0];
	}

}
