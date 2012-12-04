package luan;


public class LuaState {
	private final LuaTable env = new LuaTable();

	public LuaTable env() {
		return env;
	}


	private static class LuaStack {
		final LuaStack previousStack;
		final Object[] a;

		LuaStack( LuaStack previousStack, int stackSize) {
			this.previousStack = previousStack;
			this.a = new Object[stackSize];
		}
	}

	private LuaStack stack = null;
	public Object[] returnValues;

	Object[] newStack(int stackSize) {
		returnValues = LuaFunction.EMPTY_RTN;
		stack = new LuaStack(stack,stackSize);
		return stack.a;
	}

	void popStack() {
		stack = stack.previousStack;
	}

	public Object[] stack() {
		return stack.a;
	}

}
