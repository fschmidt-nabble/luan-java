package luan.interp;

import luan.LuaSource;


final class VarArgs extends CodeImpl implements Expressions {

	VarArgs(LuaSource.Element se) {
		super(se);
	}

	@Override public Object[] eval(LuaStateImpl lua) {
		return lua.varArgs();
	}
}
