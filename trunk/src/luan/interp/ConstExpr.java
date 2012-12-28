package luan.interp;

import luan.LuanSource;


final class ConstExpr implements Expr {
	private final Object obj;

	ConstExpr(Object obj) {
		this.obj = obj;
	}

	@Override public Object eval(LuanStateImpl lua) {
		return obj;
	}

	@Override public final LuanSource.Element se() {
		return null;
	}
}
