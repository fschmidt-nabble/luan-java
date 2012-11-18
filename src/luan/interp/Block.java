package luan.interp;

import luan.LuaState;
import luan.LuaException;


final class Block extends Stmt {
	private final Stmt[] stmts;

	Block(Stmt[] stmts) {
		this.stmts = stmts;
	}

	@Override void eval(LuaState lua) throws LuaException {
		for( Stmt stmt : stmts ) {
			stmt.eval(lua);
		}
	}

}
