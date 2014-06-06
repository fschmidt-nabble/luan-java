package luan;

import java.util.List;


public final class LuanBit {
	public final LuanState luan;
	public final LuanElement el;

	LuanBit(LuanState luan,LuanElement el) {
		this.luan = luan;
		this.el = el;
	}

	public LuanException exception(Object msg) {
		return new LuanException(this,msg);
	}

	public Object call(LuanFunction fn,String fnName) throws LuanException {
		return call(fn,fnName,LuanFunction.NOTHING);
	}

	public Object call(LuanFunction fn,String fnName,Object[] args) throws LuanException {
		List<StackTraceElement> stackTrace = luan.stackTrace;
		stackTrace.add( new StackTraceElement(el,fnName) );
		try {
			return fn.call(luan,args);
		} finally {
			stackTrace.remove(stackTrace.size()-1);
		}
	}

	public String checkString(Object obj) throws LuanException {
		String s = Luan.asString(obj);
		if( s == null )
			throw exception( "attempt to use a " + Luan.type(obj) + " as a string" );
		return s;
	}

	public Number checkNumber(Object obj) throws LuanException {
		Number n = Luan.toNumber(obj);
		if( n == null )
			throw exception( "attempt to perform arithmetic on a " + Luan.type(obj) + " value" );
		return n;
	}

	public LuanFunction checkFunction(Object obj) throws LuanException {
		if( obj instanceof LuanFunction )
			return (LuanFunction)obj;
		throw exception( "attempt to call a " + Luan.type(obj) + " value" );
	}

	public String toString(Object obj) throws LuanException {
		LuanFunction fn = getHandlerFunction("__tostring",obj);
		if( fn != null )
			return checkString( Luan.first( call(fn,"__tostring",new Object[]{obj}) ) );
		return Luan.toString(obj);
	}

	public String repr(Object obj) throws LuanException {
		LuanFunction fn = getHandlerFunction("__repr",obj);
		if( fn != null )
			return checkString( Luan.first( call(fn,"__repr",new Object[]{obj}) ) );
		String repr = Luan.repr(obj);
		if( repr==null )
			throw exception( "value '" + obj + "' doesn't support repr()" );
		return repr;
	}

	public LuanFunction getHandlerFunction(String op,Object obj) throws LuanException {
		Object f = luan.getHandler(op,obj);
		if( f == null )
			return null;
		return checkFunction(f);
	}

	public LuanFunction getBinHandler(String op,Object o1,Object o2) throws LuanException {
		LuanFunction f1 = getHandlerFunction(op,o1);
		if( f1 != null )
			return f1;
		return getHandlerFunction(op,o2);
	}

	public boolean isLessThan(Object o1,Object o2) throws LuanException {
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
		LuanFunction fn = getBinHandler("__lt",o1,o2);
		if( fn != null )
			return Luan.toBoolean( Luan.first(call(fn,"__lt",new Object[]{o1,o2})) );
		throw exception( "attempt to compare " + Luan.type(o1) + " with " + Luan.type(o2) );
	}

	public Object arithmetic(String op,Object o1,Object o2) throws LuanException {
		LuanFunction fn = getBinHandler(op,o1,o2);
		if( fn != null )
			return Luan.first(call(fn,op,new Object[]{o1,o2}));
		String type = Luan.toNumber(o1)==null ? Luan.type(o1) : Luan.type(o2);
		throw exception("attempt to perform arithmetic on a "+type+" value");
	}

}
