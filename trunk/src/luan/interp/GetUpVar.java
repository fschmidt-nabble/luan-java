package luan.interp;

import luan.LuaSource;


final class GetUpVar extends CodeImpl implements Expr {
	private final int index;

	GetUpVar(LuaSource.Element se,int index) {
		super(se);
		this.index = index;
	}

	@Override public Object eval(LuaStateImpl lua) {
		return lua.closure().upValues[index].get();
	}
}
