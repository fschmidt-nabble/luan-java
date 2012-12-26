package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaTable;
import luan.LuaFunction;
import luan.LuaException;
import luan.LuaSource;


final class LenExpr extends UnaryOpExpr {

	LenExpr(LuaSource.Element se,Expr op) {
		super(se,op);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		Object o = op.eval(lua);
		if( o instanceof String ) {
			String s = (String)o;
			return LuaNumber.of( s.length() );
		}
		LuaFunction fn = lua.getHandlerFunction(se,"__len",o);
		if( fn != null )
			return Lua.first(lua.call(fn,se,"__len",o));
		if( o instanceof LuaTable ) {
			LuaTable t = (LuaTable)o;
			return t.length();
		}
		throw new LuaException( lua, se, "attempt to get length of a " + Lua.type(o) + " value" );
	}
}
