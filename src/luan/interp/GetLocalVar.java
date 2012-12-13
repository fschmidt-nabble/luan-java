package luan.interp;


final class GetLocalVar implements Expr {
	private final int index;

	GetLocalVar(int index) {
		this.index = index;
	}

	@Override public Object eval(LuaStateImpl lua) {
		return lua.stack()[index];
	}
}
