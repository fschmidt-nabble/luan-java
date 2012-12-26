package luan.lib;

import java.lang.reflect.Array;
import java.lang.reflect.Member;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import luan.LuaNumber;
import luan.LuaState;
import luan.LuaTable;
import luan.MetatableGetter;
import luan.LuaException;
import luan.LuaFunction;
import luan.LuaJavaFunction;
import luan.LuaElement;


public final class JavaLib {

	public static void register(LuaState lua) {
		lua.addMetatableGetter(mg);
		LuaTable module = new LuaTable();
		LuaTable global = lua.global();
		global.put("java",module);
		try {
			global.put( "import", new LuaJavaFunction(JavaLib.class.getMethod("importClass",LuaState.class,String.class),null) );
			module.put( "class", new LuaJavaFunction(JavaLib.class.getMethod("getClass",LuaState.class,String.class),null) );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private static final LuaTable mt = new LuaTable();
	static {
		add( mt, "__index", LuaState.class, Object.class, Object.class );
	}

	private static void add(LuaTable t,String method,Class<?>... parameterTypes) {
		try {
			t.put( method, new LuaJavaFunction(JavaLib.class.getMethod(method,parameterTypes),null) );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private static final MetatableGetter mg = new MetatableGetter() {
		public LuaTable getMetatable(Object obj) {
			if( obj==null )
				return null;
			return mt;
		}
	};

	public static Object __index(LuaState lua,Object obj,Object key) throws LuaException {
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
							return new LuaJavaFunction(constructors[0],null);
						} else {
							List<LuaJavaFunction> fns = new ArrayList<LuaJavaFunction>();
							for( Constructor constructor : constructors ) {
								fns.add(new LuaJavaFunction(constructor,null));
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
			throw new LuaException(lua,LuaElement.JAVA,"invalid index for java class: "+key);
		}
		Class cls = obj.getClass();
		if( cls.isArray() ) {
			if( "length".equals(key) ) {
				return Array.getLength(obj);
			}
			if( key instanceof LuaNumber ) {
				LuaNumber n = (LuaNumber)key;
				double d = n.value();
				int i = (int)d;
				if( d==i ) {
					return Array.get(obj,i);
				}
			}
			throw new LuaException(lua,LuaElement.JAVA,"invalid index for java array: "+key);
		}
		if( key instanceof String ) {
			String name = (String)key;
			if( "instanceof".equals(name) ) {
				return new LuaJavaFunction(instanceOf,new InstanceOf(obj));
			} else {
				List<Member> members = getMembers(cls,name);
				if( !members.isEmpty() ) {
					return member(obj,members);
				}
			}
		}
		throw new LuaException(lua,LuaElement.JAVA,"invalid index for java object: "+key);
	}

	private static Object member(Object obj,List<Member> members) throws LuaException {
		try {
			if( members.size()==1 ) {
				Member member = members.get(0);
				if( member instanceof Field ) {
					Field field = (Field)member;
					Object value = field.get(obj);
					if( value instanceof Number ) {
						Number n = (Number)value;
						value = LuaNumber.of(n);
					}
					return value;
				} else {
					Method method = (Method)member;
					return new LuaJavaFunction(method,obj);
				}
			} else {
				List<LuaJavaFunction> fns = new ArrayList<LuaJavaFunction>();
				for( Member member : members ) {
					Method method = (Method)member;
					fns.add(new LuaJavaFunction(method,obj));
				}
				return new AmbiguousJavaFunction(fns);
			}
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private static Map<Class,Map<String,List<Member>>> memberMap = new HashMap<Class,Map<String,List<Member>>>();

	private static synchronized List<Member> getMembers(Class cls,String name) {
		Map<String,List<Member>> clsMap = memberMap.get(cls);
		if( clsMap == null ) {
			clsMap = new HashMap<String,List<Member>>();
			for( Field field : cls.getFields() ) {
				String s = field.getName();
				List<Member> list = clsMap.get(s);
				if( list == null ) {
					list = new ArrayList<Member>();
					clsMap.put(s,list);
				}
				list.add(field);
			}
			for( Method method : cls.getMethods() ) {
				String s = method.getName();
				List<Member> list = clsMap.get(s);
				if( list == null ) {
					list = new ArrayList<Member>();
					clsMap.put(s,list);
				}
				list.add(method);
			}
			memberMap.put(cls,clsMap);
		}
		return clsMap.get(name);
	}

	private static synchronized List<Member> getStaticMembers(Class cls,String name) {
		List<Member> staticMembers = new ArrayList<Member>();
		for( Member m : getMembers(cls,name) ) {
			if( Modifier.isStatic(m.getModifiers()) )
				staticMembers.add(m);
		}
		return staticMembers;
	}

	static class Static {
		final Class cls;

		Static(Class cls) {
			this.cls = cls;
		}
	}

	public static Static getClass(LuaState lua,String name) throws LuaException {
		try {
			return new Static( Class.forName(name) );
		} catch(ClassNotFoundException e) {
			throw new LuaException(lua,LuaElement.JAVA,e);
		}
	}

	public static void importClass(LuaState lua,String name) throws LuaException {
		lua.global().put( name.substring(name.lastIndexOf('.')+1), getClass(lua,name) );
	}

	static class AmbiguousJavaFunction extends LuaFunction {
		private final Map<Integer,List<LuaJavaFunction>> fnMap = new HashMap<Integer,List<LuaJavaFunction>>();

		AmbiguousJavaFunction(List<LuaJavaFunction> fns) {
			for( LuaJavaFunction fn : fns ) {
				Integer n = fn.getParameterTypes().length;
				List<LuaJavaFunction> list = fnMap.get(n);
				if( list==null ) {
					list = new ArrayList<LuaJavaFunction>();
					fnMap.put(n,list);
				}
				list.add(fn);
			}
		}

		@Override public Object[] call(LuaState lua,Object[] args) throws LuaException {
			for( LuaJavaFunction fn : fnMap.get(args.length) ) {
				try {
					return fn.call(lua,args);
				} catch(IllegalArgumentException e) {}
			}
			throw new LuaException(lua,LuaElement.JAVA,"no method matched args");
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

}
