package luan.interp;


final class GetUpVar implements Expr {
	private final int index;

	GetUpVar(int index) {
		this.index = index;
	}

	@Override public Object eval(LuaStateImpl lua) {
		return lua.closure().upValues[index].get();
	}
}
