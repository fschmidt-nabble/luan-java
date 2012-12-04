package luan.interp;

import luan.Lua;
import luan.LuaState;
import luan.LuaException;


final class SetStmt implements Stmt {
	private final Settable[] vars;
	private final Expressions expressions;

	SetStmt(Settable var,Expr expr) {
		this( new Settable[]{var}, new ExpList.SingleExpList(expr) );
	}

	SetStmt(Settable[] vars,Expressions expressions) {
		this.vars = vars;
		this.expressions = expressions;
	}

	@Override public void eval(LuaState lua) throws LuaException {
		final Object[] vals = expressions.eval(lua);
		for( int i=0; i<vars.length; i++ ) {
			Object val = i < vals.length ? vals[i] : null;
			vars[i].set(lua,val);
		}
	}

}
