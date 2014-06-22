package luan.impl;

import luan.Luan;
import luan.LuanException;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanSource;


final class IndexExpr extends BinaryOpExpr {

	IndexExpr(LuanSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuanStateImpl luan) throws LuanException {
		return index(luan,op1.eval(luan),op2.eval(luan));
	}

	private Object index(LuanStateImpl luan,Object t,Object key) throws LuanException {
		Object h;
		if( t instanceof LuanTable ) {
			LuanTable tbl = (LuanTable)t;
			Object value = tbl.get(key);
			if( value != null )
				return value;
			h = luan.getHandler("__index",t);
			if( h==null )
				return null;
		} else {
			h = luan.getHandler("__index",t);
			if( h==null )
				throw luan.bit(op1.se()).exception( "attempt to index '"+op1.se().text()+"' (a " + Luan.type(t) + " value)" );
		}
		if( h instanceof LuanFunction ) {
			LuanFunction fn = (LuanFunction)h;
			return Luan.first(luan.bit(se).call(fn,"__index",new Object[]{t,key}));
		}
		return index(luan,h,key);
	}
}
