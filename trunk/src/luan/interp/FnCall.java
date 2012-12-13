package luan.interp;

import luan.Lua;
import luan.LuaFunction;
import luan.LuaException;


final class FnCall implements Expressions {
	final Expr fnExpr;
	final Expressions args;

	FnCall(Expr fnExpr,Expressions args) {
		this.fnExpr = fnExpr;
		this.args = args;
	}

	@Override public Object[] eval(LuaStateImpl lua) throws LuaException {
		LuaFunction fn = Lua.checkFunction( fnExpr.eval(lua) );
		return fn.call( lua, args.eval(lua) );
	}
}
