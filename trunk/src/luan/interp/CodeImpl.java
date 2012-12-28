package luan.interp;

import luan.LuanSource;


class CodeImpl implements Code {
	final LuanSource.Element se;

	CodeImpl(LuanSource.Element se) {
		this.se = se;
	}

	@Override public final LuanSource.Element se() {
		return se;
	}
}
