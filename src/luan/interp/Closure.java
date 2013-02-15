package luan.interp;

import luan.LuanFunction;
import luan.LuanState;
import luan.LuanElement;
import luan.LuanException;
import luan.DeepCloner;
import luan.DeepCloneable;


final class Closure extends LuanFunction implements DeepCloneable<Closure> {
	private final Chunk chunk;
	private UpValue[] upValues;
	private final static UpValue[] NO_UP_VALUES = new UpValue[0];

	Closure(LuanStateImpl luan,Chunk chunk) {
		this.chunk = chunk;
		UpValue.Getter[] upValueGetters = chunk.upValueGetters;
		if( upValueGetters.length==0 ) {
			upValues = NO_UP_VALUES;
		} else {
			upValues = new UpValue[upValueGetters.length];
			for( int i=0; i<upValues.length; i++ ) {
				upValues[i] = upValueGetters[i].get(luan);
			}
		}
	}

	private Closure(Closure c) {
		this.chunk = c.chunk;
	}

	@Override public Closure shallowClone() {
		return new Closure(this);
	}

	@Override public void deepenClone(Closure clone,DeepCloner cloner) {
		clone.upValues = cloner.deepClone(upValues);
	}

	UpValue[] upValues() {
		return upValues;
	}

	public Object[] call(LuanState luan,Object[] args) throws LuanException {
		return call(this,(LuanStateImpl)luan,args);
	}

	private static Object[] call(Closure closure,LuanStateImpl luan,Object[] args) throws LuanException {
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
			Object[] stack = luan.newFrame(closure,chunk.stackSize,varArgs);
			final int n = Math.min(args.length,chunk.numArgs);
			for( int i=0; i<n; i++ ) {
				stack[i] = args[i];
			}
			Object[] returnValues;
			Closure tailFn;
			try {
				chunk.block.eval(luan);
			} catch(ReturnException e) {
			} finally {
				returnValues = luan.returnValues;
				closure = luan.tailFn;
				luan.popFrame();
			}
			if( closure == null )
				return returnValues;
			args = returnValues;
		}
	}

}
