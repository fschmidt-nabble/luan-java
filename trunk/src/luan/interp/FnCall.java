package luan.interp;

import luan.Lua;
import luan.LuaFunction;
import luan.LuaException;
import luan.LuaSource;


final class FnCall extends CodeImpl implements Expressions {
	final Expr fnExpr;
	final Expressions args;
	final String fnName;

	FnCall(LuaSource.Element se,Expr fnExpr,Expressions args) {
		super(se);
		this.fnExpr = fnExpr;
		this.args = args;
		this.fnName = fnExpr.se().text();
	}

	@Override public Object[] eval(LuaStateImpl lua) throws LuaException {
		return call( lua, fnExpr.eval(lua) );
	}

	private Object[] call(LuaStateImpl lua,Object o) throws LuaException {
		if( o instanceof LuaFunction ) {
			LuaFunction fn = (LuaFunction)o;
			return lua.call( fn, se, fnName, args.eval(lua) );
		}
		Object h = lua.getHandler("__call",o);
		if( h != null )
			return call(lua,h);
		throw new LuaException( lua, se, "attempt to call a " + Lua.type(o) + " value" );
	}
}
