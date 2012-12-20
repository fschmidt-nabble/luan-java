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

	public static String checkString(Object obj) throws LuaException {
		String s = asString(obj);
		if( s == null )
			throw new LuaException( "attempt to use a " + Lua.type(obj) + " as a string" );
		return s;
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
					return new LuaNumber( Double.parseDouble(s) );
				else
					return new LuaNumber( Long.parseLong(s,base) );
			} catch(NumberFormatException e) {}
		}
		return null;
	}

	public static LuaNumber checkNumber(Object obj) throws LuaException {
		LuaNumber n = toNumber(obj);
		if( n == null )
			throw new LuaException( "attempt to perform arithmetic on a " + type(obj) + " value" );
		return n;
	}

	public static LuaFunction checkFunction(Object obj) throws LuaException {
		if( obj instanceof LuaFunction )
			return (LuaFunction)obj;
		throw new LuaException( "attempt to call a " + type(obj) + " value" );
	}

}
