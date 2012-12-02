package luan.interp;

import luan.Lua;
import luan.LuaState;
import luan.LuaException;


final class RepeatStmt implements Stmt {
	private final Stmt doStmt;
	private final Expr cnd;

	RepeatStmt(Stmt doStmt,Expr cnd) {
		this.doStmt = doStmt;
		this.cnd = cnd;
	}

	@Override public void eval(LuaState lua) throws LuaException {
		try {
			do {
				doStmt.eval(lua);
			} while( !Lua.toBoolean( cnd.eval(lua) ) );
		} catch(BreakException e) {}
	}
}
