package luan.interp;

import luan.LuaState;


final class EnvExpr implements Expr {
	static final EnvExpr INSTANCE = new EnvExpr();

	private EnvExpr() {}

	@Override public Object eval(LuaState lua) {
		return lua.env();
	}
}
