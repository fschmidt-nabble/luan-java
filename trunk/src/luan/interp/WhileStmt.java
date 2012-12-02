package luan.interp;

import luan.Lua;
import luan.LuaState;
import luan.LuaException;


final class WhileStmt implements Stmt {
	private final Expr cnd;
	private final Stmt doStmt;

	WhileStmt(Expr cnd,Stmt doStmt) {
		this.cnd = cnd;
		this.doStmt = doStmt;
	}

	@Override public void eval(LuaState lua) throws LuaException {
		try {
			while( Lua.toBoolean( cnd.eval(lua) ) ) {
				doStmt.eval(lua);
			}
		} catch(BreakException e) {}
	}
}
