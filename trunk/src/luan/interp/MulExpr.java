package luan.interp;

import luan.Luan;
import luan.LuanException;
import luan.LuanSource;


final class MulExpr extends BinaryOpExpr {

	MulExpr(LuanSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuanStateImpl luan) throws LuanException {
		Object o1 = op1.eval(luan);
		Object o2 = op2.eval(luan);
		Number n1 = Luan.toNumber(o1);
		Number n2 = Luan.toNumber(o2);
		if( n1 != null && n2 != null )
			return n1.doubleValue() * n2.doubleValue();
		return arithmetic(luan,"__mul",o1,o2);
	}
}
