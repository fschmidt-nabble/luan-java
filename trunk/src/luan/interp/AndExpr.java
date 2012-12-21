package luan.interp;

import luan.Lua;
import luan.LuaException;
import luan.LuaSource;


final class AndExpr extends BinaryOpExpr {

	AndExpr(LuaSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		Object v1 = op1.eval(lua);
		return !Lua.toBoolean(v1) ? v1 : op2.eval(lua);
	}
}
