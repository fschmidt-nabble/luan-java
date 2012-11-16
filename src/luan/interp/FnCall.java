package luan.interp;

import java.util.Arrays;
import java.util.List;
import luan.Lua;
import luan.LuaFunction;
import luan.LuaException;
import luan.LuaState;


final class FnCall extends Values {
	private final Expr fnExpr;
	private final Values args;

	FnCall(Expr fnExpr,Values args) {
		this.fnExpr = fnExpr;
		this.args = args;
	}

	List eval(LuaState lua) throws LuaException {
		LuaFunction fn = Lua.checkFunction( fnExpr.eval(lua) );
		return Arrays.asList( fn.call( args.eval(lua).toArray() ) );
	}
}
