package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaTable;
import luan.LuaException;
import luan.LuaState;


final class LenExpr extends UnaryOpExpr {

	LenExpr(Expr op) {
		super(op);
	}

	@Override public Object eval(LuaState lua) throws LuaException {
		return new LuaNumber( length(op.eval(lua)) );
	}

	private static int length(Object obj) throws LuaException {
		if( obj instanceof String ) {
			String s = (String)obj;
			return s.length();
		}
		if( obj instanceof LuaTable ) {
			LuaTable t = (LuaTable)obj;
			return t.length();
		}
		throw new LuaException( "attempt to get length of a " + Lua.type(obj) + " value" );
	}
}
