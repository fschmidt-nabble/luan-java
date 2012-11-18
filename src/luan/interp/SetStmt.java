package luan.interp;

import java.util.List;
import luan.Lua;
import luan.LuaState;
import luan.LuaException;
import luan.LuaTable;


final class SetStmt extends Stmt {

	static class Var {
		final Expr table;
		final Expr key;

		Var(Expr table,Expr key) {
			this.table = table;
			this.key = key;
		}
	}

	private final Var[] vars;
	private final Values values;

	SetStmt(Var[] vars,Values values) {
		this.vars = vars;
		this.values = values;
	}

	@Override void eval(LuaState lua) throws LuaException {
		List vals = values.eval(lua);
		int n = vals.size();
		for( int i=0; i<vars.length; i++ ) {
			Var var = vars[i];
			Object t = var.table.eval(lua);
			if( !(t instanceof LuaTable) )
				throw new LuaException( "attempt to index a " + Lua.type(t) + " value" );
			LuaTable tbl = (LuaTable)t;
			Object key = var.key.eval(lua);
			Object val = i < n ? vals.get(i) : null;
			tbl.set(key,val);
		}
	}

}
