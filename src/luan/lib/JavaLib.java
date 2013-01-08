package luan.lib;

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
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.MetatableGetter;
import luan.LuanException;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanElement;


public final class JavaLib {

	public static void register(LuanState luan) {
		luan.addMetatableGetter(mg);
		LuanTable module = new LuanTable();
		LuanTable global = luan.global();
		global.put("java",module);
		try {
			global.put( "import", new LuanJavaFunction(JavaLib.class.getMethod("importClass",LuanState.class,String.class),null) );
			module.put( "class", new LuanJavaFunction(JavaLib.class.getMethod("getClass",LuanState.class,String.class),null) );
			add( module, "proxy", LuanState.class, Static.class, LuanTable.class );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private static final LuanTable mt = new LuanTable();
	static {
		add( mt, "__index", LuanState.class, Object.class, Object.class );
		add( mt, "__newindex", LuanState.class, Object.class, Object.class, Object.class );
	}

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) {
		try {
			t.put( method, new LuanJavaFunction(JavaLib.class.getMethod(method,parameterTypes),null) );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private static final MetatableGetter mg = new MetatableGetter() {
		public LuanTable getMetatable(Object obj) {
			if( obj==null )
				return null;
			return mt;
		}
	};

	public static Object __index(LuanState luan,Object obj,Object key) throws LuanException {
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
				} else {
					List<Member> members = getStaticMembers(cls,name);
					if( !members.isEmpty() ) {
						return member(null,members);
					}
				}
			}
			throw new LuanException(luan,LuanElement.JAVA,"invalid member '"+key+"' for: "+obj);
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
			throw new LuanException(luan,LuanElement.JAVA,"invalid member '"+key+"' for java array: "+obj);
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
		throw new LuanException(luan,LuanElement.JAVA,"invalid member '"+key+"' for java object: "+obj);
	}

	private static Object member(Object obj,List<Member> members) throws LuanException {
		try {
			for( Member m : members ) {
				if( m instanceof AccessibleObject )
					((AccessibleObject)m).setAccessible(true);
			}
			if( members.size()==1 ) {
				Member member = members.get(0);
				if( member instanceof Static ) {
					return member;
				} else if( member instanceof Field ) {
					Field field = (Field)member;
					return field.get(obj);
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
			throw new LuanException(luan,LuanElement.JAVA,"invalid member '"+key+"' for: "+obj);
		}
		Class cls = obj.getClass();
		if( cls.isArray() ) {
			Integer i = Luan.asInteger(key);
			if( i != null ) {
				Array.set(obj,i,value);
				return;
			}
			throw new LuanException(luan,LuanElement.JAVA,"invalid member '"+key+"' for java array: "+obj);
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
		throw new LuanException(luan,LuanElement.JAVA,"invalid member '"+key+"' for java object: "+obj);
	}

	private static void setMember(Object obj,List<Member> members,Object value) {
		Field field = (Field)members.get(0);
		try {
			field.set(obj,value);
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private static Map<Class,Map<String,List<Member>>> memberMap = new HashMap<Class,Map<String,List<Member>>>();

	private static synchronized List<Member> getMembers(Class cls,String name) {
		Map<String,List<Member>> clsMap = memberMap.get(cls);
		if( clsMap == null ) {
			clsMap = new HashMap<String,List<Member>>();
			for( Method method : cls.getMethods() ) {
				String s = method.getName();
				List<Member> list = clsMap.get(s);
				if( list == null ) {
					list = new ArrayList<Member>();
					clsMap.put(s,list);
				}
				list.add(method);
			}
			for( Class c : cls.getClasses() ) {
				String s = c.getSimpleName();
				List<Member> list = clsMap.get(s);
				if( list == null ) {
					list = new ArrayList<Member>();
					clsMap.put(s,list);
				}
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
				List<Member> list = clsMap.get(s);
				if( list != null )
					throw new RuntimeException("can't add field '"+s+"' to "+cls+" because these are already defined: "+list);
				list = new ArrayList<Member>();
				clsMap.put(s,list);
				list.add(field);
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
	}

	public static Static getClass(LuanState luan,String name) throws LuanException {
		Class cls;
		try {
			cls = Class.forName(name);
		} catch(ClassNotFoundException e) {
			try {
				cls = Thread.currentThread().getContextClassLoader().loadClass(name);
			} catch(ClassNotFoundException e2) {
				throw new LuanException(luan,LuanElement.JAVA,e);
			}
		}
		return new Static(cls);
	}

	public static void importClass(LuanState luan,String name) throws LuanException {
		luan.global().put( name.substring(name.lastIndexOf('.')+1), getClass(luan,name) );
	}

	static class AmbiguousJavaFunction extends LuanFunction {
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

		@Override public Object[] call(LuanState luan,Object[] args) throws LuanException {
			for( LuanJavaFunction fn : fnMap.get(args.length) ) {
				try {
					return fn.rawCall(luan,args);
				} catch(IllegalArgumentException e) {}
			}
			throw new LuanException(luan,LuanElement.JAVA,"no method matched args");
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


	public static Object proxy(final LuanState luan,Static st,final LuanTable t) throws LuanException {
		return Proxy.newProxyInstance(
			st.cls.getClassLoader(),
			new Class[]{st.cls},
			new InvocationHandler() {
				public Object invoke(Object proxy,Method method, Object[] args)
					throws Throwable
				{
					String name = method.getName();
					LuanFunction fn = luan.checkFunction(LuanElement.JAVA,t.get(name));
					return Luan.first(luan.call(fn,LuanElement.JAVA,name,args));
				}
			}
		);
	}
}
