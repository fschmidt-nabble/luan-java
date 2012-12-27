package luan.lib;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.Arrays;
import luan.Lua;
import luan.LuaState;
import luan.LuaTable;
import luan.LuaNumber;
import luan.LuaFunction;
import luan.LuaJavaFunction;
import luan.LuaElement;
import luan.LuaException;


public final class TableLib {

	public static void register(LuaState lua) {
		LuaTable module = new LuaTable();
		LuaTable global = lua.global();
		global.put("table",module);
		try {
			add( module, "concat", LuaState.class, LuaTable.class, String.class, Integer.class, Integer.class );
			add( module, "insert", LuaState.class, LuaTable.class, Integer.TYPE, Object.class );
			add( module, "pack", new Object[0].getClass() );
			add( module, "remove", LuaState.class, LuaTable.class, Integer.TYPE );
			add( module, "sort", LuaState.class, LuaTable.class, LuaFunction.class );
			add( module, "sub_list", LuaTable.class, Integer.TYPE, Integer.TYPE );
			add( module, "unpack", LuaTable.class );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private static void add(LuaTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuaJavaFunction(TableLib.class.getMethod(method,parameterTypes),null) );
	}

	public static String concat(LuaState lua,LuaTable list,String sep,Integer i,Integer j) throws LuaException {
		int first = i==null ? 1 : i;
		int last = j==null ? list.length() : j;
		StringBuilder buf = new StringBuilder();
		for( int k=first; k<=last; k++ ) {
			Object val = list.get(LuaNumber.of(k));
			if( val==null )
				break;
			if( sep!=null && k > first )
				buf.append(sep);
			String s = Lua.asString(val);
			if( s==null )
				throw new LuaException( lua, LuaElement.JAVA, "invalid value ("+Lua.type(val)+") at index "+k+" in table for 'concat'" );
			buf.append(val);
		}
		return buf.toString();
	}

	public static void insert(LuaState lua,LuaTable list,int pos,Object value) throws LuaException {
		try {
			list.insert(pos,value);
		} catch(IndexOutOfBoundsException e) {
			throw new LuaException( lua, LuaElement.JAVA, e);
		}
	}

	public static Object remove(LuaState lua,LuaTable list,int pos) throws LuaException {
		try {
			return list.remove(pos);
		} catch(IndexOutOfBoundsException e) {
			throw new LuaException( lua, LuaElement.JAVA, e);
		}
	}

	private static interface LessThan {
		public boolean isLessThan(Object o1,Object o2);
	}

	public static void sort(final LuaState lua,LuaTable list,final LuaFunction comp) throws LuaException {
		final LessThan lt;
		if( comp==null ) {
			lt = new LessThan() {
				public boolean isLessThan(Object o1,Object o2) {
					try {
						return lua.isLessThan(LuaElement.JAVA,o1,o2);
					} catch(LuaException e) {
						throw new LuaRuntimeException(e);
					}
				}
			};
		} else {
			lt = new LessThan() {
				public boolean isLessThan(Object o1,Object o2) {
					try {
						return Lua.toBoolean(Lua.first(lua.call(comp,LuaElement.JAVA,"comp-arg",o1,o2)));
					} catch(LuaException e) {
						throw new LuaRuntimeException(e);
					}
				}
			};
		}
		try {
			list.sort( new Comparator<Object>() {
				public int compare(Object o1,Object o2) {
					return lt.isLessThan(o1,o2) ? -1 : lt.isLessThan(o2,o1) ? 1 : 0;
				}
			} );
		} catch(LuaRuntimeException e) {
			throw (LuaException)e.getCause();
		}
	}

	public static LuaTable pack(Object[] args) {
		return new LuaTable(new ArrayList<Object>(Arrays.asList(args)));
	}

	public static Object[] unpack(LuaTable list) {
		return list.listToArray();
	}

	public static LuaTable sub_list(LuaTable list,int from,int to) {
		return list.subList(from,to);
	}

}
