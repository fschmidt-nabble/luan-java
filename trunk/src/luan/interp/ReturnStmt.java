package luan.interp;

import luan.Lua;
import luan.LuaException;
import luan.LuaFunction;


final class ReturnStmt implements Stmt {
	private final Expressions expressions;
	private final Expr tailFnExpr;
	boolean throwReturnException = true;

	ReturnStmt(Expressions expressions) {
		if( expressions instanceof FnCall ) {  // tail call
			FnCall fnCall = (FnCall)expressions;
			this.expressions = fnCall.args;
			this.tailFnExpr = fnCall.fnExpr;
		} else {
			this.expressions = expressions;
			this.tailFnExpr = null;
		}
	}

	@Override public void eval(LuaStateImpl lua) throws LuaException {
		lua.returnValues = expressions.eval(lua);
		if( tailFnExpr != null ) {
			LuaFunction tailFn = Lua.checkFunction( tailFnExpr.eval(lua) );
			if( tailFn instanceof LuaClosure ) {
				lua.tailFn = (LuaClosure)tailFn;
			} else {
				lua.returnValues =  tailFn.call(lua,lua.returnValues);
			}
		}
		if( throwReturnException )
			throw new ReturnException();
	}
}
