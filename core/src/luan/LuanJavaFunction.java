package luan;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Arrays;


public final class LuanJavaFunction extends LuanFunction {
	private final JavaMethod method;
	private Object obj;
	private final RtnConverter rtnConverter;
	private final boolean takesLuaState;
	private final ArgConverter[] argConverters;
	private final Class<?> varArgCls;

	public LuanJavaFunction(Method method,Object obj) {
		this( JavaMethod.of(method), obj );
	}

	public LuanJavaFunction(Constructor constr,Object obj) {
		this( JavaMethod.of(constr), obj );
	}

	private LuanJavaFunction(JavaMethod method,Object obj) {
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
/*
	private LuanJavaFunction(LuanJavaFunction f) {
		this.method = f.method;
		this.rtnConverter = f.rtnConverter;
		this.takesLuaState = f.takesLuaState;
		this.argConverters = f.argConverters;
		this.varArgCls = f.varArgCls;
	}

	@Override public LuanJavaFunction shallowClone() {
		return obj==null ? this : new LuanJavaFunction(this);
	}

	@Override public void deepenClone(LuanJavaFunction clone,DeepCloner cloner) {
		clone.obj = cloner.get(obj);
	}
*/
	@Override public String toString() {
		return "java-function: " + method;
	}

	public Class<?>[] getParameterTypes() {
		return method.getParameterTypes();
	}

	@Override public Object call(LuanState luan,Object[] args) throws LuanException {
		try {
			args = fixArgs(luan,args);
			return doCall(luan,args);
		} catch(IllegalArgumentException e) {
			checkArgs(luan,args);
			throw e;
		}
	}

	public Object rawCall(LuanState luan,Object[] args) throws LuanException {
		args = fixArgs(luan,args);
		return doCall(luan,args);
	}

	private Object doCall(LuanState luan,Object[] args) throws LuanException {
		Object rtn;
		try {
			rtn = method.invoke(obj,args);
		} catch(IllegalAccessException e) {
			throw new RuntimeException("method = "+method,e);
		} catch(InvocationTargetException e) {
			Throwable cause = e.getCause();
			if( cause instanceof Error )
				throw (Error)cause;
			if( cause instanceof LuanException )
				throw (LuanException)cause;
			if( cause instanceof LuanExitException )
				throw (LuanExitException)cause;
			throw luan.exception(cause);
		} catch(InstantiationException e) {
			throw new RuntimeException(e);
		}
		return rtnConverter.convert(rtn);
	}

	private static final Map<Class,Class> primitiveMap = new HashMap<Class,Class>();
	static {
		primitiveMap.put(Boolean.TYPE,Boolean.class);
		primitiveMap.put(Character.TYPE,Character.class);
		primitiveMap.put(Byte.TYPE,Byte.class);
		primitiveMap.put(Short.TYPE,Short.class);
		primitiveMap.put(Integer.TYPE,Integer.class);
		primitiveMap.put(Long.TYPE,Long.class);
		primitiveMap.put(Float.TYPE,Float.class);
		primitiveMap.put(Double.TYPE,Double.class);
		primitiveMap.put(Void.TYPE,Void.class);
	}

	private void checkArgs(LuanState luan,Object[] args) throws LuanException {
		Class<?>[] a = getParameterTypes();
		int start = takesLuaState ? 1 : 0;
		for( int i=start; i<a.length; i++ ) {
			Class<?> paramType = a[i];
			Class<?> type = paramType;
			if( type.isPrimitive() )
				type = primitiveMap.get(type);
			Object arg = args[i];
			if( !type.isInstance(arg) ) {
				String expected = paramType.getSimpleName();
				if( i==a.length-1 && method.isVarArgs() )
					expected = paramType.getComponentType().getSimpleName()+"...";
				if( arg==null ) {
					if( paramType.isPrimitive() )
						throw luan.exception("bad argument #"+(i+1-start)+" ("+expected+" expected, got nil)");
				} else {
					String got = arg.getClass().getSimpleName();
					throw luan.exception("bad argument #"+(i+1-start)+" ("+expected+" expected, got "+got+")");
				}
			}
		}
	}

	private Object[] fixArgs(LuanState luan,Object[] args) {
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
				rtn[start++] = luan;
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
		public Object convert(Object obj);
	}

	private static final RtnConverter RTN_NOTHING = new RtnConverter() {
		@Override public Object[] convert(Object obj) {
			return NOTHING;
		}
	};

	private static final RtnConverter RTN_SAME = new RtnConverter() {
		@Override public Object convert(Object obj) {
			return obj;
		}
	};

	private static final RtnConverter RTN_ARRAY = new RtnConverter() {
		@Override public Object convert(Object obj) {
			if( obj == null )
				return null;
			Object[] a = new Object[Array.getLength(obj)];
			for( int i=0; i<a.length; i++ ) {
				a[i] = Array.get(obj,i);
			}
			return new LuanTableImpl(new ArrayList<Object>(Arrays.asList(a)));
		}
	};

	private static RtnConverter getRtnConverter(JavaMethod m) {
		Class<?> rtnType = m.getReturnType();
		if( rtnType == Void.TYPE )
			return RTN_NOTHING;
		if( !m.isLuan() && rtnType.isArray() && !rtnType.getComponentType().isPrimitive() ) {
//System.out.println("qqqqqq "+m);
			return RTN_ARRAY;
		}
		return RTN_SAME;
	}

	private static boolean isNumber(Class<?> rtnType) {
		return rtnType == Short.TYPE
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
		@Override public String toString() {
			return "ARG_SAME";
		}
	};

	private static final ArgConverter ARG_BOOLEAN = new ArgConverter() {
		public Object convert(Object obj) {
			return Luan.toBoolean(obj);
		}
		@Override public String toString() {
			return "ARG_BOOLEAN";
		}
	};

	private static final ArgConverter ARG_BOOLEAN_OBJ = new ArgConverter() {
		public Object convert(Object obj) {
			return obj==null ? null : Luan.toBoolean(obj);
		}
		@Override public String toString() {
			return "ARG_BOOLEAN_OBJ";
		}
	};

	private static final ArgConverter ARG_DOUBLE = new ArgConverter() {
		public Object convert(Object obj) {
			if( obj instanceof Double )
				return obj;
			if( obj instanceof Number ) {
				Number n = (Number)obj;
				return n.doubleValue();
			}
			if( obj instanceof String ) {
				String s = (String)obj;
				try {
					return Double.valueOf(s);
				} catch(NumberFormatException e) {}
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_DOUBLE";
		}
	};

	private static final ArgConverter ARG_FLOAT = new ArgConverter() {
		public Object convert(Object obj) {
			if( obj instanceof Float )
				return obj;
			if( obj instanceof Number ) {
				Number n = (Number)obj;
				return n.floatValue();
			}
			if( obj instanceof String ) {
				String s = (String)obj;
				try {
					return Float.valueOf(s);
				} catch(NumberFormatException e) {}
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_FLOAT";
		}
	};

	private static final ArgConverter ARG_LONG = new ArgConverter() {
		public Object convert(Object obj) {
			if( obj instanceof Long )
				return obj;
			if( obj instanceof Number ) {
				Number n = (Number)obj;
				long r = n.longValue();
				if( r==n.doubleValue() )
					return r;
			}
			else if( obj instanceof String ) {
				String s = (String)obj;
				try {
					return Long.valueOf(s);
				} catch(NumberFormatException e) {}
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_LONG";
		}
	};

	private static final ArgConverter ARG_INTEGER = new ArgConverter() {
		public Object convert(Object obj) {
			if( obj instanceof Integer )
				return obj;
			if( obj instanceof Number ) {
				Number n = (Number)obj;
				int r = n.intValue();
				if( r==n.doubleValue() )
					return r;
			}
			else if( obj instanceof String ) {
				String s = (String)obj;
				try {
					return Integer.valueOf(s);
				} catch(NumberFormatException e) {}
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_INTEGER";
		}
	};

	private static final ArgConverter ARG_SHORT = new ArgConverter() {
		public Object convert(Object obj) {
			if( obj instanceof Short )
				return obj;
			if( obj instanceof Number ) {
				Number n = (Number)obj;
				short r = n.shortValue();
				if( r==n.doubleValue() )
					return r;
			}
			else if( obj instanceof String ) {
				String s = (String)obj;
				try {
					return Short.valueOf(s);
				} catch(NumberFormatException e) {}
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_SHORT";
		}
	};

	private static final ArgConverter ARG_BYTE = new ArgConverter() {
		public Object convert(Object obj) {
			if( obj instanceof Byte )
				return obj;
			if( obj instanceof Number ) {
				Number n = (Number)obj;
				byte r = n.byteValue();
				if( r==n.doubleValue() )
					return r;
			}
			else if( obj instanceof String ) {
				String s = (String)obj;
				try {
					return Byte.valueOf(s);
				} catch(NumberFormatException e) {}
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_BYTE";
		}
	};

	private static final ArgConverter ARG_TABLE = new ArgConverter() {
		public Object convert(Object obj) {
			if( obj == null )
				return null;
			if( obj instanceof List ) {
				@SuppressWarnings("unchecked")
				List<Object> list = (List<Object>)obj;
				return new LuanTableImpl(list);
			}
			if( obj instanceof Map ) {
				@SuppressWarnings("unchecked")
				Map<Object,Object> map = (Map<Object,Object>)obj;
				return new LuanTableImpl(map);
			}
			if( obj instanceof Set ) {
				@SuppressWarnings("unchecked")
				Set<Object> set = (Set<Object>)obj;
				return new LuanTableImpl(set);
			}
			Class cls = obj.getClass();
			if( cls.isArray() && !cls.getComponentType().isPrimitive() ) {
				Object[] a = (Object[])obj;
				return new LuanTableImpl(Arrays.asList(a));
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_TABLE";
		}
	};

	private static final ArgConverter ARG_MAP = new ArgConverter() {
		public Object convert(Object obj) {
			if( obj instanceof LuanTableImpl ) {
				LuanTableImpl t = (LuanTableImpl)obj;
				return t.asMap();
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_MAP";
		}
	};

	private static final ArgConverter ARG_LIST = new ArgConverter() {
		public Object convert(Object obj) {
			if( obj instanceof LuanTableImpl ) {
				LuanTableImpl t = (LuanTableImpl)obj;
				if( t.isList() )
					return t.asList();
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_LIST";
		}
	};

	private static final ArgConverter ARG_SET = new ArgConverter() {
		public Object convert(Object obj) {
			if( obj instanceof LuanTableImpl ) {
				LuanTableImpl t = (LuanTableImpl)obj;
				if( t.isSet() )
					return t.asSet();
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_SET";
		}
	};

	private static class ArgArray implements ArgConverter {
		private final Object[] a;

		ArgArray(Class cls) {
			a = (Object[])Array.newInstance(cls.getComponentType(),0);
		}

		public Object convert(Object obj) {
			if( obj instanceof LuanTableImpl ) {
				LuanTableImpl t = (LuanTableImpl)obj;
				if( t.isList() ) {
					try {
						return t.asList().toArray(a);
					} catch(ArrayStoreException e) {}
				}
			}
			return obj;
		}
	}

	private static boolean takesLuaState(JavaMethod m) {
		Class<?>[] paramTypes = m.getParameterTypes();
		return paramTypes.length > 0 && paramTypes[0].equals(LuanState.class);
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
		if( cls == Boolean.TYPE )
			return ARG_BOOLEAN;
		if( cls.equals(Boolean.class) )
			return ARG_BOOLEAN_OBJ;
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
		if( cls.equals(LuanTable.class) )
			return ARG_TABLE;
		if( cls.equals(Map.class) )
			return ARG_MAP;
		if( cls.equals(List.class) )
			return ARG_LIST;
		if( cls.equals(Set.class) )
			return ARG_SET;
		if( cls.isArray() && !cls.getComponentType().isPrimitive() )
			return new ArgArray(cls);
		return ARG_SAME;
	}



	private static abstract class JavaMethod {
		abstract boolean isVarArgs();
		abstract Class<?>[] getParameterTypes();
		abstract Object invoke(Object obj,Object... args)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException;
		abstract Class<?> getReturnType();
		abstract boolean isLuan();
	
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
				@Override boolean isLuan() {
					return m.getAnnotation(LuanMethod.class) != null;
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
				@Override boolean isLuan() {
					return false;
				}
				@Override public String toString() {
					return c.toString();
				}
			};
		}
	
	}

}
