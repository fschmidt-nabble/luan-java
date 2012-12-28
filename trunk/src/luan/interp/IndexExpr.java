package luan.interp;

import luan.Luan;
import luan.LuanException;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanSource;


final class IndexExpr extends BinaryOpExpr {

	IndexExpr(LuanSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuanStateImpl lua) throws LuanException {
		return index(lua,op1.eval(lua),op2.eval(lua));
	}

	private Object index(LuanStateImpl lua,Object t,Object key) throws LuanException {
		Object h;
		if( t instanceof LuanTable ) {
			LuanTable tbl = (LuanTable)t;
			Object value = tbl.get(key);
			if( value != null )
				return value;
			h = lua.getHandler("__index",t);
			if( h==null )
				return null;
		} else {
			h = lua.getHandler("__index",t);
			if( h==null )
				throw new LuanException( lua, se, "attempt to index a " + Luan.type(t) + " value" );
		}
		if( h instanceof LuanFunction ) {
			LuanFunction fn = (LuanFunction)h;
			return Luan.first(lua.call(fn,se,"__index",t,key));
		}
		return index(lua,h,key);
	}
}
