package luan.interp;


final class UpValue {
	private Object[] stack;
	private int index;

	UpValue(Object[] stack,int index) {
		this.stack = stack;
		this.index = index;
	}

	Object get() {
		return stack[index];
	}

	void set(Object value) {
		stack[index] = value;
	}

	void close() {
		stack = new Object[]{get()};
		index = 0;
	}

	static interface Getter {
		public UpValue get(LuaStateImpl lua);
	}

	static final class StackGetter implements Getter {
		private final int index;

		StackGetter(int index) {
			this.index = index;
		}

		public UpValue get(LuaStateImpl lua) {
			return lua.getUpValue(index);
		}
	}

	static final class NestedGetter implements Getter {
		private final int index;

		NestedGetter(int index) {
			this.index = index;
		}

		public UpValue get(LuaStateImpl lua) {
			return lua.closure().upValues[index];
		}
	}

}
