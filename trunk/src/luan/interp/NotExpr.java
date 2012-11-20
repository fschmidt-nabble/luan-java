package luan.interp;

import luan.Lua;
import luan.LuaException;
import luan.LuaState;


final class NotExpr extends UnaryOpExpr {

	NotExpr(Expr op) {
		super(op);
	}

	@Override Object eval(LuaState lua) throws LuaException {
		return !Lua.toBoolean(op.eval(lua));
	}
}
