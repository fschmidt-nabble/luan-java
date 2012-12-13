package luan.interp;


final class BreakStmt implements Stmt {

	@Override public void eval(LuaStateImpl lua) {
		throw new BreakException();
	}
}
