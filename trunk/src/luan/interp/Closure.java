package luan.interp;

import luan.LuanFunction;
import luan.LuanState;
import luan.LuanElement;
import luan.LuanException;


final class Closure extends LuanFunction {
	private final Chunk chunk;
	final UpValue[] upValues;
	private final static UpValue[] NO_UP_VALUES = new UpValue[0];

	Closure(Chunk chunk,LuanStateImpl lua) {
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

	public Object[] call(LuanState lua,Object[] args) throws LuanException {
		return call(this,(LuanStateImpl)lua,args);
	}

	private static Object[] call(Closure closure,LuanStateImpl lua,Object[] args) throws LuanException {
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
					varArgs = LuanFunction.EMPTY_RTN;
				}
			}
			Object[] stack = lua.newFrame(closure,chunk.stackSize,varArgs);
			final int n = Math.min(args.length,chunk.numArgs);
			for( int i=0; i<n; i++ ) {
				stack[i] = args[i];
			}
			Object[] returnValues;
			Closure tailFn;
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
