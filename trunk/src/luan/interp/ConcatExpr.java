package luan.interp;

import luan.Lua;
import luan.LuaFunction;
import luan.LuaException;
import luan.LuaSource;


final class ConcatExpr extends BinaryOpExpr {

	ConcatExpr(LuaSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		Object o1 = op1.eval(lua);
		Object o2 = op2.eval(lua);
		String s1 = Lua.asString(o1);
		String s2 = Lua.asString(o2);
		if( s1 != null && s2 != null )
			return s1 + s2;
		LuaFunction fn = lua.getBinHandler(se,"__concat",o1,o2);
		if( fn != null )
			return Lua.first(lua.call(fn,se,"__concat",o1,o2));
		String type = s1==null ? Lua.type(o1) : Lua.type(o2);
		throw new LuaException( lua, se, "attempt to concatenate a " + type + " value" );
	}
}
