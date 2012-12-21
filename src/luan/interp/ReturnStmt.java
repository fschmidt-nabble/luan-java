package luan.interp;

import luan.Lua;
import luan.LuaException;
import luan.LuaFunction;
import luan.LuaSource;


final class ReturnStmt extends CodeImpl implements Stmt {
	private final Expressions expressions;
	private final Expr tailFnExpr;
	boolean throwReturnException = true;

	ReturnStmt(LuaSource.Element se,Expressions expressions) {
		super(se);
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
			LuaFunction tailFn = lua.checkFunction( se, tailFnExpr.eval(lua) );
			if( tailFn instanceof LuaClosure ) {
				lua.tailFn = (LuaClosure)tailFn;
			} else {
				lua.returnValues =  lua.call(tailFn,tailFnExpr.se(),tailFnExpr.se().text(),lua.returnValues);
			}
		}
		if( throwReturnException )
			throw new ReturnException();
	}
}
