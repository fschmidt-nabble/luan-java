package luan.interp;

import luan.LuaSource;


final class GetLocalVar extends CodeImpl implements Expr {
	private final int index;

	GetLocalVar(LuaSource.Element se,int index) {
		super(se);
		this.index = index;
	}

	@Override public Object eval(LuaStateImpl lua) {
		return lua.stackGet(index);
	}
}
