package luan.interp;

import luan.Luan;
import luan.LuanException;
import luan.LuanFunction;
import luan.LuanSource;


final class ReturnStmt extends CodeImpl implements Stmt {
	private final Expressions expressions;
	private final Expr tailFnExpr;
	boolean throwReturnException = true;

	ReturnStmt(LuanSource.Element se,Expressions expressions) {
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

	@Override public void eval(LuanStateImpl lua) throws LuanException {
		lua.returnValues = expressions.eval(lua);
		if( tailFnExpr != null ) {
			LuanFunction tailFn = lua.checkFunction( se, tailFnExpr.eval(lua) );
			if( tailFn instanceof Closure ) {
				lua.tailFn = (Closure)tailFn;
			} else {
				lua.returnValues =  lua.call(tailFn,tailFnExpr.se(),tailFnExpr.se().text(),lua.returnValues);
			}
		}
		if( throwReturnException )
			throw new ReturnException();
	}
}
