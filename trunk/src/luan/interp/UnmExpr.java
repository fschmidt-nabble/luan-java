package luan.interp;

import luan.Luan;
import luan.LuanFunction;
import luan.LuanException;
import luan.LuanSource;
import luan.LuanBit;


// unary minus
final class UnmExpr extends UnaryOpExpr {

	UnmExpr(LuanSource.Element se,Expr op) {
		super(se,op);
	}

	@Override public Object eval(LuanStateImpl luan) throws LuanException {
		Object o = op.eval(luan);
		Number n = Luan.toNumber(o);
		if( n != null )
			return -n.doubleValue();
		LuanBit bit = luan.bit(se);
		LuanFunction fn = bit.getHandlerFunction("__unm",o);
		if( fn != null ) {
			return Luan.first(bit.call(fn,"__unm",o));
		}
		throw bit.exception("attempt to perform arithmetic on a "+Luan.type(o)+" value");
	}
}
