package luan.lib;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import luan.Lua;
import luan.LuaState;
import luan.LuaTable;
import luan.LuaNumber;
import luan.LuaFunction;
import luan.LuaJavaFunction;
import luan.LuaException;
import luan.interp.LuaCompiler;


public final class BasicLib {

	public static void register(LuaState lua) {
		LuaTable global = lua.global();
		global.put( "_G", global );
		add( global, "getmetatable", LuaState.class, Object.class );
		add( global, "ipairs", LuaTable.class );
		add( global, "load", LuaState.class, String.class );
		add( global, "loadfile", LuaState.class, String.class );
		add( global, "pairs", LuaTable.class );
		add( global, "print", LuaState.class, new Object[0].getClass() );
		add( global, "rawequal", Object.class, Object.class );
		add( global, "rawget", LuaTable.class, Object.class );
		add( global, "rawlen", Object.class );
		add( global, "rawset", LuaTable.class, Object.class, Object.class );
		add( global, "setmetatable", LuaTable.class, LuaTable.class );
		add( global, "tonumber", Object.class, Integer.class );
		add( global, "tostring", LuaState.class, Object.class );
		add( global, "type", Object.class );
		global.put( "_VERSION", Lua.version );
	}

	private static void add(LuaTable t,String method,Class<?>... parameterTypes) {
		try {
			t.put( method, new LuaJavaFunction(BasicLib.class.getMethod(method,parameterTypes),null) );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static void print(LuaState lua,Object... args) throws LuaException {
		for( int i=0; i<args.length; i++ ) {
			if( i > 0 )
				System.out.print('\t');
			System.out.print( lua.toString(args[i]) );
		}
		System.out.println();
	}

	public static String type(Object obj) {
		return Lua.type(obj);
	}

	public static LuaFunction load(LuaState lua,String ld) throws LuaException {
		return LuaCompiler.compile(lua,ld);
	}

	public static String readAll(Reader in)
		throws IOException
	{
		char[] a = new char[8192];
		StringBuilder buf = new StringBuilder();
		int n;
		while( (n=in.read(a)) != -1 ) {
			buf.append(a,0,n);
		}
		return buf.toString();
	}

	public static String read(File file)
		throws IOException
	{
		Reader in = new FileReader(file);
		String s = readAll(in);
		in.close();
		return s;
	}


	public static LuaFunction loadfile(LuaState lua,String fileName) throws LuaException,IOException {
		String src = fileName==null ? readAll(new InputStreamReader(System.in)) : read(new File(fileName));
		return load(lua,src);
	}

	public static Object[] dofile(LuaState lua,String fileName) throws LuaException,IOException {
		return loadfile(lua,fileName).call(lua);
	}

	private static class TableIter {
		private final Iterator<Map.Entry<Object,Object>> iter;

		TableIter(Iterator<Map.Entry<Object,Object>> iter) {
			this.iter = iter;
		}

		public Object[] next() {
			if( !iter.hasNext() )
				return LuaFunction.EMPTY_RTN;
			Map.Entry<Object,Object> entry = iter.next();
			return new Object[]{entry.getKey(),entry.getValue()};
		}
	}
	private static final Method nextTableIter;
	static {
		try {
			nextTableIter = TableIter.class.getMethod("next");
			nextTableIter.setAccessible(true);
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	static LuaFunction pairs(Iterator<Map.Entry<Object,Object>> iter) {
		TableIter ti = new TableIter(iter);
		return new LuaJavaFunction(nextTableIter,ti);
	}

	public static LuaFunction pairs(LuaTable t) {
		return pairs(t.iterator());
	}

	private static class ArrayIter {
		private final LuaTable t;
		private double i = 0.0;

		ArrayIter(LuaTable t) {
			this.t = t;
		}

		public Object[] next() {
			LuaNumber n = new LuaNumber(++i);
			Object val = t.get(n);
			return val==null ? LuaFunction.EMPTY_RTN : new Object[]{n,val};
		}
	}
	private static final Method nextArrayIter;
	static {
		try {
			nextArrayIter = ArrayIter.class.getMethod("next");
			nextArrayIter.setAccessible(true);
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static LuaFunction ipairs(LuaTable t) {
		ArrayIter ai = new ArrayIter(t);
		return new LuaJavaFunction(nextArrayIter,ai);
	}

	public static LuaTable getmetatable(LuaState lua,Object obj) {
		return lua.getMetatable(obj);
	}

	public static LuaTable setmetatable(LuaTable table,LuaTable metatable) {
		table.setMetatable(metatable);
		return table;
	}

	public static boolean rawequal(Object v1,Object v2) {
		return v1 == v2 || v1 != null && v1.equals(v2);
	}

	public static Object rawget(LuaTable table,Object index) {
		return table.get(index);
	}

	public static LuaTable rawset(LuaTable table,Object index,Object value) {
		table.put(index,value);
		return table;
	}

	public static int rawlen(Object v) throws LuaException {
		if( v instanceof String ) {
			String s = (String)v;
			return s.length();
		}
		if( v instanceof LuaTable ) {
			LuaTable t = (LuaTable)v;
			return t.length();
		}
		throw new LuaException( "bad argument #1 to 'rawlen' (table or string expected)" );
	}

	public static LuaNumber tonumber(Object e,Integer base) {
		return Lua.toNumber(e,base);
	}

	public static String tostring(LuaState lua,Object v) throws LuaException {
		return lua.toString(v);
	}
}
