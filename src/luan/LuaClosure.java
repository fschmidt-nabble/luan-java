package luan;

import luan.interp.Chunk;
import luan.interp.ReturnException;


public final class LuaClosure extends LuaFunction {
	private final Chunk chunk;

	public LuaClosure(Chunk chunk,LuaState lua) {
		this.chunk = chunk;
	}

	public Object[] call(LuaState lua,Object... args) throws LuaException {
		Object[] stack = lua.newStack(chunk.stackSize);
		final int n = Math.min(args.length,chunk.numArgs);
		for( int i=0; i<n; i++ ) {
			stack[i] = args[i];
		}
		try {
			chunk.block.eval(lua);
		} catch(ReturnException e) {
		} finally {
			lua.popStack();
		}
		return lua.returnValues;
	}

}
