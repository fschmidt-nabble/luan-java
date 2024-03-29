package luan.impl;

import luan.LuanSource;


final class ConstExpr extends CodeImpl implements Expr {
	private final Object obj;

	ConstExpr(LuanSource.Element se,Object obj) {
		super(se);
		this.obj = obj;
	}

	@Override public Object eval(LuanStateImpl luan) {
		return obj;
	}

	@Override public String toString() {
		return "(ConstExpr "+obj+")";
	}
}
