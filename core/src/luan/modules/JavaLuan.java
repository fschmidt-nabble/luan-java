package luan.modules;

import java.lang.reflect.Array;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Arrays;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanException;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanElement;


public final class JavaLuan {

	public static void java(LuanState luan) throws LuanException {
		check(luan,luan.currentSource().name);
		luan.currentEnvironment().setJava();
	}

	private static void checkJava(LuanState luan) throws LuanException {
		if( !luan.currentEnvironment().hasJava() )
			throw luan.exception("Java isn't allowed");
	}

	private static boolean isLoaded(LuanState luan) {
//		return PackageLuan.loaded(luan).get("luan:Java") != null;
		return true;
	}

	public static Object __index(LuanState luan,Object obj,Object key) throws LuanException {
		if( !luan.currentEnvironment().hasJava() )
			return null;
		if( obj instanceof Static ) {
			if( key instanceof String ) {
				String name = (String)key;
				Static st = (Static)obj;
				Class cls = st.cls;
				if( "class".equals(name) ) {
					return cls;
				} else if( "new".equals(name) ) {
					Constructor<?>[] constructors = cls.getConstructors();
					if( constructors.length > 0 ) {
						if( constructors.length==1 ) {
							return new LuanJavaFunction(constructors[0],null);
						} else {
							List<LuanJavaFunction> fns = new ArrayList<LuanJavaFunction>();
							for( Constructor constructor : constructors ) {
								fns.add(new LuanJavaFunction(constructor,null));
							}
							return new AmbiguousJavaFunction(fns);
						}
					}
/*
				} else if( "assert".equals(name) ) {
					return new LuanJavaFunction(assertClass,new AssertClass(cls));
*/
				} else if( "luan_proxy".equals(name) ) {
					return new LuanJavaFunction(luan_proxyMethod,st);
				} else {
					List<Member> members = getStaticMembers(cls,name);
					if( !members.isEmpty() ) {
						return member(null,members);
					}
				}
			}
			throw luan.exception("invalid member '"+key+"' for: "+obj);
		}
		Class cls = obj.getClass();
		if( cls.isArray() ) {
			if( "length".equals(key) ) {
				return Array.getLength(obj);
			}
			Integer i = Luan.asInteger(key);
			if( i != null ) {
				return Array.get(obj,i);
			}
			throw luan.exception("invalid member '"+key+"' for java array: "+obj);
		}
		if( key instanceof String ) {
			String name = (String)key;
			if( "instanceof".equals(name) ) {
				return new LuanJavaFunction(instanceOf,new InstanceOf(obj));
			} else {
				List<Member> members = getMembers(cls,name);
				if( !members.isEmpty() ) {
					return member(obj,members);
				}
			}
		}
//		throw luan.exception("invalid member '"+key+"' for java object: "+obj);
//System.out.println("invalid member '"+key+"' for java object: "+obj);
		return null;
	}

