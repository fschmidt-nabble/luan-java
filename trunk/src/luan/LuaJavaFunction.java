package luan;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


public final class LuaJavaFunction extends LuaFunction {
	private final JavaMethod method;
	private final Object obj;
	private final RtnConverter rtnConverter;
	private final boolean takesLuaState;
	private final ArgConverter[] argConverters;
	private final Class<?> varArgCls;

	public LuaJavaFunction(Method method,Object obj) {
		this( JavaMethod.of(method), obj );
	}

	public LuaJavaFunction(Constructor constr,Object obj) {
		this( JavaMethod.of(constr), obj );
	}

	LuaJavaFunction(JavaMethod method,Object obj) {
		this.method = method;
		this.obj = obj;
		this.rtnConverter = getRtnConverter(method);
		this.takesLuaState = takesLuaState(method);
		this.argConverters = getArgConverters(takesLuaState,method);
		if( method.isVarArgs() ) {
			Class<?>[] paramTypes = method.getParameterTypes();
			this.varArgCls = paramTypes[paramTypes.length-1].getComponentType();
		} else {
			this.varArgCls = null;
		}
	}

	public Class<?>[] getParameterTypes() {
		return method.getParameterTypes();
	}

	@Override public Object[] call(LuaState lua,Object[] args) throws LuaException {
		args = fixArgs(lua,args);
		Object rtn;
		try {
			rtn = method.invoke(obj,args);
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch(InvocationTargetException e) {
			Throwable cause = e.getCause();
			if( cause instanceof Error )
				throw (Error)cause;
			if( cause instanceof RuntimeException )
				throw (RuntimeException)cause;
			if( cause instanceof LuaException )
				throw (LuaException)cause;
			throw new RuntimeException(e);
		} catch(InstantiationException e) {
			throw new RuntimeException(e);
		}
		return rtnConverter.convert(rtn);
	}

	private Object[] fixArgs(LuaState lua,Object[] args) {
		int n = argConverters.length;
		Object[] rtn;
		int start = 0;
		if( !takesLuaState && varArgCls==null && args.length == n ) {
			rtn = args;
		} else {
			if( takesLuaState )
				n++;
			rtn = new Object[n];
			if( takesLuaState ) {
				rtn[start++] = lua;
			}
			n = argConverters.length;
			if( varArgCls != null ) {
				n--;
				if( args.length < argConverters.length ) {
					rtn[rtn.length-1] = Array.newInstance(varArgCls,0);
				} else {
					int len = args.length - n;
					Object varArgs = Array.newInstance(varArgCls,len);
					ArgConverter ac = argConverters[n];
					for( int i=0; i<len; i++ ) {
						Array.set( varArgs, i, ac.convert(args[n+i]) );
					}
					rtn[rtn.length-1] = varArgs;
				}
			}
			System.arraycopy(args,0,rtn,start,Math.min(args.length,n));
		}
		for( int i=0; i<n; i++ ) {
			rtn[start+i] = argConverters[i].convert(rtn[start+i]);
		}
		return rtn;
	}


	private interface RtnConverter {
		public Object[] convert(Object obj);
	}

	private static final RtnConverter RTN_EMPTY = new RtnConverter() {
		public Object[] convert(Object obj) {
			return EMPTY_RTN;
		}
	};

	private static final RtnConverter RTN_ARRAY = new RtnConverter() {
		public Object[] convert(Object obj) {
			if( obj == null )
				return NULL_RTN;
			return (Object[])obj;
		}
	};

	private static final RtnConverter RTN_ONE = new RtnConverter() {
		public Object[] convert(Object obj) {
			return new Object[]{obj};
		}
	};

	private static final Object[] NULL_RTN = new Object[1];

	private static final RtnConverter RTN_NUMBER = new RtnConverter() {
		public Object[] convert(Object obj) {
			if( obj == null )
				return NULL_RTN;
			Number n = (Number)obj;
			LuaNumber ln = new LuaNumber(n.doubleValue());
			return new Object[]{ln};
		}
	};

	private static final RtnConverter RTN_NUMBER_ARRAY = new RtnConverter() {
		public Object[] convert(Object obj) {
			if( obj == null )
				return NULL_RTN;
			Object[] rtn = new Object[Array.getLength(obj)];
			for( int i=0; i<rtn.length; i++ ) {
				Number n = (Number)Array.get(obj,i);
				if( n != null )
					rtn[i] = new LuaNumber(n.doubleValue());
			}
			return rtn;
		}
	};

	private static RtnConverter getRtnConverter(JavaMethod m) {
		Class<?> rtnType = m.getReturnType();
		if( rtnType == Void.TYPE )
			return RTN_EMPTY;
		if( isNumber(rtnType) )
			return RTN_NUMBER;
		if( rtnType.isArray() ) {
			rtnType = rtnType.getComponentType();
			if( isNumber(rtnType) )
				return RTN_NUMBER_ARRAY;
			return RTN_ARRAY;
		}
		return RTN_ONE;
	}

	private static boolean isNumber(Class<?> rtnType) {
		return Number.class.isAssignableFrom(rtnType)
			|| rtnType == Byte.TYPE
			|| rtnType == Short.TYPE
			|| rtnType == Integer.TYPE
			|| rtnType == Long.TYPE
			|| rtnType == Float.TYPE
			|| rtnType == Double.TYPE
		;
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

	private static final ArgConverter ARG_BYTE = new ArgConverter() {
		public Object convert(Object obj) {
			if( obj instanceof LuaNumber ) {
				LuaNumber ln = (LuaNumber)obj;
				byte i = (byte)ln.n;
				if( i == ln.n )
					return Byte.valueOf(i);
			}
			else if( obj instanceof String ) {
				String s = (String)obj;
				try {
					return Byte.valueOf(s);
				} catch(NumberFormatException e) {}
			}
			return obj;
		}
	};

	private static boolean takesLuaState(JavaMethod m) {
		Class<?>[] paramTypes = m.getParameterTypes();
		return paramTypes.length > 0 && paramTypes[0].equals(LuaState.class);
	}

	private static ArgConverter[] getArgConverters(boolean takesLuaState,JavaMethod m) {
		final boolean isVarArgs = m.isVarArgs();
		Class<?>[] paramTypes = m.getParameterTypes();
		if( takesLuaState ) {
			Class<?>[] t = new Class<?>[paramTypes.length-1];
			System.arraycopy(paramTypes,1,t,0,t.length);
			paramTypes = t;
		}
		ArgConverter[] a = new ArgConverter[paramTypes.length];
		for( int i=0; i<a.length; i++ ) {
			Class<?> paramType = paramTypes[i];
			if( isVarArgs && i == a.length-1 )
				paramType = paramType.getComponentType();
			a[i] = getArgConverter(paramType);
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
		if( cls == Byte.TYPE || cls.equals(Byte.class) )
			return ARG_BYTE;
		return ARG_SAME;
	}



	private static abstract class JavaMethod {
		abstract boolean isVarArgs();
		abstract Class<?>[] getParameterTypes();
		abstract Object invoke(Object obj,Object... args)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException;
		abstract Class<?> getReturnType();
	
		static JavaMethod of(final Method m) {
			return new JavaMethod() {
				@Override boolean isVarArgs() {
					return m.isVarArgs();
				}
				@Override Class<?>[] getParameterTypes() {
					return m.getParameterTypes();
				}
				@Override Object invoke(Object obj,Object... args)
					throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
				{
					return m.invoke(obj,args);
				}
				@Override Class<?> getReturnType() {
					return m.getReturnType();
				}
				@Override public String toString() {
					return m.toString();
				}
			};
		}
	
		static JavaMethod of(final Constructor c) {
			return new JavaMethod() {
				@Override boolean isVarArgs() {
					return c.isVarArgs();
				}
				@Override Class<?>[] getParameterTypes() {
					return c.getParameterTypes();
				}
				@Override Object invoke(Object obj,Object... args)
					throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException
				{
					return c.newInstance(args);
				}
				@Override Class<?> getReturnType() {
					return c.getDeclaringClass();
				}
				@Override public String toString() {
					return c.toString();
				}
			};
		}
	
	}

}
