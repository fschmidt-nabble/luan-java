package luan.lib;

import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanLoader;
import luan.LuanJavaFunction;


public final class HtmlLib {

	public static final String NAME = "html";

	public static final LuanLoader LOADER = new LuanLoader() {
		@Override protected void load(LuanState luan) {
			LuanTable module = new LuanTable();
			try {
				add( module, "encode", String.class );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			luan.loaded().put(NAME,module);
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
