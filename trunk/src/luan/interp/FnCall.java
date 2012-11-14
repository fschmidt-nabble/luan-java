package luan.interp;

import java.util.Arrays;
import java.util.List;
import luan.Lua;
import luan.LuaFunction;
import luan.LuaException;


final class FnCall extends Values {
	private final Expr fnExpr;
	private final Values args;

	FnCall(Expr fnExpr,Values args) {
		this.fnExpr = fnExpr;
		this.args = args;
	}

	List eval() throws LuaException {
		LuaFunction fn = Lua.toFunction( fnExpr.eval() );
		return Arrays.asList( fn.call( args.eval().toArray() ) );
	}
}
