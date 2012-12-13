package luan.interp;

import luan.LuaException;


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

	@Override public void eval(LuaStateImpl lua) throws LuaException {
		try {
			for( Stmt stmt : stmts ) {
				stmt.eval(lua);
			}
		} finally {
			Object[] stack = lua.stack();
			for( int i=stackStart; i<stackEnd; i++ ) {
				stack[i] = null;
			}
		}
	}

}
