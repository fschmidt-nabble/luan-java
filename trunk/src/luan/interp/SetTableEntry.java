package luan.interp;

import luan.LuaException;
import luan.LuaTable;
import luan.Lua;


final class SetTableEntry implements Settable {
	private final Expr tableExpr;
	private final Expr keyExpr;

	SetTableEntry(Expr tableExpr,Expr keyExpr) {
		this.tableExpr = tableExpr;
		this.keyExpr = keyExpr;
	}

	@Override public void set(LuaStateImpl lua,Object value) throws LuaException {
		Object t = tableExpr.eval(lua);
		if( !(t instanceof LuaTable) )
			throw new LuaException( "attempt to index a " + Lua.type(t) + " value" );
		LuaTable table = (LuaTable)t;
		Object key = keyExpr.eval(lua);
		table.set(key,value);
	}

}
