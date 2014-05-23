package luan.interp;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.MetatableGetter;
import luan.LuanException;
import luan.LuanElement;
import luan.DeepCloner;


final class LuanStateImpl extends LuanState {

	private static class Frame {
		final Frame previousFrame;
		final Closure closure;
		final Object[] stack;
		final Object[] varArgs;
		UpValue[] downValues = null;

		Frame( Frame previousFrame, Closure closure, int stackSize, Object[] varArgs) {
			this.previousFrame = previousFrame;
			this.closure = closure;
			this.stack = new Object[stackSize];
			this.varArgs = varArgs;
		}

		void stackClear(int start,int end) {
			if( downValues != null ) {
				for( int i=start; i<end; i++ ) {
					UpValue downValue = downValues[i];
					if( downValue != null ) {
						downValue.close();
						downValues[i] = null;
					}
				}
			}
			for( int i=start; i<end; i++ ) {
				stack[i] = null;
			}
		}

		UpValue getUpValue(int index) {
			if( downValues==null )
				downValues = new UpValue[stack.length];
			if( downValues[index] == null )
				downValues[index] = new UpValue(stack,index);
			return downValues[index];
		}
	}

	private Frame frame = null;
	Object returnValues = LuanFunction.EMPTY;
	Closure tailFn;
	Map<UpValue.EnvGetter,UpValue> envs = new HashMap<UpValue.EnvGetter,UpValue>();

	LuanStateImpl() {}

	private LuanStateImpl(LuanStateImpl luan) {
		super(luan);
	}

	@Override public LuanState shallowClone() {
		if( frame != null )
			throw new IllegalStateException("frame isn't null");
		return new LuanStateImpl(this);
	}

	@Override public void deepenClone(LuanState clone,DeepCloner cloner) {
		super.deepenClone(clone,cloner);
		LuanStateImpl cloneImpl = (LuanStateImpl)clone;
		cloneImpl.envs = new HashMap<UpValue.EnvGetter,UpValue>();
		for( Map.Entry<UpValue.EnvGetter,UpValue> entry : envs.entrySet() ) {
			cloneImpl.envs.put( entry.getKey(), cloner.deepClone(entry.getValue()) );
		}
	}

	// returns stack
	Object[] newFrame(Closure closure, int stackSize, Object[] varArgs) {
		frame = new Frame(frame,closure,stackSize,varArgs);
		return frame.stack;
	}

	void popFrame() {
		returnValues = LuanFunction.EMPTY;
		tailFn = null;
		frame = frame.previousFrame;
	}

	Object stackGet(int index) {
		return frame.stack[index];
	}

	void stackSet(int index,Object value) {
		frame.stack[index] = value;
	}

	void stackClear(int start,int end) {
		frame.stackClear(start,end);
	}

	Object[] varArgs() {
		return frame.varArgs;
	}

	Closure closure() {
		return frame.closure;
	}

	UpValue getUpValue(int index) {
		return frame.getUpValue(index);
	}

	UpValue getUpValue(UpValue.EnvGetter getter) throws LuanException {
		UpValue uv = envs.get(getter);
		if( uv == null ) {
			LuanTable env = new LuanTable();
			uv = new UpValue(env);
			envs.put(getter,uv);
		}
		return uv;
	}

	@Override public LuanTable currentEnvironment() {
		if( frame==null )
			return null;
		return (LuanTable)frame.closure.upValues()[0].get();
	}

}
