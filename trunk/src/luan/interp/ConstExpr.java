package luan.interp;

import luan.LuaSource;


final class ConstExpr implements Expr {
	private final Object obj;

	ConstExpr(Object obj) {
		this.obj = obj;
	}

	@Override public Object eval(LuaStateImpl lua) {
		return obj;
	}

	@Override public final LuaSource.Element se() {
		return null;
	}
}
