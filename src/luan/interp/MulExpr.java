package luan.interp;

import luan.Lua;
import luan.LuaException;
import luan.LuaSource;


final class MulExpr extends BinaryOpExpr {

	MulExpr(LuaSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		Object o1 = op1.eval(lua);
		Object o2 = op2.eval(lua);
		Number n1 = Lua.toNumber(o1);
		Number n2 = Lua.toNumber(o2);
		if( n1 != null && n2 != null )
			return n1.doubleValue() * n2.doubleValue();
		return arithmetic(lua,"__mul",o1,o2);
	}
}
