package luan;

import java.util.List;
import java.util.ArrayList;


public abstract class LuaState {
	private final LuaTable global = new LuaTable();
	private final List<MetatableGetter> mtGetters = new ArrayList<MetatableGetter>();
	final List<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>();

	public final LuaTable global() {
		return global;
	}

	public final LuaTable getMetatable(Object obj) {
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

	public final void addMetatableGetter(MetatableGetter mg) {
		mtGetters.add(mg);
	}

	public Object[] call(LuaFunction fn,LuaElement calledFrom,String fnName,Object... args) throws LuaException {
		stackTrace.add( new StackTraceElement(calledFrom,fnName) );
		try {
			return fn.call(this,args);
		} finally {
			stackTrace.remove(stackTrace.size()-1);
		}
	}

	public final String checkString(LuaElement el,Object obj) throws LuaException {
		String s = Lua.asString(obj);
		if( s == null )
			throw new LuaException( this, el, "attempt to use a " + Lua.type(obj) + " as a string" );
		return s;
	}

	public final Number checkNumber(LuaElement el,Object obj) throws LuaException {
		Number n = Lua.toNumber(obj);
		if( n == null )
			throw new LuaException( this, el, "attempt to perform arithmetic on a " + Lua.type(obj) + " value" );
		return n;
	}

	public final LuaFunction checkFunction(LuaElement el,Object obj) throws LuaException {
		if( obj instanceof LuaFunction )
			return (LuaFunction)obj;
		throw new LuaException( this, el, "attempt to call a " + Lua.type(obj) + " value" );
	}

	public final String toString(LuaElement el,Object obj) throws LuaException {
		LuaFunction fn = getHandlerFunction(el,"__tostring",obj);
		if( fn != null )
			return checkString( el, Lua.first( call(fn,el,"__tostring",obj) ) );
		if( obj == null )
			return "nil";
		if( obj instanceof Number )
			return Lua.toString((Number)obj);
		if( obj instanceof LuaException ) {
			LuaException le = (LuaException)obj;
			return le.getMessage();
		}
		return obj.toString();
	}

	public final LuaFunction getHandlerFunction(LuaElement el,String op,Object obj) throws LuaException {
		Object f = getHandler(op,obj);
		if( f == null )
			return null;
		return checkFunction(el,f);
	}

	public final Object getHandler(String op,Object obj) {
		LuaTable t = getMetatable(obj);
		return t==null ? null : t.get(op);
	}


	public final LuaFunction getBinHandler(LuaElement el,String op,Object o1,Object o2) throws LuaException {
		LuaFunction f1 = getHandlerFunction(el,op,o1);
		if( f1 != null )
			return f1;
		return getHandlerFunction(el,op,o2);
	}

	public final boolean isLessThan(LuaElement el,Object o1,Object o2) throws LuaException {
		if( o1 instanceof Number && o2 instanceof Number ) {
			Number n1 = (Number)o1;
			Number n2 = (Number)o2;
			return n1.doubleValue() < n2.doubleValue();
		}
		if( o1 instanceof String && o2 instanceof String ) {
			String s1 = (String)o1;
			String s2 = (String)o2;
			return s1.compareTo(s2) < 0;
		}
		LuaFunction fn = getBinHandler(el,"__lt",o1,o2);
		if( fn != null )
			return Lua.toBoolean( Lua.first(call(fn,el,"__lt",o1,o2)) );
		throw new LuaException( this, el, "attempt to compare " + Lua.type(o1) + " with " + Lua.type(o2) );
	}
}
