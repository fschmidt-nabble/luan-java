package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaFunction;
import luan.LuaException;
import luan.LuaSource;


final class LtExpr extends BinaryOpExpr {

	LtExpr(LuaSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		Object o1 = op1.eval(lua);
		Object o2 = op2.eval(lua);
		return lua.isLessThan(se,o1,o2);
	}
}
