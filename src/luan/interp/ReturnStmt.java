package luan.interp;

import luan.LuaState;
import luan.LuaException;


final class ReturnStmt implements Stmt {
	private final Expressions expressions;
	boolean throwReturnException = true;

	ReturnStmt(Expressions expressions) {
		this.expressions = expressions;
	}

	@Override public void eval(LuaState lua) throws LuaException {
		lua.returnValues = expressions.eval(lua);
		if( throwReturnException )
			throw new ReturnException();
	}
}
