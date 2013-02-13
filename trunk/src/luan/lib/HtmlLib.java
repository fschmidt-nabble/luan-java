package luan.lib;

import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;


public final class HtmlLib {

	public static final String NAME = "html";

	public static final LuanFunction LOADER = new LuanFunction() {
		public Object[] call(LuanState luan,Object[] args) {
			LuanTable module = new LuanTable();
			LuanTable global = luan.global;
			try {
				add( module, "encode", String.class );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return new Object[]{module};
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(HtmlLib.class.getMethod(method,parameterTypes),null) );
	}

	public static String encode(String s) {
		char[] a = s.toCharArray();
		StringBuilder buf = new StringBuilder();
		for( int i=0; i<a.length; i++ ) {
			char c = a[i];
			switch(c) {
			case '&':
				buf.append("&amp;");
				break;
			case '<':
				buf.append("&lt;");
				break;
			case '>':
				buf.append("&gt;");
				break;
			case '"':
				buf.append("&quot;");
				break;
			default:
				buf.append(c);
			}
		}
		return buf.toString();
	}

}
