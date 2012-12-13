package luan.interp;


final class EnvExpr implements Expr {
	static final EnvExpr INSTANCE = new EnvExpr();

	private EnvExpr() {}

	@Override public Object eval(LuaStateImpl lua) {
		return lua.env();
	}
}
