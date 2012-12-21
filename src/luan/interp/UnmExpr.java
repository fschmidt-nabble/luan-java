package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaFunction;
import luan.LuaException;
import luan.LuaSource;


// unary minus
final class UnmExpr extends UnaryOpExpr {

	UnmExpr(LuaSource.Element se,Expr op) {
		super(se,op);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		Object o = op.eval(lua);
		LuaNumber n = Lua.toNumber(o);
		if( n != null )
			return new LuaNumber( -n.value() );
		LuaFunction fn = lua.getHandlerFunction(se,"__unm",o);
		if( fn != null ) {
			return Lua.first(lua.call(fn,se,"__unm",o));
		}
		throw new LuaException(lua,se,"attempt to perform arithmetic on a "+Lua.type(o)+" value");
	}
}