	private static Object member(Object obj,List<Member> members) throws LuanException {
		try {
			if( members.size()==1 ) {
				Member member = members.get(0);
				if( member instanceof Static ) {
					return member;
				} else if( member instanceof Field ) {
					Field field = (Field)member;
					Object rtn = field.get(obj);
					return rtn instanceof Object[] ? Arrays.asList((Object[])rtn) : rtn;
				} else {
					Method method = (Method)member;
					return new LuanJavaFunction(method,obj);
				}
			} else {
				List<LuanJavaFunction> fns = new ArrayList<LuanJavaFunction>();
				for( Member member : members ) {
					Method method = (Method)member;
					fns.add(new LuanJavaFunction(method,obj));
				}
				return new AmbiguousJavaFunction(fns);
			}
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static void __newindex(LuanState luan,Object obj,Object key,Object value) throws LuanException {
		checkJava(luan);
		if( obj instanceof Static ) {
			if( key instanceof String ) {
				String name = (String)key;
				Static st = (Static)obj;
				Class cls = st.cls;
				List<Member> members = getStaticMembers(cls,name);
				if( !members.isEmpty() ) {
					if( members.size() != 1 )
						throw new RuntimeException("not field '"+name+"' of "+obj);
					setMember(obj,members,value);
					return;
				}
			}
			throw luan.exception("invalid member '"+key+"' for: "+obj);
		}
		Class cls = obj.getClass();
		if( cls.isArray() ) {
			Integer i = Luan.asInteger(key);
			if( i != null ) {
				Array.set(obj,i,value);
				return;
			}
			throw luan.exception("invalid member '"+key+"' for java array: "+obj);
		}
		if( key instanceof String ) {
			String name = (String)key;
			List<Member> members = getMembers(cls,name);
			if( !members.isEmpty() ) {
				if( members.size() != 1 )
					throw new RuntimeException("not field '"+name+"' of "+obj);
				setMember(obj,members,value);
				return;
			}
		}
		throw luan.exception("invalid member '"+key+"' for java object: "+obj);
	}

	private static void setMember(Object obj,List<Member> members,Object value) {
		Field field = (Field)members.get(0);
		try {
			try {
				field.set(obj,value);
			} catch(IllegalArgumentException e) {
				Class cls = field.getType();
				if( value instanceof Number ) {
					Number n = (Number)value;
					if( cls.equals(Integer.TYPE) || cls.equals(Integer.class) ) {
						int r = n.intValue();
						if( r==n.doubleValue() ) {
							field.setInt(obj,r);
							return;
						}
					}
				}
				throw e;
			}
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean privateAccess = false;
	private static Map<Class,Map<String,List<Member>>> memberMap = new HashMap<Class,Map<String,List<Member>>>();

	private static synchronized List<Member> getMembers(Class cls,String name) {
		Map<String,List<Member>> clsMap = memberMap.get(cls);
		if( clsMap == null ) {
			clsMap = new HashMap<String,List<Member>>();
			for( Class c : cls.getClasses() ) {
				String s = c.getSimpleName();
				List<Member> list = new ArrayList<Member>();
				clsMap.put(s,list);
				list.add(new Static(c));
			}
			for( Field field : cls.getFields() ) {
				String s = field.getName();
				try {
					if( !cls.getField(s).equals(field) )
						continue;  // not accessible
				} catch(NoSuchFieldException e) {
					throw new RuntimeException(e);
				}
				List<Member> list = new ArrayList<Member>();
				clsMap.put(s,list);
				list.add(field);
			}
			for( Method method : cls.getMethods() ) {
				String s = method.getName();
				List<Member> list = clsMap.get(s);
				if( list == null || !(list.get(0) instanceof Method) ) {
					list = new ArrayList<Member>();
					clsMap.put(s,list);
				}
				list.add(method);
			}
			if( privateAccess ) {
				for( Method method : cls.getDeclaredMethods() ) {
					String s = method.getName();
					List<Member> list = clsMap.get(s);
					if( list == null ) {
						list = new ArrayList<Member>();
						clsMap.put(s,list);
					} else if( !(list.get(0) instanceof Method) )
						continue;
					if( !list.contains(method) ) {
						list.add(method);
					}
				}
				for( Field field : cls.getDeclaredFields() ) {
					String s = field.getName();
					List<Member> list = clsMap.get(s);
					if( list == null ) {
						list = new ArrayList<Member>();
						clsMap.put(s,list);
						list.add(field);
					}
				}
			}
			for( List<Member> members : clsMap.values() ) {
				for( Member m : members ) {
					if( m instanceof AccessibleObject )
						((AccessibleObject)m).setAccessible(true);
				}
			}
			memberMap.put(cls,clsMap);
		}
		List<Member> rtn = clsMap.get(name);
		if( rtn==null )
			rtn = Collections.emptyList();
		return rtn;
	}

	private static synchronized List<Member> getStaticMembers(Class cls,String name) {
		List<Member> staticMembers = new ArrayList<Member>();
		for( Member m : getMembers(cls,name) ) {
			if( Modifier.isStatic(m.getModifiers()) )
				staticMembers.add(m);
		}
		return staticMembers;
	}

	static final class Static implements Member {
		final Class cls;

		Static(Class cls) {
			this.cls = cls;
		}

		@Override public String toString() {
			return cls.toString();
		}

		@Override public Class<?> getDeclaringClass() {
			return cls.getDeclaringClass();
		}

		@Override public String getName() {
			return cls.getName();
		}

		@Override public int getModifiers() {
			return cls.getModifiers();
		}

		@Override public boolean isSynthetic() {
			return cls.isSynthetic();
		}

		public Object luan_proxy(final LuanState luan,final LuanTable t,final Object base) throws LuanException {
			return Proxy.newProxyInstance(
				cls.getClassLoader(),
				new Class[]{cls},
				new InvocationHandler() {
					public Object invoke(Object proxy,Method method, Object[] args)
						throws Throwable
					{
						if( args==null )
							args = new Object[0];
						String name = method.getName();
						Object fnObj = t.get(name);
						if( fnObj==null && base!=null )
							return method.invoke(base,args);
						LuanFunction fn = luan.checkFunction(fnObj);
						return Luan.first(luan.call(fn,name,args));
					}
				}
			);
		}
	}
	private static final Method luan_proxyMethod;
	static {
		try {
			luan_proxyMethod = Static.class.getMethod("luan_proxy",LuanState.class,LuanTable.class,Object.class);
			luan_proxyMethod.setAccessible(true);
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static Static load(LuanState luan,String name) throws LuanException {
		checkJava(luan);
		Class cls;
		try {
			cls = Class.forName(name);
		} catch(ClassNotFoundException e) {
			try {
				cls = Thread.currentThread().getContextClassLoader().loadClass(name);
			} catch(ClassNotFoundException e2) {
				return null;
			}
		}
		return new Static(cls);
	}

	private static class AmbiguousJavaFunction extends LuanFunction {
		private final Map<Integer,List<LuanJavaFunction>> fnMap = new HashMap<Integer,List<LuanJavaFunction>>();

		AmbiguousJavaFunction(List<LuanJavaFunction> fns) {
			for( LuanJavaFunction fn : fns ) {
				Integer n = fn.getParameterTypes().length;
				List<LuanJavaFunction> list = fnMap.get(n);
				if( list==null ) {
					list = new ArrayList<LuanJavaFunction>();
					fnMap.put(n,list);
				}
				list.add(fn);
			}
		}

		@Override public Object call(LuanState luan,Object[] args) throws LuanException {
			for( LuanJavaFunction fn : fnMap.get(args.length) ) {
				try {
					return fn.rawCall(luan,args);
				} catch(IllegalArgumentException e) {}
			}
			throw luan.exception("no method matched args");
		}
	}

	private static class InstanceOf {
		private final Object obj;

		InstanceOf(Object obj) {
			this.obj = obj;
		}

		public boolean instanceOf(Static st) {
			return st.cls.isInstance(obj);
		}
	}
	private static final Method instanceOf;
	static {
		try {
			instanceOf = InstanceOf.class.getMethod("instanceOf",Static.class);
			instanceOf.setAccessible(true);
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

/*
	private static class AssertClass {
		private final Class cls;

		AssertClass(Class cls) {
			this.cls = cls;
		}

		public Object assertClass(LuanState luan,Object v) throws LuanException {
			if( !cls.isInstance(v) ) {
				String got = v.getClass().getSimpleName();
				String expected = cls.getSimpleName();
				throw luan.exception("bad argument #1 ("+expected+" expected, got "+got+")");
			}
			return v;
		}
	}
	private static final Method assertClass;
	static {
		try {
			assertClass = AssertClass.class.getMethod("assertClass",LuanState.class,Object.class);
			assertClass.setAccessible(true);
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}


	public static Object proxy(final LuanState luan,Static st,final LuanTable t,final Object base) throws LuanException {
		return Proxy.newProxyInstance(
			st.cls.getClassLoader(),
			new Class[]{st.cls},
			new InvocationHandler() {
				public Object invoke(Object proxy,Method method, Object[] args)
					throws Throwable
				{
					if( args==null )
						args = new Object[0];
					String name = method.getName();
					Object fnObj = t.get(name);
					if( fnObj==null && base!=null )
						return method.invoke(base,args);
					LuanFunction fn = luan.checkFunction(fnObj);
					return Luan.first(luan.call(fn,name,args));
				}
			}
		);
	}
*/



	// security

	public interface Security {
		public void check(LuanState luan,String name) throws LuanException;
	}

	private static String SECURITY_KEY = "Java.Security";

	private static void check(LuanState luan,String name) throws LuanException {
		Security s = (Security)luan.registry().get(SECURITY_KEY);
		if( s!=null )
			s.check(luan,name);
	}

	public static void setSecurity(LuanState luan,Security s) {
		luan.registry().put(SECURITY_KEY,s);
	}

}
