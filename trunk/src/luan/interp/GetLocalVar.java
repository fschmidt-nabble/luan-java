package luan.interp;

import luan.LuaState;


final class GetLocalVar implements Expr {
	private final int index;

	GetLocalVar(int index) {
		this.index = index;
	}

	@Override public Object eval(LuaState lua) {
		return lua.stack()[index];
	}
}
