package luan.interp;

import luan.Lua;
import luan.LuaException;
import luan.LuaState;


final class OrExpr extends BinaryOpExpr {

	OrExpr(Expr op1,Expr op2) {
		super(op1,op2);
	}

	@Override Object eval(LuaState lua) throws LuaException {
		Object v1 = op1.eval(lua);
		return Lua.toBoolean(v1) ? v1 : op2.eval(lua);
	}
}
