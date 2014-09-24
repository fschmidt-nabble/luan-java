package luan.impl;

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

	@Override public void eval(LuanStateImpl luan) throws LuanException {
		luan.returnValues = expressions.eval(luan);
		if( tailFnExpr != null ) {
			LuanSource.Element seTail = tailFnExpr.se();
			LuanFunction tailFn = luan.bit(seTail).checkFunction( tailFnExpr.eval(luan) );
			if( tailFn instanceof Closure ) {
				luan.tailFn = (Closure)tailFn;
			} else {
				luan.returnValues =  luan.bit(seTail).call(tailFn,seTail.text(),Luan.array(luan.returnValues));
			}
		}
		if( throwReturnException )
			throw new ReturnException();
	}
}
