package luan.interp;


final class BreakStmt implements Stmt {

	@Override public void eval(LuanStateImpl lua) {
		throw new BreakException();
	}
}
