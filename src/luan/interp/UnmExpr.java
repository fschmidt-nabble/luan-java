package luan.interp;

import luan.Luan;
import luan.LuanFunction;
import luan.LuanException;
import luan.LuanSource;


// unary minus
final class UnmExpr extends UnaryOpExpr {

	UnmExpr(LuanSource.Element se,Expr op) {
		super(se,op);
	}

	@Override public Object eval(LuanStateImpl lua) throws LuanException {
		Object o = op.eval(lua);
		Number n = Luan.toNumber(o);
		if( n != null )
			return -n.doubleValue();
		LuanFunction fn = lua.getHandlerFunction(se,"__unm",o);
		if( fn != null ) {
			return Luan.first(lua.call(fn,se,"__unm",o));
		}
		throw new LuanException(lua,se,"attempt to perform arithmetic on a "+Luan.type(o)+" value");
	}
}
