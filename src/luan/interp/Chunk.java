package luan.interp;

import luan.LuaState;
import luan.LuaException;


final class Chunk implements Stmt {
	private final Stmt block;
	private final int stackSize;

	Chunk(Stmt block,int stackSize) {
		this.block = block;
		this.stackSize = stackSize;
	}

	@Override public void eval(LuaState lua) throws LuaException {
		lua.newStack(stackSize);
		try {
			block.eval(lua);
		} finally {
			lua.popStack();
		}
	}

}
