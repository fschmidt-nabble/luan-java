package luan.interp;

import luan.Lua;
import luan.LuaException;


final class NotExpr extends UnaryOpExpr {

	NotExpr(Expr op) {
		super(op);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		return !Lua.toBoolean(op.eval(lua));
	}
}
