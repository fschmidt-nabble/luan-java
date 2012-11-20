package luan;


public class Lua {

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

	public static String toString(Object obj) {
		if( obj == null )
			return "nil";
		return obj.toString();
	}

	public static String checkString(Object obj) {
		return toString(obj);
	}

	public static LuaNumber toNumber(Object obj) {
		if( obj instanceof LuaNumber )
			return (LuaNumber)obj;
		if( obj instanceof String ) {
			String s = (String)obj;
			try {
				return new LuaNumber( Double.parseDouble(s) );
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
