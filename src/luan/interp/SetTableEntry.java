package luan.interp;

import luan.LuanException;
import luan.LuanTable;
import luan.Luan;
import luan.LuanFunction;
import luan.LuanSource;


final class SetTableEntry extends CodeImpl implements Settable {
	private final Expr tableExpr;
	private final Expr keyExpr;

	SetTableEntry(LuanSource.Element se,Expr tableExpr,Expr keyExpr) {
		super(se);
		this.tableExpr = tableExpr;
		this.keyExpr = keyExpr;
	}

	@Override public void set(LuanStateImpl lua,Object value) throws LuanException {
		newindex( lua, tableExpr.eval(lua), keyExpr.eval(lua), value );
	}

	private void newindex(LuanStateImpl lua,Object t,Object key,Object value) throws LuanException {
		Object h;
		if( t instanceof LuanTable ) {
			LuanTable table = (LuanTable)t;
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
				throw new LuanException( lua, se, "attempt to index a " + Luan.type(t) + " value" );
		}
		if( h instanceof LuanFunction ) {
			LuanFunction fn = (LuanFunction)h;
			lua.call(fn,se,"__newindex",t,key,value);
		}
		newindex(lua,h,key,value);
	}

}
