package luan.interp;

import java.util.Arrays;
import java.util.List;
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

	@Override public List eval(LuaState lua) throws LuaException {
		LuaFunction fn = Lua.checkFunction( fnExpr.eval(lua) );
		return Arrays.asList( fn.call( lua, args.eval(lua).toArray() ) );
	}
}
