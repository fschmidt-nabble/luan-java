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
import luan.LuaSource;
import luan.LuaElement;
import luan.interp.LuaCompiler;


public final class BasicLib {

	public static void register(LuaState lua) {
		LuaTable global = lua.global();
		global.put( "_G", global );
		add( global, "do_file", LuaState.class, String.class );
		add( global, "error", LuaState.class, Object.class );
		add( global, "get_metatable", LuaState.class, Object.class );
		add( global, "ipairs", LuaTable.class );
		add( global, "load", LuaState.class, String.class, String.class );
		add( global, "load_file", LuaState.class, String.class );
		add( global, "pairs", LuaTable.class );
		add( global, "print", LuaState.class, new Object[0].getClass() );
		add( global, "raw_equal", Object.class, Object.class );
		add( global, "raw_get", LuaTable.class, Object.class );
		add( global, "raw_len", LuaState.class, Object.class );
		add( global, "raw_set", LuaTable.class, Object.class, Object.class );
		add( global, "set_metatable", LuaTable.class, LuaTable.class );
		add( global, "to_number", Object.class, Integer.class );
		add( global, "to_string", LuaState.class, Object.class );
		add( global, "type", Object.class );
		global.put( "_VERSION", Lua.version );

		add( global, "make_standard", LuaState.class );
	}

	private static void add(LuaTable t,String method,Class<?>... parameterTypes) {
		try {
			t.put( method, new LuaJavaFunction(BasicLib.class.getMethod(method,parameterTypes),null) );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static void make_standard(LuaState lua) {
		LuaTable global = lua.global();
		global.put( "dofile", global.get("do_file") );
		global.put( "getmetatable", global.get("get_metatable") );
		global.put( "loadfile", global.get("load_file") );
		global.put( "rawequal", global.get("raw_equal") );
		global.put( "rawget", global.get("raw_get") );
		global.put( "rawlen", global.get("raw_len") );
		global.put( "rawset", global.get("raw_set") );
		global.put( "setmetatable", global.get("set_metatable") );
		global.put( "tonumber", global.get("to_number") );
		global.put( "tostring", global.get("to_string") );
	}

	public static void print(LuaState lua,Object... args) throws LuaException {
		for( int i=0; i<args.length; i++ ) {
			if( i > 0 )
				System.out.print('\t');
			System.out.print( lua.toString(LuaElement.JAVA,args[i]) );
		}
		System.out.println();
	}

	public static String type(Object obj) {
		return Lua.type(obj);
	}

	public static LuaFunction load(LuaState lua,String text,String sourceName) throws LuaException {
		return LuaCompiler.compile(lua,new LuaSource(sourceName,text));
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


	public static LuaFunction load_file(LuaState lua,String fileName) throws LuaException {
		try {
			String src = fileName==null ? readAll(new InputStreamReader(System.in)) : read(new File(fileName));
			return load(lua,src,fileName);
		} catch(IOException e) {
			throw new LuaException(lua,LuaElement.JAVA,e);
		}
	}

	public static Object[] do_file(LuaState lua,String fileName) throws LuaException {
		LuaFunction fn = load_file(lua,fileName);
		return lua.call(fn,LuaElement.JAVA,null);
	}

	private static LuaFunction pairs(final Iterator<Map.Entry<Object,Object>> iter) {
		return new LuaFunction() {
			public Object[] call(LuaState lua,Object[] args) {
				if( !iter.hasNext() )
					return LuaFunction.EMPTY_RTN;
				Map.Entry<Object,Object> entry = iter.next();
				return new Object[]{entry.getKey(),entry.getValue()};
			}
		};
	}

	public static LuaFunction pairs(LuaTable t) {
		return pairs( t.iterator() );
	}

	public static LuaFunction ipairs(LuaTable t) {
		return pairs( t.listIterator() );
	}

	public static LuaTable get_metatable(LuaState lua,Object obj) {
		return lua.getMetatable(obj);
	}

	public static LuaTable set_metatable(LuaTable table,LuaTable metatable) {
		table.setMetatable(metatable);
		return table;
	}

	public static boolean raw_equal(Object v1,Object v2) {
		return v1 == v2 || v1 != null && v1.equals(v2);
	}

	public static Object raw_get(LuaTable table,Object index) {
		return table.get(index);
	}

	public static LuaTable raw_set(LuaTable table,Object index,Object value) {
		table.put(index,value);
		return table;
	}

	public static int raw_len(LuaState lua,Object v) throws LuaException {
		if( v instanceof String ) {
			String s = (String)v;
			return s.length();
		}
		if( v instanceof LuaTable ) {
			LuaTable t = (LuaTable)v;
			return t.length();
		}
		throw new LuaException( lua, LuaElement.JAVA, "bad argument #1 to 'raw_len' (table or string expected)" );
	}

	public static LuaNumber to_number(Object e,Integer base) {
		return Lua.toNumber(e,base);
	}

	public static String to_string(LuaState lua,Object v) throws LuaException {
		return lua.toString(LuaElement.JAVA,v);
	}

	public static void error(LuaState lua,Object msg) throws LuaException {
		throw new LuaException(lua,LuaElement.JAVA,msg);
	}

}
