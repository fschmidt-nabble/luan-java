package luan.interp;

import luan.Luan;
import luan.LuanException;


final class IfStmt implements Stmt {
	private final Expr cnd;
	final Stmt thenStmt;
	final Stmt elseStmt;

	IfStmt(Expr cnd,Stmt thenStmt,Stmt elseStmt) {
		this.cnd = cnd;
		this.thenStmt = thenStmt;
		this.elseStmt = elseStmt;
	}

	@Override public void eval(LuanStateImpl lua) throws LuanException {
		if( Luan.toBoolean( cnd.eval(lua) ) ) {
			thenStmt.eval(lua);
		} else {
			elseStmt.eval(lua);
		}
	}
}
