package luan.interp;

import luan.Luan;
import luan.LuanFunction;
import luan.LuanException;
import luan.LuanSource;


final class LtExpr extends BinaryOpExpr {

	LtExpr(LuanSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuanStateImpl lua) throws LuanException {
		Object o1 = op1.eval(lua);
		Object o2 = op2.eval(lua);
		return lua.isLessThan(se,o1,o2);
	}
}
