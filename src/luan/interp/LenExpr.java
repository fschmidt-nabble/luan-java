package luan.interp;

import luan.Luan;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanException;
import luan.LuanSource;
import luan.LuanBit;


final class LenExpr extends UnaryOpExpr {

	LenExpr(LuanSource.Element se,Expr op) {
		super(se,op);
	}

	@Override public Object eval(LuanStateImpl luan) throws LuanException {
		Object o = op.eval(luan);
		if( o instanceof String ) {
			String s = (String)o;
			return s.length();
		}
		LuanBit bit = luan.bit(se);
		LuanFunction fn = bit.getHandlerFunction("__len",o);
		if( fn != null )
			return Luan.first(bit.call(fn,"__len",new Object[]{o}));
		if( o instanceof LuanTable ) {
			LuanTable t = (LuanTable)o;
			return t.length();
		}
		throw bit.exception( "attempt to get length of a " + Luan.type(o) + " value" );
	}
}
