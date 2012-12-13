package luan.interp;

import luan.LuaException;


final class ExpressionsStmt implements Stmt {
	private final Expressions expressions;

	ExpressionsStmt(Expressions expressions) {
		this.expressions = expressions;
	}

	@Override public void eval(LuaStateImpl lua) throws LuaException {
		expressions.eval(lua);
	}

}
