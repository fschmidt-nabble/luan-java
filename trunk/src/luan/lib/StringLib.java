package luan.lib;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanElement;
import luan.LuanException;
import luan.MetatableGetter;


public final class StringLib {

	public static final LuanFunction LOADER = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) {
			luan.addMetatableGetter(mg);
			LuanTable module = new LuanTable();
			try {
				add( module, "to_binary", String.class );
				add( module, "to_integers", String.class );
				add( module, "from_integers", new int[0].getClass() );
				add( module, "find", String.class, String.class, Integer.class, Boolean.class );
				add( module, "format", String.class, new Object[0].getClass() );
				add( module, "gmatch", String.class, String.class );
				add( module, "gsub", LuanState.class, String.class, String.class, Object.class, Integer.class );
				add( module, "len", String.class );
				add( module, "lower", String.class );
				add( module, "match", String.class, String.class, Integer.class );
				add( module, "rep", String.class, Integer.TYPE, String.class );
				add( module, "reverse", String.class );
				add( module, "sub", String.class, Integer.TYPE, Integer.class );
				add( module, "upper", String.class );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return module;
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(StringLib.class.getMethod(method,parameterTypes),null) );
	}

	private static final LuanTable mt = new LuanTable();
	static {
		try {
			add( mt, "__index", LuanState.class, String.class, Object.class );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private static final MetatableGetter mg = new MetatableGetter() {
		public LuanTable getMetatable(Object obj) {
			return obj instanceof String ? mt : null;
		}
	};

	public static Object __index(LuanState luan,final String s,Object key) throws LuanException {
		LuanTable mod = (LuanTable)luan.loaded().get("String");
		if( mod!=null ) {
			Object obj = mod.get(key);
			if( obj instanceof LuanFunction ) {
				final LuanFunction fn = (LuanFunction)obj;
				return new LuanFunction() {
					@Override public Object call(LuanState luan,Object[] args) throws LuanException {
						Object[] a = new Object[args.length+1];
						a[0] = s;
						System.arraycopy(args,0,a,1,args.length);
						return fn.call(luan,a);
					}
				};
			}
		}
		if( luan.loaded().get("Java") != null )
			return JavaLib.__index(luan,s,key);
		return null;
	}

	static int start(String s,int i) {
		return i==0 ? 0 : i > 0 ? i - 1 : s.length() + i;
	}

	static int start(String s,Integer i,int dflt) {
		return i==null ? dflt : start(s,i);
	}

	static int end(String s,int i) {
		return i==0 ? 0 : i > 0 ? i : s.length() + i + 1;
	}

	static int end(String s,Integer i,int dflt) {
		return i==null ? dflt : end(s,i);
	}

	public static byte[] to_binary(String s) {
		return s.getBytes();
	}

	public static int[] to_integers(String s) {
		char[] a = s.toCharArray();
		int[] chars = new int[a.length];
		for( int i=0; i<a.length; i++ ) {
			chars[i] = a[i];
		}
		return chars;
	}

	public static String from_integers(int... chars) {
		char[] a = new char[chars.length];
		for( int i=0; i<chars.length; i++ ) {
			a[i] = (char)chars[i];
		}
		return new String(a);
	}

	public static int len(String s) {
		return s.length();
	}

	public static String lower(String s) {
		return s.toLowerCase();
	}

	public static String upper(String s) {
		return s.toUpperCase();
	}

	public static String reverse(String s) {
		return new StringBuilder(s).reverse().toString();
	}

	public static String rep(String s,int n,String sep) {
		if( n < 1 )
			return "";
		StringBuilder buf = new StringBuilder(s);
		while( --n > 0 ) {
			if( sep != null )
				buf.append(sep);
			buf.append(s);
		}
		return buf.toString();
	}

	public static String sub(String s,int i,Integer j) {
		int start = start(s,i);
		int end = end(s,j,s.length());
		return s.substring(start,end);
	}

	public static int[] find(String s,String pattern,Integer init,Boolean plain) {
		int start = start(s,init,0);
		if( Boolean.TRUE.equals(plain) ) {
			int i = s.indexOf(pattern,start);
			return i == -1 ? null : new int[]{i+1,i+pattern.length()};
		}
		Matcher m = Pattern.compile(pattern).matcher(s);
		return m.find(start) ? new int[]{m.start()+1,m.end()} : null;
	}

	public static String[] match(String s,String pattern,Integer init) {
		int start = start(s,init,0);
		Matcher m = Pattern.compile(pattern).matcher(s);
		if( !m.find(start) )
			return null;
		final int n = m.groupCount();
		if( n == 0 )
			return new String[]{m.group()};
		String[] rtn = new String[n];
		for( int i=0; i<n; i++ ) {
			rtn[i] = m.group(i+1);
		}
		return rtn;
	}

	public static LuanFunction gmatch(String s,String pattern) {
		final Matcher m = Pattern.compile(pattern).matcher(s);
		return new LuanFunction() {
			@Override public Object call(LuanState luan,Object[] args) {
				if( !m.find() )
					return null;
				final int n = m.groupCount();
				if( n == 0 )
					return m.group();
				String[] rtn = new String[n];
				for( int i=0; i<n; i++ ) {
					rtn[i] = m.group(i+1);
				}
				return rtn;
			}
		};
	}

	public static Object[] gsub(LuanState luan,String s,String pattern,Object repl,Integer n) throws LuanException {
		int max = n==null ? Integer.MAX_VALUE : n;
		final Matcher m = Pattern.compile(pattern).matcher(s);
		if( repl instanceof String ) {
			String replacement = (String)repl;
			int i = 0;
			StringBuffer sb = new StringBuffer();
			while( i<max && m.find() ) {
				m.appendReplacement(sb,replacement);
				i++;
			}
			m.appendTail(sb);
			return new Object[]{ sb.toString(), i };
		}
		if( repl instanceof LuanTable ) {
			LuanTable t = (LuanTable)repl;
			int i = 0;
			StringBuffer sb = new StringBuffer();
			while( i<max && m.find() ) {
				String match = m.groupCount()==0 ? m.group() : m.group(1);
				Object val = t.get(match);
				if( Luan.toBoolean(val) ) {
					String replacement = Luan.asString(val);
					if( replacement==null )
						throw luan.exception( "invalid replacement value (a "+Luan.type(val)+")" );
					m.appendReplacement(sb,replacement);
				}
				i++;
			}
			m.appendTail(sb);
			return new Object[]{ sb.toString(), i };
		}
		if( repl instanceof LuanFunction ) {
			LuanFunction fn = (LuanFunction)repl;
			int i = 0;
			StringBuffer sb = new StringBuffer();
			while( i<max && m.find() ) {
				Object[] args;
				final int count = m.groupCount();
				if( count == 0 ) {
					args = new Object[]{m.group()};
				} else {
					args = new Object[count];
					for( int j=0; j<count; j++ ) {
						args[j] = m.group(j);
					}
				}
				Object val = Luan.first( luan.call(fn,"repl-arg",args) );
				if( Luan.toBoolean(val) ) {
					String replacement = Luan.asString(val);
					if( replacement==null )
						throw luan.exception( "invalid replacement value (a "+Luan.type(val)+")" );
					m.appendReplacement(sb,replacement);
				}
				i++;
			}
			m.appendTail(sb);
			return new Object[]{ sb.toString(), i };
		}
		throw luan.exception( "bad argument #3 to 'gsub' (string/function/table expected)" );
	}

	// note - String.format() is too stupid to convert between ints and floats.
	public static String format(String format,Object... args) {
		return String.format(format,args);
	}

}
