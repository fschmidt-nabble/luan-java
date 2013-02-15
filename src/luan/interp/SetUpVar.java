package luan.interp;


final class SetUpVar implements Settable {
	private final int index;

	SetUpVar(int index) {
		this.index = index;
	}

	@Override public void set(LuanStateImpl luan,Object value) {
		luan.closure().upValues()[index].set(value);
	}
}
