package luan.interp;

import luan.Lua;
import luan.LuaFunction;
import luan.LuaException;
import luan.LuaSource;


final class LeExpr extends BinaryOpExpr {

	LeExpr(LuaSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		Object o1 = op1.eval(lua);
		Object o2 = op2.eval(lua);
		if( o1 instanceof Number && o2 instanceof Number ) {
			Number n1 = (Number)o1;
			Number n2 = (Number)o2;
			return n1.doubleValue() <= n2.doubleValue();
		}
		if( o1 instanceof String && o2 instanceof String ) {
			String s1 = (String)o1;
			String s2 = (String)o2;
			return s1.compareTo(s2) <= 0;
		}
		LuaFunction fn = lua.getBinHandler(se,"__le",o1,o2);
		if( fn != null )
			return Lua.toBoolean( Lua.first(lua.call(fn,se,"__le",o1,o2)) );
		fn = lua.getBinHandler(se,"__lt",o1,o2);
		if( fn != null )
			return !Lua.toBoolean( Lua.first(lua.call(fn,se,"__lt",o2,o1)) );
		throw new LuaException( lua, se, "attempt to compare " + Lua.type(o1) + " with " + Lua.type(o2) );
	}
}
