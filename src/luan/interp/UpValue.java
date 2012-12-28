package luan.interp;


final class UpValue {
	private Object[] stack;
	private int index;
	private boolean isClosed = false;
	private Object value;

	UpValue(Object[] stack,int index) {
		this.stack = stack;
		this.index = index;
	}

	UpValue(Object value) {
		this.value = value;
		this.isClosed = true;
	}

	Object get() {
		return isClosed ? value : stack[index];
	}

	void set(Object value) {
		if( isClosed ) {
			this.value = value;
		} else {
			stack[index] = value;
		}
	}

	void close() {
		value = stack[index];
		isClosed = true;
		stack = null;
	}

	static interface Getter {
		public UpValue get(LuanStateImpl lua);
	}

	static final class StackGetter implements Getter {
		private final int index;

		StackGetter(int index) {
			this.index = index;
		}

		public UpValue get(LuanStateImpl lua) {
			return lua.getUpValue(index);
		}
	}

	static final class NestedGetter implements Getter {
		private final int index;

		NestedGetter(int index) {
			this.index = index;
		}

		public UpValue get(LuanStateImpl lua) {
			return lua.closure().upValues[index];
		}
	}

	static final Getter globalGetter = new Getter() {
		public UpValue get(LuanStateImpl lua) {
			return new UpValue(lua.global());
		}
	};

}
