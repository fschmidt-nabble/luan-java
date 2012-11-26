package luan.interp;

import luan.Lua;
import luan.LuaState;
import luan.LuaException;


final class IfStmt implements Stmt {
	private final Expr cnd;
	private final Stmt thenStmt;
	private final Stmt elseStmt;

	IfStmt(Expr cnd,Stmt thenStmt,Stmt elseStmt) {
		this.cnd = cnd;
		this.thenStmt = thenStmt;
		this.elseStmt = elseStmt;
	}

	@Override public void eval(LuaState lua) throws LuaException {
		if( Lua.toBoolean( cnd.eval(lua) ) ) {
			thenStmt.eval(lua);
		} else {
			elseStmt.eval(lua);
		}
	}
}
