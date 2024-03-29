package luan.impl;

import luan.DeepCloner;
import luan.DeepCloneable;
import luan.LuanException;


final class UpValue implements DeepCloneable<UpValue> {
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

	private UpValue() {}

	@Override public UpValue shallowClone() {
		return new UpValue();
	}

	@Override public void deepenClone(UpValue clone,DeepCloner cloner) {
		clone.isClosed = isClosed;
		if( isClosed ) {
			clone.value = cloner.get(value);
		} else {
			clone.stack = cloner.deepClone(stack);
			clone.index = index;
		}
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
		public UpValue get(LuanStateImpl luan) throws LuanException;
	}

	static final class StackGetter implements Getter {
		private final int index;

		StackGetter(int index) {
			this.index = index;
		}

		public UpValue get(LuanStateImpl luan) {
			return luan.getUpValue(index);
		}
	}

	static final class NestedGetter implements Getter {
		private final int index;

		NestedGetter(int index) {
			this.index = index;
		}

		public UpValue get(LuanStateImpl luan) {
			return luan.closure().upValues()[index];
		}
	}

	static final class ValueGetter implements Getter {
		private final UpValue upValue;

		ValueGetter(Object value) {
			this.upValue = new UpValue(value);
		}

		public UpValue get(LuanStateImpl luan) {
			return upValue;
		}
	}

}
