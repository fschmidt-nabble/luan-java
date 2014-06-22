package luan.impl;

import luan.LuanSource;


final class GetLocalVar extends CodeImpl implements Expr {
	private final int index;

	GetLocalVar(LuanSource.Element se,int index) {
		super(se);
		this.index = index;
	}

	@Override public Object eval(LuanStateImpl luan) {
		return luan.stackGet(index);
	}
}
