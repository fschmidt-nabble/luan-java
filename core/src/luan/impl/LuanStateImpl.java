package luan.impl;

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
		MtGetterLink mtGetterLink;
		UpValue[] downValues = null;

		Frame( Frame previousFrame, Closure closure, int stackSize, Object[] varArgs) {
			this.previousFrame = previousFrame;
			this.closure = closure;
			this.stack = new Object[stackSize];
			this.varArgs = varArgs;
			this.mtGetterLink = closure.mtGetterLink();
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

		void addMetatableGetter(MetatableGetter mg) {
			if( mtGetterLink==null || !mtGetterLink.contains(mg) )
				mtGetterLink = new MtGetterLink(mg,mtGetterLink);
		}
	}

	private Frame frame = null;
	Object returnValues;
	Closure tailFn;
	MtGetterLink mtGetterLink = null;

	LuanStateImpl() {}

	private LuanStateImpl(LuanStateImpl luan) {
		super(luan);
	}

	@Override public LuanState shallowClone() {
//		if( frame != null )
//			throw new IllegalStateException("frame isn't null");
		return new LuanStateImpl(this);
	}

	// returns stack
	Object[] newFrame(Closure closure, int stackSize, Object[] varArgs) {
		returnValues = LuanFunction.NOTHING;
		tailFn = null;
		frame = new Frame(frame,closure,stackSize,varArgs);
		return frame.stack;
	}

	void popFrame() {
		returnValues = LuanFunction.NOTHING;
		tailFn = null;
		mtGetterLink = frame.mtGetterLink;
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

	@Override public LuanTable currentEnvironment() {
		if( frame==null )
			return null;
		return (LuanTable)frame.closure.upValues()[0].get();
	}

	MtGetterLink mtGetterLink() {
		return frame==null ? null : frame.mtGetterLink;
	}

	@Override public LuanTable getMetatable(Object obj,MetatableGetter beforeThis) {
		if( obj instanceof LuanTable ) {
			LuanTable table = (LuanTable)obj;
			return table.getMetatable();
		}
		MtGetterLink mtGetterLink = mtGetterLink();
		return mtGetterLink==null ? null : mtGetterLink.getMetatable(obj,beforeThis);
	}

	@Override public void addMetatableGetter(MetatableGetter mg) {
		frame.addMetatableGetter(mg);
	}

}
