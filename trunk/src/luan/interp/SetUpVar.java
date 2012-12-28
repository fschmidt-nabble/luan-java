package luan.interp;


final class SetUpVar implements Settable {
	private final int index;

	SetUpVar(int index) {
		this.index = index;
	}

	@Override public void set(LuanStateImpl lua,Object value) {
		lua.closure().upValues[index].set(value);
	}
}
