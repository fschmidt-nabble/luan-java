package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaException;
import luan.LuaState;


// unary minus
final class UnmExpr extends UnaryOpExpr {

	UnmExpr(Expr op) {
		super(op);
	}

	@Override public Object eval(LuaState lua) throws LuaException {
		double n = Lua.checkNumber(op.eval(lua)).value();
		return new LuaNumber( -n );
	}
}
