package luan.impl;

import luan.Luan;
import luan.LuanException;
import luan.LuanSource;


final class WhileStmt extends CodeImpl implements Stmt {
	private final Expr cnd;
	private final Stmt doStmt;

	WhileStmt(LuanSource.Element se,Expr cnd,Stmt doStmt) {
		super(se);
		this.cnd = cnd;
		this.doStmt = doStmt;
	}

	@Override public void eval(LuanStateImpl luan) throws LuanException {
		try {
			while( luan.bit(se).checkBoolean( cnd.eval(luan) ) ) {
				doStmt.eval(luan);
			}
		} catch(BreakException e) {}
	}
}
