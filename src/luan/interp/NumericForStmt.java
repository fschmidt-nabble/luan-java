package luan.interp;

import luan.Luan;
import luan.LuanException;
import luan.LuanSource;


final class NumericForStmt extends CodeImpl implements Stmt {
	private final int iVar;
	private final Expr fromExpr;
	private final Expr toExpr;
	private final Expr stepExpr;
	private final Stmt block;

	NumericForStmt(LuanSource.Element se,int iVar,Expr fromExpr,Expr toExpr,Expr stepExpr,Stmt block) {
		super(se);
		this.iVar = iVar;
		this.fromExpr = fromExpr;
		this.toExpr = toExpr;
		this.stepExpr = stepExpr;
		this.block = block;
	}

	@Override public void eval(LuanStateImpl lua) throws LuanException {
		double v = lua.checkNumber( se, fromExpr.eval(lua) ).doubleValue();
		double limit = lua.checkNumber( se, toExpr.eval(lua) ).doubleValue();
		double step = lua.checkNumber( se, stepExpr.eval(lua) ).doubleValue();
		try {
			while( step > 0.0 && v <= limit || step < 0.0 && v >= limit ) {
				lua.stackSet( iVar, v );
				block.eval(lua);
				v += step;
			}
		} catch(BreakException e) {
		} finally {
			lua.stackClear(iVar,iVar+1);
		}
	}

}
