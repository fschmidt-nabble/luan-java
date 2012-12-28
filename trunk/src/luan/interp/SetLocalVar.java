package luan.interp;


final class SetLocalVar implements Settable {
	private final int index;

	SetLocalVar(int index) {
		this.index = index;
	}

	@Override public void set(LuanStateImpl lua,Object value) {
		lua.stackSet( index, value );
	}
}
