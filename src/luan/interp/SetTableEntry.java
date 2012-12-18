package luan.interp;

import luan.LuaException;
import luan.LuaTable;
import luan.Lua;
import luan.LuaFunction;


final class SetTableEntry implements Settable {
	private final Expr tableExpr;
	private final Expr keyExpr;

	SetTableEntry(Expr tableExpr,Expr keyExpr) {
		this.tableExpr = tableExpr;
		this.keyExpr = keyExpr;
	}

	@Override public void set(LuaStateImpl lua,Object value) throws LuaException {
		newindex( lua, tableExpr.eval(lua), keyExpr.eval(lua), value );
	}

	private static void newindex(LuaStateImpl lua,Object t,Object key,Object value) throws LuaException {
		Object h;
		if( t instanceof LuaTable ) {
			LuaTable table = (LuaTable)t;
			Object old = table.put(key,value);
			if( old != null )
				return;
			h = Utils.getHandler("__newindex",t);
			if( h==null )
				return;
			table.put(key,old);
		} else {
			h = Utils.getHandler("__newindex",t);
			if( h==null )
				throw new LuaException( "attempt to index a " + Lua.type(t) + " value" );
		}
		if( h instanceof LuaFunction ) {
			LuaFunction fn = (LuaFunction)h;
			fn.call(lua,t,key,value);
		}
		newindex(lua,h,key,value);
	}

}
