package luan.interp;

import luan.LuaState;


final class ConstExpr implements Expr {
	private final Object obj;

	ConstExpr(Object obj) {
		this.obj = obj;
	}

	@Override public Object eval(LuaState lua) {
		return obj;
	}
}
