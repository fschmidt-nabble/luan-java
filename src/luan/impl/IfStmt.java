package luan.impl;

import luan.Luan;
import luan.LuanException;
import luan.LuanSource;


final class IfStmt extends CodeImpl implements Stmt {
	private final Expr cnd;
	final Stmt thenStmt;
	final Stmt elseStmt;

	IfStmt(LuanSource.Element se,Expr cnd,Stmt thenStmt,Stmt elseStmt) {
		super(se);
		this.cnd = cnd;
		this.thenStmt = thenStmt;
		this.elseStmt = elseStmt;
	}

	@Override public void eval(LuanStateImpl luan) throws LuanException {
		if( luan.bit(se).checkBoolean( cnd.eval(luan) ) ) {
			thenStmt.eval(luan);
		} else {
			elseStmt.eval(luan);
		}
	}
}
