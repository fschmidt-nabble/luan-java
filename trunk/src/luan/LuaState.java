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

	public void newStack(int stackSize) {
		stack = new LuaStack(stack,stackSize);
	}

	public void popStack() {
		stack = stack.previousStack;
	}

	public Object[] stack() {
		return stack.a;
	}

}
