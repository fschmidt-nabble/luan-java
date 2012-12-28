package luan.interp;

import luan.Luan;
import luan.LuanException;


final class RepeatStmt implements Stmt {
	private final Stmt doStmt;
	private final Expr cnd;

	RepeatStmt(Stmt doStmt,Expr cnd) {
		this.doStmt = doStmt;
		this.cnd = cnd;
	}

	@Override public void eval(LuanStateImpl luan) throws LuanException {
		try {
			do {
				doStmt.eval(luan);
			} while( !Luan.toBoolean( cnd.eval(luan) ) );
		} catch(BreakException e) {}
	}
}
