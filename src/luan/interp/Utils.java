package luan.interp;

import luan.Lua;
import luan.LuaFunction;
import luan.LuaTable;
import luan.LuaException;


final class Utils {
	private Utils() {}  // never

	static Object first(Object[] a) {
		return a.length==0 ? null : a[0];
	}

}
