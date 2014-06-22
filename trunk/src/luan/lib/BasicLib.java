package luan.lib;

import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanException;
import luan.LuanSource;
import luan.LuanElement;
import luan.impl.LuanCompiler;


public final class BasicLib {

	public static final LuanFunction LOADER = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) {
			LuanTable module = new LuanTable();
			try {
				module.put( "assert", new LuanJavaFunction(BasicLib.class.getMethod("assert_",LuanState.class,Object.class,String.class),null) );
				add( module, "assert_boolean", LuanState.class, Boolean.TYPE );
				add( module, "assert_nil", LuanState.class, Object.class );
				add( module, "assert_number", LuanState.class, Number.class );
				add( module, "assert_string", LuanState.class, String.class );
				add( module, "assert_table", LuanState.class, LuanTable.class );
				add( module, "do_file", LuanState.class, String.class );
				add( module, "error", LuanState.class, Object.class );
				add( module, "get_metatable", LuanState.class, Object.class );
				add( module, "ipairs", LuanState.class, LuanTable.class );
				add( module, "load", LuanState.class, String.class, String.class, LuanTable.class, Boolean.class );
				add( module, "load_file", LuanState.class, String.class );
				add( module, "pairs", LuanState.class, LuanTable.class );
				add( module, "range", LuanState.class, Double.TYPE, Double.TYPE, Double.class );
				add( module, "raw_equal", Object.class, Object.class );
				add( module, "raw_get", LuanTable.class, Object.class );
				add( module, "raw_len", LuanState.class, Object.class );
				add( module, "raw_set", LuanTable.class, Object.class, Object.class );
				add( module, "repr", LuanState.class, Object.class );
				add( module, "set_metatable", LuanTable.class, LuanTable.class );
				add( module, "to_number", Object.class, Integer.class );
				add( module, "to_string", LuanState.class, Object.class );
				add( module, "type", Object.class );
				add( module, "values", new Object[0].getClass() );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return module;
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(BasicLib.class.getMethod(method,parameterTypes),null) );
	}

	public static String type(Object obj) {
		return Luan.type(obj);
	}

	public static LuanFunction load(LuanState luan,String text,String sourceName,LuanTable env,Boolean allowExpr)
		throws LuanException
	{
		if( allowExpr==null )
			allowExpr = false;
		return LuanCompiler.compile(luan,new LuanSource(sourceName,text),env,allowExpr);
	}

	public static LuanFunction load_file(LuanState luan,String fileName) throws LuanException {
		try {
			String src = fileName==null ? Utils.readAll(new InputStreamReader(System.in)) : IoLib.luanIo(luan,fileName).read_text();
			return load(luan,src,fileName,null,false);
		} catch(IOException e) {
			throw luan.exception(e);
		}
	}

	public static Object do_file(LuanState luan,String fileName) throws LuanException {
		LuanFunction fn = load_file(luan,fileName);
		return luan.call(fn);
	}

	private static LuanFunction pairs(final Iterator<Map.Entry<Object,Object>> iter) {
		return new LuanFunction() {
			@Override public Object[] call(LuanState luan,Object[] args) {
				if( !iter.hasNext() )
					return LuanFunction.NOTHING;
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
		throw luan.exception( "bad argument #1 to 'raw_len' (table or string expected)" );
	}

	public static Number to_number(Object e,Integer base) {
		return Luan.toNumber(e,base);
	}

	public static String to_string(LuanState luan,Object v) throws LuanException {
		return luan.toString(v);
	}

	public static void error(LuanState luan,Object msg) throws LuanException {
		throw luan.exception(msg);
	}

	public static Object assert_(LuanState luan,Object v,String msg) throws LuanException {
		if( Luan.toBoolean(v) )
			return v;
		if( msg == null )
			msg = "assertion failed!";
		throw luan.exception( msg );
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
			throw luan.exception("bad argument #1 (nil expected, got "+Luan.type(v)+")");
		return v;
	}

	public static String repr(LuanState luan,Object v) throws LuanException {
		return luan.repr(v);
	}

	public static LuanFunction range(LuanState luan,final double from,final double to,Double stepV) throws LuanException {
		final double step = stepV==null ? 1.0 : stepV;
		if( step == 0.0 )
			throw luan.exception("bad argument #3 (step may not be zero)");
		return new LuanFunction() {
			double v = from;

			@Override public Object call(LuanState luan,Object[] args) {
				if( step > 0.0 && v > to || step < 0.0 && v < to )
					return LuanFunction.NOTHING;
				double rtn = v;
				v += step;
				return rtn;
			}
		};
	}

	public static LuanFunction values(final Object... args) throws LuanException {
		return new LuanFunction() {
			int i = 0;

			@Override public Object call(LuanState luan,Object[] unused) {
				if( ++i > args.length )
					return LuanFunction.NOTHING;
				return new Object[]{i,args[i-1]};
			}
		};
	}

}
