package luan.interp;

import luan.Luan;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanException;
import luan.LuanSource;


final class LenExpr extends UnaryOpExpr {

	LenExpr(LuanSource.Element se,Expr op) {
		super(se,op);
	}

	@Override public Object eval(LuanStateImpl lua) throws LuanException {
		Object o = op.eval(lua);
		if( o instanceof String ) {
			String s = (String)o;
			return s.length();
		}
		LuanFunction fn = lua.getHandlerFunction(se,"__len",o);
		if( fn != null )
			return Luan.first(lua.call(fn,se,"__len",o));
		if( o instanceof LuanTable ) {
			LuanTable t = (LuanTable)o;
			return t.length();
		}
		throw new LuanException( lua, se, "attempt to get length of a " + Luan.type(o) + " value" );
	}
}
