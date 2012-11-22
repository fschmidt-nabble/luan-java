package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaException;
import luan.LuaState;


final class LeExpr extends BinaryOpExpr {

	LeExpr(Expr op1,Expr op2) {
		super(op1,op2);
	}

	@Override public Object eval(LuaState lua) throws LuaException {
		Object v1 = op1.eval(lua);
		Object v2 = op2.eval(lua);
		if( v1 instanceof LuaNumber && v2 instanceof LuaNumber ) {
			LuaNumber n1 = (LuaNumber)v1;
			LuaNumber n2 = (LuaNumber)v2;
			return n1.compareTo(n2) <= 0;
		}
		if( v1 instanceof String && v2 instanceof String ) {
			String s1 = (String)v1;
			String s2 = (String)v2;
			return s1.compareTo(s2) <= 0;
		}
		throw new LuaException( "attempt to compare " + Lua.type(v1) + " with " + Lua.type(v2) );
	}
}
