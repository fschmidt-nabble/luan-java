package luan.interp;

import luan.LuaState;


final class VarArgs implements Expressions {
	static final VarArgs INSTANCE = new VarArgs();

	private VarArgs() {}

	@Override public Object[] eval(LuaState lua) {
		return lua.varArgs();
	}
}
