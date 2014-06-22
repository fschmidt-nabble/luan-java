package luan.impl;

import luan.Luan;
import luan.LuanException;
import luan.LuanSource;


final class RepeatStmt extends CodeImpl implements Stmt {
	private final Stmt doStmt;
	private final Expr cnd;

	RepeatStmt(LuanSource.Element se,Stmt doStmt,Expr cnd) {
		super(se);
		this.doStmt = doStmt;
		this.cnd = cnd;
	}

	@Override public void eval(LuanStateImpl luan) throws LuanException {
		try {
			do {
				doStmt.eval(luan);
			} while( !luan.bit(se).checkBoolean( cnd.eval(luan) ) );
		} catch(BreakException e) {}
	}
}
