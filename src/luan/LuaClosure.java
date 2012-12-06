package luan;

import luan.interp.Chunk;
import luan.interp.ReturnException;


public final class LuaClosure extends LuaFunction {
	private final Chunk chunk;

	public LuaClosure(Chunk chunk,LuaState lua) {
		this.chunk = chunk;
	}

	public Object[] call(LuaState lua,Object... args) throws LuaException {
		Chunk chunk = this.chunk;
		while(true) {
			Object[] varArgs = null;
			if( chunk.isVarArg ) {
				if( args.length > chunk.numArgs ) {
					varArgs = new Object[ args.length - chunk.numArgs ];
					for( int i=0; i<varArgs.length; i++ ) {
						varArgs[i] = args[chunk.numArgs+i];
					}
				} else {
					varArgs = LuaFunction.EMPTY_RTN;
				}
			}
			Object[] stack = lua.newStack(chunk.stackSize,varArgs);
			final int n = Math.min(args.length,chunk.numArgs);
			for( int i=0; i<n; i++ ) {
				stack[i] = args[i];
			}
			Object[] returnValues;
			LuaClosure tailFn;
			try {
				chunk.block.eval(lua);
			} catch(ReturnException e) {
			} finally {
				returnValues = lua.returnValues;
				tailFn = lua.tailFn;
				lua.popStack();
			}
			if( tailFn == null )
				return returnValues;
			chunk = tailFn.chunk;
			args = returnValues;
		}
	}

}
