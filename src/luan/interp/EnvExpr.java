package luan.interp;

import luan.LuaState;


final class EnvExpr extends Expr {
	static final EnvExpr INSTANCE = new EnvExpr();

	private EnvExpr() {}

	@Override Object eval(LuaState lua) {
		return lua.env();
	}
}
