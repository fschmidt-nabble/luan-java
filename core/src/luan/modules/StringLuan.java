package luan.modules;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanElement;
import luan.LuanException;
import luan.LuanMethod;


public final class StringLuan {

	public static Object __index(LuanState luan,final String s,Object key) throws LuanException {
		LuanTable mod = (LuanTable)PackageLuan.loaded(luan).get("luan:String");
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
		return null;
	}

	static int start(String s,int i) {
		int len = s.length();
		return i==0 ? 0 : i > 0 ? Math.min(i-1,len) : Math.max(len+i,0);
	}

	static int start(String s,Integer i,int dflt) {
		return i==null ? dflt : start(s,i);
	}

	static int end(String s,int i) {
		int len = s.length();
		return i==0 ? 0 : i > 0 ? Math.min(i,len) : Math.max(len+i+1,0);
	}

	static int end(String s,Integer i,int dflt) {
		return i==null ? dflt : end(s,i);
	}

	@LuanMethod public static Integer[] unicode(LuanState luan,String s,Integer i,Integer j) throws LuanException {
		Utils.checkNotNull(luan,s);
		int start = start(s,i,1);
		int end = end(s,j,start+1);
		Integer[] chars = new Integer[end-start];
		for( int k=0; k<chars.length; k++ ) {
			chars[k] = (int)s.charAt(start+k);
		}
		return chars;
	}

	public static String char_(int... chars) {
		char[] a = new char[chars.length];
		for( int i=0; i<chars.length; i++ ) {
			a[i] = (char)chars[i];
		}
		return new String(a);
	}

	public static int len(LuanState luan,String s) throws LuanException {
		Utils.checkNotNull(luan,s);
		return s.length();
	}

	public static String lower(LuanState luan,String s) throws LuanException {
		Utils.checkNotNull(luan,s);
		return s.toLowerCase();
	}

	public static String upper(LuanState luan,String s) throws LuanException {
		Utils.checkNotNull(luan,s);
		return s.toUpperCase();
	}

	public static String trim(LuanState luan,String s) throws LuanException {
		Utils.checkNotNull(luan,s);
		return s.trim();
	}

	public static String reverse(LuanState luan,String s) throws LuanException {
		Utils.checkNotNull(luan,s);
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

	public static String sub(LuanState luan,String s,int i,Integer j) throws LuanException {
		Utils.checkNotNull(luan,s);
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

	@LuanMethod public static String[] match(String s,String pattern,Integer init) {
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

	public static LuanFunction gmatch(LuanState luan,String s,String pattern) throws LuanException {
		Utils.checkNotNull(luan,s);
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

	@LuanMethod public static Object[] gsub(LuanState luan,String s,String pattern,Object repl,Integer n) throws LuanException {
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

	public static String concat(LuanState luan,Object... args) throws LuanException {
		StringBuilder sb = new StringBuilder();
		for( Object arg : args ) {
			sb.append( luan.toString(arg) );
		}
		return sb.toString();
	}

}
