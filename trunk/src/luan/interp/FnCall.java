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
		return call( lua, fnExpr.eval(lua) );
	}

	private Object[] call(LuaStateImpl lua,Object o) throws LuaException {
		if( o instanceof LuaFunction ) {
			LuaFunction fn = (LuaFunction)o;
			return fn.call( lua, args.eval(lua) );
		}
		Object h = Utils.getHandler("__call",o);
		if( h != null )
			return call(lua,h);
		throw new LuaException( "attempt to call a " + Lua.type(o) + " value" );
	}
}
