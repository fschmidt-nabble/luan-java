package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaException;
import luan.LuaState;


final class ConcatExpr extends BinaryOpExpr {

	ConcatExpr(Expr op1,Expr op2) {
		super(op1,op2);
	}

	@Override Object eval(LuaState lua) throws LuaException {
		Object v1 = op1.eval(lua);
		Object v2 = op2.eval(lua);
		check(v1);
		check(v2);
		return Lua.toString(v1) + Lua.toString(v2);
	}

	private static void check(Object v) throws LuaException {
		if( !(v instanceof String || v instanceof LuaNumber) )
			throw new LuaException( "attempt to concatenate a " + Lua.type(v) + " value" );
	}
}
