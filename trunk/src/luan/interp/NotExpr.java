package luan.interp;

import luan.Lua;
import luan.LuaException;
import luan.LuaSource;


final class NotExpr extends UnaryOpExpr {

	NotExpr(LuaSource.Element se,Expr op) {
		super(se,op);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		return !Lua.toBoolean(op.eval(lua));
	}
}
