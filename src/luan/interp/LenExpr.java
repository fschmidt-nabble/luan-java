package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaTable;
import luan.LuaFunction;
import luan.LuaException;


final class LenExpr extends UnaryOpExpr {

	LenExpr(Expr op) {
		super(op);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		Object o = op.eval(lua);
		if( o instanceof String ) {
			String s = (String)o;
			return new LuaNumber( s.length() );
		}
		LuaFunction fn = Utils.getHandler("__len",o);
		if( fn != null )
			return Utils.first(fn.call(lua,o));
		if( o instanceof LuaTable ) {
			LuaTable t = (LuaTable)o;
			return t.length();
		}
		throw new LuaException( "attempt to get length of a " + Lua.type(o) + " value" );
	}
}
