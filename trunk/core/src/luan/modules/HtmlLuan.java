package luan.modules;

import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanException;


public final class HtmlLuan {

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
