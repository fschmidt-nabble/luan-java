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

	@Override public void set(LuanStateImpl luan,Object value) throws LuanException {
		newindex( luan, tableExpr.eval(luan), keyExpr.eval(luan), value );
	}

	private void newindex(LuanStateImpl luan,Object t,Object key,Object value) throws LuanException {
		Object h;
		if( t instanceof LuanTable ) {
			LuanTable table = (LuanTable)t;
			Object old = table.put(key,value);
			if( old != null )
				return;
			h = luan.getHandler("__newindex",t);
			if( h==null )
				return;
			table.put(key,old);
		} else {
			h = luan.getHandler("__newindex",t);
			if( h==null )
				throw new LuanException( luan, se, "attempt to index a " + Luan.type(t) + " value" );
		}
		if( h instanceof LuanFunction ) {
			LuanFunction fn = (LuanFunction)h;
			luan.call(fn,se,"__newindex",t,key,value);
		}
		newindex(luan,h,key,value);
	}

}
