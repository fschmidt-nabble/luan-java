package luan.interp;

import luan.LuaState;
import luan.LuaTable;
import luan.LuaFunction;


final class LuaStateImpl implements LuaState {
	private final LuaTable env = new LuaTable();

	public LuaTable env() {
		return env;
	}


	private static class LuaStack {
		final LuaStack previousStack;
		final Object[] a;
		final Object[] varArgs;

		LuaStack( LuaStack previousStack, int stackSize, Object[] varArgs) {
			this.previousStack = previousStack;
			this.a = new Object[stackSize];
			this.varArgs = varArgs;
		}
	}

	private LuaStack stack = null;
	public Object[] returnValues;
	public LuaClosure tailFn;

	Object[] newStack(int stackSize, Object[] varArgs) {
		stack = new LuaStack(stack,stackSize,varArgs);
		return stack.a;
	}

	void popStack() {
		returnValues = LuaFunction.EMPTY_RTN;
		tailFn = null;
		stack = stack.previousStack;
	}

	public Object[] stack() {
		return stack.a;
	}

	public Object[] varArgs() {
		return stack.varArgs;
	}
}
