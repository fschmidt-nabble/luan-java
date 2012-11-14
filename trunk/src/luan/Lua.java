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

	public static String toString(Object obj) {
		if( obj == null )
			return "nil";
		return obj.toString();
	}

	public static LuaNumber toNumber(Object obj) throws LuaException {
		if( obj instanceof LuaNumber )
			return (LuaNumber)obj;
		if( obj instanceof String ) {
			String s = (String)obj;
			try {
				return new LuaNumber( Double.parseDouble(s) );
			} catch(NumberFormatException e) {}
		}
		throw new LuaException( "attempt to perform arithmetic on a " + type(obj) + " value" );
	}

	public static LuaFunction toFunction(Object obj) throws LuaException {
		if( obj instanceof LuaFunction )
			return (LuaFunction)obj;
		throw new LuaException( "attempt to call a " + type(obj) + " value" );
	}

}
