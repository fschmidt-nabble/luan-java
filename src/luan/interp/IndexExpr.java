package luan.interp;

import luan.Lua;
import luan.LuaException;
import luan.LuaState;
import luan.LuaTable;


final class IndexExpr extends BinaryOpExpr {

	IndexExpr(Expr op1,Expr op2) {
		super(op1,op2);
	}

	@Override Object eval(LuaState lua) throws LuaException {
		Object t = op1.eval(lua);
		if( t instanceof LuaTable ) {
			LuaTable tbl = (LuaTable)t;
			Object key = op2.eval(lua);
			return tbl.get(key);
		}
		throw new LuaException( "attempt to index a " + Lua.type(t) + " value" );
	}
}
