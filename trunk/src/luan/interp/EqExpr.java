package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaException;
import luan.LuaState;


final class EqExpr extends BinaryOpExpr {

	EqExpr(Expr op1,Expr op2) {
		super(op1,op2);
	}

	@Override Object eval(LuaState lua) throws LuaException {
		Object v1 = op1.eval(lua);
		Object v2 = op2.eval(lua);
		return v1 == v2 || v1 != null && v1.equals(v2);
	}
}
