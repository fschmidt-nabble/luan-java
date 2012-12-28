package luan.interp;

import luan.Luan;
import luan.LuanFunction;
import luan.LuanException;
import luan.LuanSource;


final class FnCall extends CodeImpl implements Expressions {
	final Expr fnExpr;
	final Expressions args;
	final String fnName;

	FnCall(LuanSource.Element se,Expr fnExpr,Expressions args) {
		super(se);
		this.fnExpr = fnExpr;
		this.args = args;
		this.fnName = fnExpr.se().text();
	}

	@Override public Object[] eval(LuanStateImpl lua) throws LuanException {
		return call( lua, fnExpr.eval(lua) );
	}

	private Object[] call(LuanStateImpl lua,Object o) throws LuanException {
		if( o instanceof LuanFunction ) {
			LuanFunction fn = (LuanFunction)o;
			return lua.call( fn, se, fnName, args.eval(lua) );
		}
		Object h = lua.getHandler("__call",o);
		if( h != null )
			return call(lua,h);
		throw new LuanException( lua, se, "attempt to call a " + Luan.type(o) + " value" );
	}
}
