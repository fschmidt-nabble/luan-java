package luan.interp;

import luan.Lua;
import luan.LuaException;


final class IfStmt implements Stmt {
	private final Expr cnd;
	final Stmt thenStmt;
	final Stmt elseStmt;

	IfStmt(Expr cnd,Stmt thenStmt,Stmt elseStmt) {
		this.cnd = cnd;
		this.thenStmt = thenStmt;
		this.elseStmt = elseStmt;
	}

	@Override public void eval(LuaStateImpl lua) throws LuaException {
		if( Lua.toBoolean( cnd.eval(lua) ) ) {
			thenStmt.eval(lua);
		} else {
			elseStmt.eval(lua);
		}
	}
}
