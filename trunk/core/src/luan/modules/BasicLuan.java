package luan.modules;

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


public final class BasicLuan {

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

	public static LuanFunction load_file(LuanState luan,String fileName,LuanTable env) throws LuanException {
		if( fileName == null )
			fileName = "stdin:";
		String src = PackageLuan.read(luan,fileName);
		if( src == null )
			throw luan.exception( "file '"+fileName+"' not found" );
		return load(luan,src,fileName,env,false);
	}

	public static Object do_file(LuanState luan,String fileName) throws LuanException {
		LuanFunction fn = load_file(luan,fileName,null);
		return luan.call(fn);
	}

	public static LuanFunction pairs(LuanState luan,final LuanTable t) throws LuanException {
		Utils.checkNotNull(luan,t);
		return new LuanFunction() {
			Iterator<Map.Entry<Object,Object>> iter = t.iterator();

			@Override public Object[] call(LuanState luan,Object[] args) {
				if( !iter.hasNext() )
					return LuanFunction.NOTHING;
				Map.Entry<Object,Object> entry = iter.next();
				return new Object[]{entry.getKey(),entry.getValue()};
			}
		};
	}

	public static LuanFunction ipairs(LuanState luan,final LuanTable t) throws LuanException {
		Utils.checkNotNull(luan,t);
		return new LuanFunction() {
			List<Object> list = t.asList();
			int i = 0;
			final int size = list.size();

			@Override public Object[] call(LuanState luan,Object[] args) {
				if( i >= size )
					return LuanFunction.NOTHING;
				Object val = list.get(i++);
				return new Object[]{i,val};
			}
		};
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

	public static boolean to_boolean(Object v) throws LuanException {
		return Luan.toBoolean(v);
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
		Utils.checkNotNull(luan,v);
		return v;
	}

	public static Number assert_number(LuanState luan,Number v) throws LuanException {
		Utils.checkNotNull(luan,v);
		return v;
	}

	public static LuanTable assert_table(LuanState luan,LuanTable v) throws LuanException {
		Utils.checkNotNull(luan,v);
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

	public static int assert_integer(LuanState luan,int v) throws LuanException {
		return v;
	}

	public static long assert_long(LuanState luan,long v) throws LuanException {
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
