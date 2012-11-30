package luan.interp;

import luan.LuaState;


final class SetLocalVar implements Settable {
	private final int index;

	SetLocalVar(int index) {
		this.index = index;
	}

	@Override public void set(LuaState lua,Object value) {
		lua.stack()[index] = value;
	}
}
