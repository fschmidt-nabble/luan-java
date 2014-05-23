package luan.interp;

import luan.LuanSource;


final class ConstExpr extends CodeImpl implements Expr {
	private final Object obj;

	ConstExpr(Object obj) {
		this(null,obj);
	}

	ConstExpr(LuanSource.Element se,Object obj) {
		super(se);
		this.obj = obj;
	}

	@Override public Object eval(LuanStateImpl luan) {
		return obj;
	}
}
