package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaException;


// unary minus
final class UnmExpr extends UnaryOpExpr {

	UnmExpr(Expr op) {
		super(op);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		double n = Lua.checkNumber(op.eval(lua)).value();
		return new LuaNumber( -n );
	}
}