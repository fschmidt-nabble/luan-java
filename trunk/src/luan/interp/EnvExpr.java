package luan.interp;

import luan.LuaState;


final class EnvExpr extends Expr {

	@Override Object eval(LuaState lua) {
		return lua.env();
	}
}
