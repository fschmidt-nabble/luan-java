package luan.interp;

import luan.LuaState;
import luan.LuaException;


final class Block implements Stmt {
	final Stmt[] stmts;
	private final int stackStart;
	private final int stackEnd;

	Block(Stmt[] stmts,int stackStart,int stackEnd) {
		this.stmts = stmts;
		this.stackStart = stackStart;
		this.stackEnd = stackEnd;
	}

	@Override public void eval(LuaState lua) throws LuaException {
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
