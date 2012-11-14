package luan;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;


public final class LuaJavaFunction extends LuaFunction {
	private final Method method;
	private final Object obj;
	private final RtnConverter rtnConverter;
	private final ArgConverter[] argConverters;

	public LuaJavaFunction(Method method,Object obj) {
		this.method = method;
		this.obj = obj;
		rtnConverter = getRtnConverter(method);
		argConverters = getArgConverters(method);
	}

	@Override public Object[] call(Object... args) {
		args = fixArgs(args);
		Object rtn;
		try {
			rtn = method.invoke(obj,args);
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch(InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		return rtnConverter.convert(rtn);
	}

	private Object[] fixArgs(Object[] args) {
		if( args.length != argConverters.length ) {
			Object[] t = new Object[argConverters.length];
			System.arraycopy(args,0,t,0,Math.min(args.length,t.length));
			args = t;
		}
		for( int i=0; i<args.length; i++ ) {
			args[i] = argConverters[i].convert(args[i]);
		}
		return args;
	}


	private interface RtnConverter {
		public Object[] convert(Object obj);
	}

	private static final Object[] EMPTY = new Object[0];

	private static final RtnConverter RTN_EMPTY = new RtnConverter() {
		public Object[] convert(Object obj) {
			return EMPTY;
		}
	};

	private static final RtnConverter RTN_SAME = new RtnConverter() {
		public Object[] convert(Object obj) {
			return new Object[]{obj};
		}
	};

	private static final RtnConverter RTN_NUMBER = new RtnConverter() {
		public Object[] convert(Object obj) {
			if( obj == null )
				return new Object[1];
			Number n = (Number)obj;
			LuaNumber ln = new LuaNumber(n.doubleValue());
			return new Object[]{ln};
		}
	};

	private static RtnConverter getRtnConverter(Method m) {
		Class<?> rtnType = m.getReturnType();
		if( rtnType == Void.TYPE )
			return RTN_EMPTY;
		if( Number.class.isAssignableFrom(rtnType)
			|| rtnType == Short.TYPE
			|| rtnType == Integer.TYPE
			|| rtnType == Long.TYPE
			|| rtnType == Float.TYPE
			|| rtnType == Double.TYPE
		)
			return RTN_NUMBER;
		return RTN_SAME;
	}


	private interface ArgConverter {
		public Object convert(Object obj);
	}

	private static final ArgConverter ARG_SAME = new ArgConverter() {
		public Object convert(Object obj) {
			return obj;
		}
	};

	private static final ArgConverter ARG_DOUBLE = new ArgConverter() {
		public Object convert(Object obj) {
			if( obj instanceof LuaNumber ) {
				LuaNumber ln = (LuaNumber)obj;
				return Double.valueOf(ln.n);
			}
			if( obj instanceof String ) {
				String s = (String)obj;
				try {
					return Double.valueOf(s);
				} catch(NumberFormatException e) {}
			}
			return obj;
		}
	};

	private static final ArgConverter ARG_FLOAT = new ArgConverter() {
		public Object convert(Object obj) {
			if( obj instanceof LuaNumber ) {
				LuaNumber ln = (LuaNumber)obj;
				return Float.valueOf((float)ln.n);
			}
			if( obj instanceof String ) {
				String s = (String)obj;
				try {
					return Float.valueOf(s);
				} catch(NumberFormatException e) {}
			}
			return obj;
		}
	};

	private static final ArgConverter ARG_LONG = new ArgConverter() {
		public Object convert(Object obj) {
			if( obj instanceof LuaNumber ) {
				LuaNumber ln = (LuaNumber)obj;
				long i = (long)ln.n;
				if( i == ln.n )
					return Long.valueOf(i);
			}
			else if( obj instanceof String ) {
				String s = (String)obj;
				try {
					return Long.valueOf(s);
				} catch(NumberFormatException e) {}
			}
			return obj;
		}
	};

	private static final ArgConverter ARG_INTEGER = new ArgConverter() {
		public Object convert(Object obj) {
			if( obj instanceof LuaNumber ) {
				LuaNumber ln = (LuaNumber)obj;
				int i = (int)ln.n;
				if( i == ln.n )
					return Integer.valueOf(i);
			}
			else if( obj instanceof String ) {
				String s = (String)obj;
				try {
					return Integer.valueOf(s);
				} catch(NumberFormatException e) {}
			}
			return obj;
		}
	};

	private static final ArgConverter ARG_SHORT = new ArgConverter() {
		public Object convert(Object obj) {
			if( obj instanceof LuaNumber ) {
				LuaNumber ln = (LuaNumber)obj;
				short i = (short)ln.n;
				if( i == ln.n )
					return Short.valueOf(i);
			}
			else if( obj instanceof String ) {
				String s = (String)obj;
				try {
					return Short.valueOf(s);
				} catch(NumberFormatException e) {}
			}
			return obj;
		}
	};

	private static ArgConverter[] getArgConverters(Method m) {
		Class<?>[] paramTypes = m.getParameterTypes();
		ArgConverter[] a = new ArgConverter[paramTypes.length];
		for( int i=0; i<a.length; i++ ) {
			a[i] = getArgConverter(paramTypes[i]);
		}
		return a;
	}

	private static ArgConverter getArgConverter(Class<?> cls) {
		if( cls == Double.TYPE || cls.equals(Double.class) )
			return ARG_DOUBLE;
		if( cls == Float.TYPE || cls.equals(Float.class) )
			return ARG_FLOAT;
		if( cls == Long.TYPE || cls.equals(Long.class) )
			return ARG_LONG;
		if( cls == Integer.TYPE || cls.equals(Integer.class) )
			return ARG_INTEGER;
		if( cls == Short.TYPE || cls.equals(Short.class) )
			return ARG_SHORT;
		return ARG_SAME;
	}

}
