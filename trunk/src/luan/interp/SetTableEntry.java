package luan.interp;

import luan.LuaException;
import luan.LuaTable;
import luan.Lua;
import luan.LuaFunction;
import luan.LuaSource;


final class SetTableEntry extends CodeImpl implements Settable {
	private final Expr tableExpr;
	private final Expr keyExpr;

	SetTableEntry(LuaSource.Element se,Expr tableExpr,Expr keyExpr) {
		super(se);
		this.tableExpr = tableExpr;
		this.keyExpr = keyExpr;
	}

	@Override public void set(LuaStateImpl lua,Object value) throws LuaException {
		newindex( lua, tableExpr.eval(lua), keyExpr.eval(lua), value );
	}

	private void newindex(LuaStateImpl lua,Object t,Object key,Object value) throws LuaException {
		Object h;
		if( t instanceof LuaTable ) {
			LuaTable table = (LuaTable)t;
			Object old = table.put(key,value);
			if( old != null )
				return;
			h = lua.getHandler("__newindex",t);
			if( h==null )
				return;
			table.put(key,old);
		} else {
			h = lua.getHandler("__newindex",t);
			if( h==null )
				throw new LuaException( lua, se, "attempt to index a " + Lua.type(t) + " value" );
		}
		if( h instanceof LuaFunction ) {
			LuaFunction fn = (LuaFunction)h;
			lua.call(fn,se,"__newindex",t,key,value);
		}
		newindex(lua,h,key,value);
	}

}
