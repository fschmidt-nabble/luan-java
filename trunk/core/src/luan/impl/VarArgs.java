package luan.impl;

import luan.LuanSource;


final class VarArgs extends CodeImpl implements Expressions {

	VarArgs(LuanSource.Element se) {
		super(se);
	}

	@Override public Object[] eval(LuanStateImpl luan) {
		return luan.varArgs();
	}
}
