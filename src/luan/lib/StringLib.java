package luan.lib;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import luan.Lua;
import luan.LuaState;
import luan.LuaTable;
import luan.LuaFunction;
import luan.LuaJavaFunction;
import luan.LuaNumber;
import luan.LuaElement;
import luan.LuaException;


public final class StringLib {

	public static void register(LuaState lua) {
		LuaTable module = new LuaTable();
		LuaTable global = lua.global();
		global.put("string",module);
		try {
			module.put( "byte", new LuaJavaFunction(StringLib.class.getMethod("byte_",String.class,Integer.class,Integer.class),null) );
			module.put( "char", new LuaJavaFunction(StringLib.class.getMethod("char_",new byte[0].getClass()),null) );
			add( module, "find", String.class, String.class, Integer.class, Boolean.class );
			add( module, "gmatch", String.class, String.class );
			add( module, "gsub", LuaState.class, String.class, String.class, Object.class, Integer.class );
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
	}

	private static void add(LuaTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuaJavaFunction(StringLib.class.getMethod(method,parameterTypes),null) );
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

	public static byte[] byte_(String s,Integer i,Integer j) {
		int start = start(s,i,0);
		int end = end(s,j,start+1);
		return s.substring(start,end).getBytes();
	}

	public static String char_(byte... bytes) {
		return new String(bytes);
	}

	// format is hard because String.format() is too stupid to convert ints to floats.

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
			rtn[i] = m.group(i);
		}
		return rtn;
	}

	public static LuaFunction gmatch(String s,String pattern) {
		final Matcher m = Pattern.compile(pattern).matcher(s);
		return new LuaFunction() {
			public Object[] call(LuaState lua,Object[] args) {
				if( !m.find() )
					return LuaFunction.EMPTY_RTN;
				final int n = m.groupCount();
				if( n == 0 )
					return new String[]{m.group()};
				String[] rtn = new String[n];
				for( int i=0; i<n; i++ ) {
					rtn[i] = m.group(i);
				}
				return rtn;
			}
		};
	}

	public static Object[] gsub(LuaState lua,String s,String pattern,Object repl,Integer n) throws LuaException {
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
			return new Object[]{ sb.toString(), new LuaNumber(i) };
		}
		if( repl instanceof LuaTable ) {
			LuaTable t = (LuaTable)repl;
			int i = 0;
			StringBuffer sb = new StringBuffer();
			while( i<max && m.find() ) {
				String match = m.groupCount()==0 ? m.group() : m.group(0);
				Object val = t.get(match);
				if( Lua.toBoolean(val) ) {
					String replacement = Lua.asString(val);
					if( replacement==null )
						throw new LuaException( lua, LuaElement.JAVA, "invalid replacement value (a "+Lua.type(val)+")" );
					m.appendReplacement(sb,replacement);
				}
				i++;
			}
			m.appendTail(sb);
			return new Object[]{ sb.toString(), new LuaNumber(i) };
		}
		if( repl instanceof LuaFunction ) {
			LuaFunction fn = (LuaFunction)repl;
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
				Object val = Lua.first( lua.call(fn,LuaElement.JAVA,"repl-arg",args) );
				if( Lua.toBoolean(val) ) {
					String replacement = Lua.asString(val);
					if( replacement==null )
						throw new LuaException( lua, LuaElement.JAVA, "invalid replacement value (a "+Lua.type(val)+")" );
					m.appendReplacement(sb,replacement);
				}
				i++;
			}
			m.appendTail(sb);
			return new Object[]{ sb.toString(), new LuaNumber(i) };
		}
		throw new LuaException( lua, LuaElement.JAVA, "bad argument #3 to 'gsub' (string/function/table expected)" );
	}

}
