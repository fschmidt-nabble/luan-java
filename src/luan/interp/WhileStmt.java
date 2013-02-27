package luan.interp;

import luan.Luan;
import luan.LuanException;


final class WhileStmt implements Stmt {
	private final Expr cnd;
	private final Stmt doStmt;

	WhileStmt(Expr cnd,Stmt doStmt) {
		this.cnd = cnd;
		this.doStmt = doStmt;
	}

	@Override public void eval(LuanStateImpl luan) throws LuanException {
		try {
			while( Luan.toBoolean( cnd.eval(luan) ) ) {
				doStmt.eval(luan);
			}
		} catch(BreakException e) {}
	}
}