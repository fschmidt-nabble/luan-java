package luan.interp;

import java.util.List;
import java.util.ArrayList;
import luan.Lua;
import luan.LuaState;
import luan.LuaTable;
import luan.LuaFunction;
import luan.MetatableGetter;
import luan.LuaException;
import luan.LuaElement;


final class LuaStateImpl extends LuaState {

	LuaFunction getBinHandler(LuaElement el,String op,Object o1,Object o2) throws LuaException {
		LuaFunction f1 = getHandlerFunction(el,op,o1);
		if( f1 != null )
			return f1;
		return getHandlerFunction(el,op,o2);
	}

	final Object arithmetic(LuaElement el,String op,Object o1,Object o2) throws LuaException {
		LuaFunction fn = getBinHandler(el,op,o1,o2);
		if( fn != null )
			return Lua.first(call(fn,el,op,o1,o2));
		String type = Lua.toNumber(o1)==null ? Lua.type(o1) : Lua.type(o2);
		throw new LuaException(this,el,"attempt to perform arithmetic on a "+type+" value");
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
