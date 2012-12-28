package luan.interp;

import luan.LuanException;


final class ExpressionsStmt implements Stmt {
	private final Expressions expressions;

	ExpressionsStmt(Expressions expressions) {
		this.expressions = expressions;
	}

	@Override public void eval(LuanStateImpl lua) throws LuanException {
		expressions.eval(lua);
	}

}
