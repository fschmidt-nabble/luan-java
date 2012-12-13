package luan.interp;


final class SetLocalVar implements Settable {
	private final int index;

	SetLocalVar(int index) {
		this.index = index;
	}

	@Override public void set(LuaStateImpl lua,Object value) {
		lua.stack()[index] = value;
	}
}
