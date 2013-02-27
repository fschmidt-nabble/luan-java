package luan.lib;

import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanLoader;
import luan.LuanJavaFunction;
import luan.LuanException;
import luan.LuanSource;
import luan.LuanElement;
import luan.interp.LuanCompiler;


public final class BasicLib {

	public static final String NAME = "basic";

	public static final LuanLoader LOADER = new LuanLoader() {
		@Override protected void load(LuanState luan) {
			LuanTable module = new LuanTable();
			LuanTable global = new LuanTable();
			module.put( LuanState._G, global );
			try {
				global.put( "assert", new LuanJavaFunction(BasicLib.class.getMethod("assert_",LuanState.class,Object.class,String.class),null) );
				add( global, "assert_boolean", LuanState.class, Boolean.TYPE );
				add( global, "assert_nil", LuanState.class, Object.class );
				add( global, "assert_number", LuanState.class, Number.class );
				add( global, "assert_string", LuanState.class, String.class );
				add( global, "assert_table", LuanState.class, LuanTable.class );
				add( global, "do_file", LuanState.class, String.class, LuanTable.class );
				add( global, "error", LuanState.class, Object.class );
				add( global, "get_metatable", LuanState.class, Object.class );
				add( global, "ipairs", LuanState.class, LuanTable.class );
				add( global, "load", LuanState.class, String.class, String.class, LuanTable.class );
				add( global, "load_file", LuanState.class, String.class, LuanTable.class );
				add( global, "pairs", LuanState.class, LuanTable.class );
				add( global, "print", LuanState.class, new Object[0].getClass() );
				add( global, "raw_equal", Object.class, Object.class );
				add( global, "raw_get", LuanTable.class, Object.class );
				add( global, "raw_len", LuanState.class, Object.class );
				add( global, "raw_set", LuanTable.class, Object.class, Object.class );
				add( global, "repr", LuanState.class, Object.class );
				add( global, "set_metatable", LuanTable.class, LuanTable.class );
				add( global, "to_number", Object.class, Integer.class );
				add( global, "to_string", LuanState.class, Object.class );
				add( global, "type", Object.class );
				global.put( "_VERSION", Luan.version );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			luan.loaded().put(NAME,module);
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(BasicLib.class.getMethod(method,parameterTypes),null) );
	}

	public static void print(LuanState luan,Object... args) throws LuanException {
		for( int i=0; i<args.length; i++ ) {
			if( i > 0 )
				luan.out.print('\t');
			luan.out.print( luan.JAVA.toString(args[i]) );
		}
		luan.out.println();
	}

	public static String type(Object obj) {
		return Luan.type(obj);
	}

	public static LuanFunction load(LuanState luan,String text,String sourceName,LuanTable env) throws LuanException {
		return LuanCompiler.compile(luan,new LuanSource(sourceName,text),env);
	}


	public static LuanFunction load_file(LuanState luan,String fileName,LuanTable env) throws LuanException {
		try {
			String src = fileName==null ? Utils.readAll(new InputStreamReader(System.in)) : Utils.read(new File(fileName));
			return load(luan,src,fileName,env);
		} catch(IOException e) {
			throw luan.JAVA.exception(e);
		}
	}

	public static Object[] do_file(LuanState luan,String fileName,LuanTable env) throws LuanException {
		LuanFunction fn = load_file(luan,fileName,env);
		return luan.JAVA.call(fn,null);
	}

	private static LuanFunction pairs(final Iterator<Map.Entry<Object,Object>> iter) {
		return new LuanFunction() {
			public Object[] call(LuanState luan,Object[] args) {
				if( !iter.hasNext() )
					return LuanFunction.EMPTY;
				Map.Entry<Object,Object> entry = iter.next();
				return new Object[]{entry.getKey(),entry.getValue()};
			}
		};
	}

	public static LuanFunction pairs(LuanState luan,LuanTable t) throws LuanException {
		Utils.checkNotNull(luan,t,"table");
		return pairs( t.iterator() );
	}

	public static LuanFunction ipairs(LuanState luan,LuanTable t) throws LuanException {
		Utils.checkNotNull(luan,t,"table");
		return pairs( t.listIterator() );
	}

	public static LuanTable get_metatable(LuanState luan,Object obj) {
		return luan.getMetatable(obj);
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

	public static int raw_len(LuanState luan,Object v) throws LuanException {
		if( v instanceof String ) {
			String s = (String)v;
			return s.length();
		}
		if( v instanceof LuanTable ) {
			LuanTable t = (LuanTable)v;
			return t.length();
		}
		throw luan.JAVA.exception( "bad argument #1 to 'raw_len' (table or string expected)" );
	}

	public static Number to_number(Object e,Integer base) {
		return Luan.toNumber(e,base);
	}

	public static String to_string(LuanState luan,Object v) throws LuanException {
		return luan.JAVA.toString(v);
	}

	public static void error(LuanState luan,Object msg) throws LuanException {
		throw luan.JAVA.exception(msg);
	}

	public static Object assert_(LuanState luan,Object v,String msg) throws LuanException {
		if( Luan.toBoolean(v) )
			return v;
		if( msg == null )
			msg = "assertion failed!";
		throw luan.JAVA.exception( msg );
	}

	public static String assert_string(LuanState luan,String v) throws LuanException {
		Utils.checkNotNull(luan,v,"string");
		return v;
	}

	public static Number assert_number(LuanState luan,Number v) throws LuanException {
		Utils.checkNotNull(luan,v,"number");
		return v;
	}

	public static LuanTable assert_table(LuanState luan,LuanTable v) throws LuanException {
		Utils.checkNotNull(luan,v,"table");
		return v;
	}

	public static boolean assert_boolean(LuanState luan,boolean v) throws LuanException {
		return v;
	}

	public static Object assert_nil(LuanState luan,Object v) throws LuanException {
		if( v != null )
			throw luan.JAVA.exception("bad argument #1 (nil expected, got "+Luan.type(v)+")");
		return v;
	}

	public static String repr(LuanState luan,Object v) throws LuanException {
		return luan.JAVA.repr(v);
	}

}
