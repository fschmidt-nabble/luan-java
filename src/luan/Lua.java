package luan;


public class Lua {

	public static String toString(Object obj) {
		if( obj == null )
			return "nil";
		return obj.toString();
	}

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

	public static int length(Object obj) throws LuaException {
		if( obj instanceof String ) {
			String s = (String)obj;
			return s.length();
		}
		if( obj instanceof LuaTable ) {
			LuaTable t = (LuaTable)obj;
			return t.length();
		}
		throw new LuaException( "attempt to get length of a " + type(obj) + " value" );
	}

	static LuaNumber toNumber(Object obj) throws LuaException {
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

	static LuaNumber add(Object n1,Object n2) throws LuaException {
		return new LuaNumber( toNumber(n1).n + toNumber(n2).n );
	}

	static LuaNumber sub(Object n1,Object n2) throws LuaException {
		return new LuaNumber( toNumber(n1).n - toNumber(n2).n );
	}

}
