package luan.impl;

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
			h = luan.getHandler("__newindex",table);
			if( h==null || table.get(key)!=null ) {
				try {
					table.put(key,value);
				} catch(IllegalArgumentException e) {
					throw luan.bit(se).exception(e);
				} catch(UnsupportedOperationException e) {
					throw luan.bit(se).exception(e);
				}
				return;
			}
		} else {
			h = luan.getHandler("__newindex",t);
			if( h==null )
				throw luan.bit(se).exception( "attempt to index '"+tableExpr.se().text()+"' (a " + Luan.type(t) + " value)" );
		}
		if( h instanceof LuanFunction ) {
			LuanFunction fn = (LuanFunction)h;
			luan.bit(se).call(fn,"__newindex",new Object[]{t,key,value});
			return;
		}
		newindex(luan,h,key,value);
	}

}
