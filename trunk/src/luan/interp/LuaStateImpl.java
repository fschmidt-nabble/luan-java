package luan.interp;

import luan.LuaState;
import luan.LuaTable;
import luan.LuaFunction;


final class LuaStateImpl implements LuaState {
	private final LuaTable env = new LuaTable();

	@Override public LuaTable env() {
		return env;
	}


	private static class Frame {
		final Frame previousFrame;
		final LuaClosure closure;
		final Object[] stack;
		final Object[] varArgs;
		UpValue[] downValues = null;

		Frame( Frame previousFrame, LuaClosure closure, int stackSize, Object[] varArgs) {
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
	Object[] returnValues;
	LuaClosure tailFn;

	// returns stack
	Object[] newFrame(LuaClosure closure, int stackSize, Object[] varArgs) {
		frame = new Frame(frame,closure,stackSize,varArgs);
		return frame.stack;
	}

	void popFrame() {
		returnValues = LuaFunction.EMPTY_RTN;
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

	LuaClosure closure() {
		return frame.closure;
	}

	UpValue getUpValue(int index) {
		return frame.getUpValue(index);
	}
}
