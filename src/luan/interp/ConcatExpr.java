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
		return toString(op1.eval(lua)) + toString(op2.eval(lua));
	}

	private static String toString(Object v) throws LuaException {
		String s = Lua.asString(v);
		if( s==null )
			throw new LuaException( "attempt to concatenate a " + Lua.type(v) + " value" );
		return s;
	}
}
