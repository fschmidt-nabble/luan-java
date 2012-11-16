package luan.interp;

import luan.LuaState;


final class ConstExpr extends Expr {
	private final Object obj;

	ConstExpr(Object obj) {
		this.obj = obj;
	}

	@Override Object eval(LuaState lua) {
		return obj;
	}
}
