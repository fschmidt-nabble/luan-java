package luan.lib;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanException;
import luan.LuanSource;
import luan.LuanElement;
import luan.interp.LuanCompiler;


public final class BasicLib {

	public static void register(LuanState lua) {
		LuanTable global = lua.global();
		global.put( "_G", global );
		add( global, "do_file", LuanState.class, String.class );
		add( global, "error", LuanState.class, Object.class );
		add( global, "get_metatable", LuanState.class, Object.class );
		add( global, "ipairs", LuanTable.class );
		add( global, "load", LuanState.class, String.class, String.class );
		add( global, "load_file", LuanState.class, String.class );
		add( global, "pairs", LuanTable.class );
		add( global, "print", LuanState.class, new Object[0].getClass() );
		add( global, "raw_equal", Object.class, Object.class );
		add( global, "raw_get", LuanTable.class, Object.class );
		add( global, "raw_len", LuanState.class, Object.class );
		add( global, "raw_set", LuanTable.class, Object.class, Object.class );
		add( global, "set_metatable", LuanTable.class, LuanTable.class );
		add( global, "to_number", Object.class, Integer.class );
		add( global, "to_string", LuanState.class, Object.class );
		add( global, "type", Object.class );
		global.put( "_VERSION", Luan.version );

		add( global, "make_standard", LuanState.class );
	}

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) {
		try {
			t.put( method, new LuanJavaFunction(BasicLib.class.getMethod(method,parameterTypes),null) );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static void make_standard(LuanState lua) {
		LuanTable global = lua.global();
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

	public static void print(LuanState lua,Object... args) throws LuanException {
		for( int i=0; i<args.length; i++ ) {
			if( i > 0 )
				System.out.print('\t');
			System.out.print( lua.toString(LuanElement.JAVA,args[i]) );
		}
		System.out.println();
	}

	public static String type(Object obj) {
		return Luan.type(obj);
	}

	public static LuanFunction load(LuanState lua,String text,String sourceName) throws LuanException {
		return LuanCompiler.compile(lua,new LuanSource(sourceName,text));
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


	public static LuanFunction load_file(LuanState lua,String fileName) throws LuanException {
		try {
			String src = fileName==null ? readAll(new InputStreamReader(System.in)) : read(new File(fileName));
			return load(lua,src,fileName);
		} catch(IOException e) {
			throw new LuanException(lua,LuanElement.JAVA,e);
		}
	}

	public static Object[] do_file(LuanState lua,String fileName) throws LuanException {
		LuanFunction fn = load_file(lua,fileName);
		return lua.call(fn,LuanElement.JAVA,null);
	}

	private static LuanFunction pairs(final Iterator<Map.Entry<Object,Object>> iter) {
		return new LuanFunction() {
			public Object[] call(LuanState lua,Object[] args) {
				if( !iter.hasNext() )
					return LuanFunction.EMPTY_RTN;
				Map.Entry<Object,Object> entry = iter.next();
				return new Object[]{entry.getKey(),entry.getValue()};
			}
		};
	}

	public static LuanFunction pairs(LuanTable t) {
		return pairs( t.iterator() );
	}

	public static LuanFunction ipairs(LuanTable t) {
		return pairs( t.listIterator() );
	}

	public static LuanTable get_metatable(LuanState lua,Object obj) {
		return lua.getMetatable(obj);
	}

	public static LuanTable set_metatable(LuanTable table,LuanTable metatable) {
		table.setMetatable(metatable);
		return table;
	}

	public static boolean raw_equal(Object v1,Object v2) {
		return v1 == v2 || v1 != null && v1.equals(v2);
	}

	public static Object raw_get(LuanTable table,Object index) {
		return table.get(index);
	}

	public static LuanTable raw_set(LuanTable table,Object index,Object value) {
		table.put(index,value);
		return table;
	}

	public static int raw_len(LuanState lua,Object v) throws LuanException {
		if( v instanceof String ) {
			String s = (String)v;
			return s.length();
		}
		if( v instanceof LuanTable ) {
			LuanTable t = (LuanTable)v;
			return t.length();
		}
		throw new LuanException( lua, LuanElement.JAVA, "bad argument #1 to 'raw_len' (table or string expected)" );
	}

	public static Number to_number(Object e,Integer base) {
		return Luan.toNumber(e,base);
	}

	public static String to_string(LuanState lua,Object v) throws LuanException {
		return lua.toString(LuanElement.JAVA,v);
	}

	public static void error(LuanState lua,Object msg) throws LuanException {
		throw new LuanException(lua,LuanElement.JAVA,msg);
	}

}
