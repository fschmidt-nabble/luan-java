package luan.interp;

import luan.LuaSource;


class CodeImpl implements Code {
	final LuaSource.Element se;

	CodeImpl(LuaSource.Element se) {
		this.se = se;
	}

	@Override public final LuaSource.Element se() {
		return se;
	}
}
