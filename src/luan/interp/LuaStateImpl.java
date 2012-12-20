package luan.interp;

import java.util.List;
import java.util.ArrayList;
import luan.Lua;
import luan.LuaState;
import luan.LuaTable;
import luan.LuaFunction;
import luan.MetatableGetter;
import luan.LuaException;


final class LuaStateImpl implements LuaState {
	private final LuaTable global = new LuaTable();
	private final List<MetatableGetter> mtGetters = new ArrayList<MetatableGetter>();

	@Override public LuaTable global() {
		return global;
	}

	@Override public String toString(Object obj) throws LuaException {
		LuaFunction fn = getHandlerFunction("__tostring",obj);
		if( fn != null )
			return Lua.checkString( Utils.first( fn.call(this,obj) ) );
		if( obj == null )
			return "nil";
		return obj.toString();
	}

	@Override public LuaTable getMetatable(Object obj) {
		if( obj instanceof LuaTable ) {
			LuaTable table = (LuaTable)obj;
			return table.getMetatable();
		}
		for( MetatableGetter mg : mtGetters ) {
			LuaTable table = mg.getMetatable(obj);
			if( table != null )
				return table;
		}
		return null;
	}

	public void addMetatableGetter(MetatableGetter mg) {
		mtGetters.add(mg);
	}

	Object getHandler(String op,Object obj) throws LuaException {
		LuaTable t = getMetatable(obj);
		return t==null ? null : t.get(op);
	}

	LuaFunction getHandlerFunction(String op,Object obj) throws LuaException {
		Object f = getHandler(op,obj);
		if( f == null )
			return null;
		return Lua.checkFunction(f);
	}

	LuaFunction getBinHandler(String op,Object o1,Object o2) throws LuaException {
		LuaFunction f1 = getHandlerFunction(op,o1);
		if( f1 != null )
			return f1;
		return getHandlerFunction(op,o2);
	}

	final Object arithmetic(String op,Object o1,Object o2) throws LuaException {
		LuaFunction fn = getBinHandler(op,o1,o2);
		if( fn != null )
			return Utils.first(fn.call(this,o1,o2));
		String type = Lua.toNumber(o1)==null ? Lua.type(o1) : Lua.type(o2);
		throw new LuaException("attempt to perform arithmetic on a "+type+" value");
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
