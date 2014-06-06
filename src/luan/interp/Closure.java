package luan.interp;

import luan.Luan;
import luan.LuanFunction;
import luan.LuanState;
import luan.LuanElement;
import luan.LuanException;
import luan.DeepCloner;
import luan.DeepCloneable;


final class Closure extends LuanFunction implements DeepCloneable<Closure> {
	private final FnDef fnDef;
	private UpValue[] upValues;

	Closure(LuanStateImpl luan,FnDef fnDef) throws LuanException {
		this.fnDef = fnDef;
		UpValue.Getter[] upValueGetters = fnDef.upValueGetters;
		upValues = new UpValue[upValueGetters.length];
		for( int i=0; i<upValues.length; i++ ) {
			upValues[i] = upValueGetters[i].get(luan);
		}
	}

	private Closure(Closure c) {
		this.fnDef = c.fnDef;
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

	@Override public Object call(LuanState luan,Object[] args) throws LuanException {
		return call(this,(LuanStateImpl)luan,args);
	}

	private static Object call(Closure closure,LuanStateImpl luan,Object[] args) throws LuanException {
		while(true) {
			FnDef fnDef = closure.fnDef;
			Object[] varArgs = null;
			if( fnDef.isVarArg ) {
				if( args.length > fnDef.numArgs ) {
					varArgs = new Object[ args.length - fnDef.numArgs ];
					for( int i=0; i<varArgs.length; i++ ) {
						varArgs[i] = args[fnDef.numArgs+i];
					}
				} else {
					varArgs = LuanFunction.NOTHING;
				}
			}
			Object[] stack = luan.newFrame(closure,fnDef.stackSize,varArgs);
			final int n = Math.min(args.length,fnDef.numArgs);
			for( int i=0; i<n; i++ ) {
				stack[i] = args[i];
			}
			Object returnValues;
			Closure tailFn;
			try {
				fnDef.block.eval(luan);
			} catch(ReturnException e) {
			} finally {
				returnValues = luan.returnValues;
				closure = luan.tailFn;
				luan.popFrame();
			}
			if( closure == null )
				return returnValues;
			args = Luan.array(returnValues);
		}
	}

}
