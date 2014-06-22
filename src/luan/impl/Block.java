package luan.impl;

import luan.LuanException;


final class Block implements Stmt {
	final Stmt[] stmts;
	private final int stackStart;
	private final int stackEnd;

	Block(Stmt[] stmts,int stackStart,int stackEnd) {
		if( stmts.length==0 )
			throw new RuntimeException("empty block");
		this.stmts = stmts;
		this.stackStart = stackStart;
		this.stackEnd = stackEnd;
	}

	@Override public void eval(LuanStateImpl luan) throws LuanException {
		try {
			for( Stmt stmt : stmts ) {
				stmt.eval(luan);
			}
		} finally {
			luan.stackClear(stackStart,stackEnd);
		}
	}

}
