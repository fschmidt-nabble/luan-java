package luan.interp;

import luan.LuanSource;


final class GetUpVar extends CodeImpl implements Expr {
	private final int index;

	GetUpVar(LuanSource.Element se,int index) {
		super(se);
		this.index = index;
	}

	@Override public Object eval(LuanStateImpl luan) {
		return luan.closure().upValues[index].get();
	}
}
