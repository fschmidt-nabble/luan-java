package luan.interp;


final class ConstExpr extends Expr {
	private final Object obj;

	ConstExpr(Object obj) {
		this.obj = obj;
	}

	@Override Object eval() {
		return obj;
	}
}
