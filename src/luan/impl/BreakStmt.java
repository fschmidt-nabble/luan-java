package luan.impl;


final class BreakStmt implements Stmt {

	@Override public void eval(LuanStateImpl luan) {
		throw new BreakException();
	}
}
