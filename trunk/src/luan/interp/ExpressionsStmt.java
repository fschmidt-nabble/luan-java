package luan.interp;

import luan.LuaState;
import luan.LuaException;


final class ExpressionsStmt implements Stmt {
	private final Expressions expressions;

	ExpressionsStmt(Expressions expressions) {
		this.expressions = expressions;
	}

	@Override public void eval(LuaState lua) throws LuaException {
		expressions.eval(lua);
	}

}
