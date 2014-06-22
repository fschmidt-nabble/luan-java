package luan.impl;

import luan.Luan;
import luan.LuanException;


final class TryStmt implements Stmt {
	private final Stmt tryBlock;
	private final int iExceptionVar;
	private final Stmt catchBlock;

	TryStmt(Stmt tryBlock,int iExceptionVar,Stmt catchBlock) {
		this.tryBlock = tryBlock;
		this.iExceptionVar = iExceptionVar;
		this.catchBlock = catchBlock;
	}

	@Override public void eval(LuanStateImpl luan) throws LuanException {
		try {
			tryBlock.eval(luan);
		} catch(LuanException e) {
			try {
				luan.stackSet( iExceptionVar, e );
				catchBlock.eval(luan);
			} finally {
				luan.stackClear(iExceptionVar,iExceptionVar+1);
			}
		}
	}
}
