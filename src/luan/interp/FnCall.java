package luan.interp;

import luan.Lua;
import luan.LuaFunction;
import luan.LuaException;
import luan.LuaState;


final class FnCall implements Expressions {
	private final Expr fnExpr;
	private final Expressions args;

	FnCall(Expr fnExpr,Expressions args) {
		this.fnExpr = fnExpr;
		this.args = args;
	}

	@Override public Object[] eval(LuaState lua) throws LuaException {
		LuaFunction fn = Lua.checkFunction( fnExpr.eval(lua) );
		return fn.call( lua, args.eval(lua) );
	}
}
