package luan.impl;

import luan.Luan;
import luan.LuanException;
import luan.LuanSource;


final class NotExpr extends UnaryOpExpr {

	NotExpr(LuanSource.Element se,Expr op) {
		super(se,op);
	}

	@Override public Object eval(LuanStateImpl luan) throws LuanException {
		return !Luan.toBoolean(op.eval(luan));
	}

	@Override public String toString() {
		return "(NotExpr "+op+")";
	}
}
