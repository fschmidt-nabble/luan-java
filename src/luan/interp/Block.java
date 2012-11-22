package luan.interp;

import luan.LuaState;
import luan.LuaException;


final class Block implements Stmt {
	private final Stmt[] stmts;

	Block(Stmt[] stmts) {
		this.stmts = stmts;
	}

	@Override public void eval(LuaState lua) throws LuaException {
		for( Stmt stmt : stmts ) {
			stmt.eval(lua);
		}
	}

}
