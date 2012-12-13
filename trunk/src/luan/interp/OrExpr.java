package luan.interp;

import luan.Lua;
import luan.LuaException;


final class OrExpr extends BinaryOpExpr {

	OrExpr(Expr op1,Expr op2) {
		super(op1,op2);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		Object v1 = op1.eval(lua);
		return Lua.toBoolean(v1) ? v1 : op2.eval(lua);
	}
}
