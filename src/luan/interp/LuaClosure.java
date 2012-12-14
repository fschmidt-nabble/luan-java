package luan.interp;

import luan.LuaFunction;
import luan.LuaState;
import luan.LuaException;


final class LuaClosure extends LuaFunction {
	private final Chunk chunk;
	final UpValue[] upValues;
	private final static UpValue[] NO_UP_VALUES = new UpValue[0];

	LuaClosure(Chunk chunk,LuaStateImpl lua) {
		this.chunk = chunk;
		UpValue.Getter[] upValueGetters = chunk.upValueGetters;
		if( upValueGetters.length==0 ) {
			upValues = NO_UP_VALUES;
		} else {
			upValues = new UpValue[upValueGetters.length];
			for( int i=0; i<upValues.length; i++ ) {
				upValues[i] = upValueGetters[i].get(lua);
			}
		}
	}

	public Object[] call(LuaState lua,Object... args) throws LuaException {
		return call(this,(LuaStateImpl)lua,args);
	}

	private static Object[] call(LuaClosure closure,LuaStateImpl lua,Object[] args) throws LuaException {
		while(true) {
			Chunk chunk = closure.chunk;
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
			Object[] stack = lua.newFrame(closure,chunk.stackSize,varArgs);
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
				closure = lua.tailFn;
				lua.popFrame();
			}
			if( closure == null )
				return returnValues;
			args = returnValues;
		}
	}

}
