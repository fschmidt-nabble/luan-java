package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaFunction;
import luan.LuaException;


// unary minus
final class UnmExpr extends UnaryOpExpr {

	UnmExpr(Expr op) {
		super(op);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		Object o = op.eval(lua);
		LuaNumber n = Lua.toNumber(o);
		if( n != null )
			return new LuaNumber( -n.value() );
		LuaFunction fn = Utils.getHandler("__unm",o);
		if( fn != null )
			return Utils.first(fn.call(lua,o));
		throw new LuaException("attempt to perform arithmetic on a "+Lua.type(o)+" value");
	}
}
