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

	static final Object getHandler(String op,Object obj) throws LuaException {
		LuaTable t = Lua.getMetatable(obj);
		return t==null ? null : t.get(op);
	}

	static final LuaFunction getHandlerFunction(String op,Object obj) throws LuaException {
		Object f = getHandler(op,obj);
		if( f == null )
			return null;
		return Lua.checkFunction(f);
	}

}
