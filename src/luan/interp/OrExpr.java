package luan.interp;

import luan.Luan;
import luan.LuanException;
import luan.LuanSource;


final class OrExpr extends BinaryOpExpr {

	OrExpr(LuanSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuanStateImpl luan) throws LuanException {
		Object v1 = op1.eval(luan);
		return Luan.toBoolean(v1) ? v1 : op2.eval(luan);
	}
}
