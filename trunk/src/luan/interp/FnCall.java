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

	@Override public Object[] eval(LuanStateImpl luan) throws LuanException {
		return call( luan, fnExpr.eval(luan) );
	}

	private Object[] call(LuanStateImpl luan,Object o) throws LuanException {
		if( o instanceof LuanFunction ) {
			LuanFunction fn = (LuanFunction)o;
			return luan.call( fn, se, fnName, args.eval(luan) );
		}
		Object h = luan.getHandler("__call",o);
		if( h != null )
			return call(luan,h);
		throw new LuanException( luan, fnExpr.se(), "attempt to call '"+fnExpr.se().text()+"' (a " + Luan.type(o) + " value)" );
	}
}
