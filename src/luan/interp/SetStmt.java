package luan.interp;

import java.util.List;
import luan.Lua;
import luan.LuaState;
import luan.LuaException;
import luan.LuaTable;


final class SetStmt implements Stmt {

	static class Var {
		final Expr table;
		final Expr key;

		Var(Expr table,Expr key) {
			this.table = table;
			this.key = key;
		}
	}

	private final Var[] vars;
	private final Expressions expressions;

	SetStmt(Var[] vars,Expressions expressions) {
		this.vars = vars;
		this.expressions = expressions;
	}

	@Override public void eval(LuaState lua) throws LuaException {
		final Object[] vals = expressions.eval(lua);
		for( int i=0; i<vars.length; i++ ) {
			Var var = vars[i];
			Object t = var.table.eval(lua);
			if( !(t instanceof LuaTable) )
				throw new LuaException( "attempt to index a " + Lua.type(t) + " value" );
			LuaTable tbl = (LuaTable)t;
			Object key = var.key.eval(lua);
			Object val = i < vals.length ? vals[i] : null;
			tbl.set(key,val);
		}
	}

}
