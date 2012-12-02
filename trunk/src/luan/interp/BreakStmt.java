package luan.interp;

import luan.LuaState;


final class BreakStmt implements Stmt {

	@Override public void eval(LuaState lua) {
		throw new BreakException();
	}
}
