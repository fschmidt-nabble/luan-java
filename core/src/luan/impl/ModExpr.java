package luan.impl;

import luan.Luan;
import luan.LuanException;
import luan.LuanSource;


final class ModExpr extends BinaryOpExpr {

	ModExpr(LuanSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuanStateImpl luan) throws LuanException {
		Object o1 = op1.eval(luan);
		Object o2 = op2.eval(luan);
		Number n1 = Luan.toNumber(o1);
		Number n2 = Luan.toNumber(o2);
		if( n1 != null && n2 != null ) {
			double d1 = n1.doubleValue();
			double d2 = n2.doubleValue();
			return d1 - Math.floor(d1/d2)*d2;
		}
		return arithmetic(luan,"__mod",o1,o2);
	}
}
