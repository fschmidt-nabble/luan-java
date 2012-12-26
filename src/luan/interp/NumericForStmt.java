package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaException;
import luan.LuaSource;


final class NumericForStmt extends CodeImpl implements Stmt {
	private final int iVar;
	private final Expr fromExpr;
	private final Expr toExpr;
	private final Expr stepExpr;
	private final Stmt block;

	NumericForStmt(LuaSource.Element se,int iVar,Expr fromExpr,Expr toExpr,Expr stepExpr,Stmt block) {
		super(se);
		this.iVar = iVar;
		this.fromExpr = fromExpr;
		this.toExpr = toExpr;
		this.stepExpr = stepExpr;
		this.block = block;
	}

	@Override public void eval(LuaStateImpl lua) throws LuaException {
		double v = lua.checkNumber( se, fromExpr.eval(lua) ).value();
		double limit = lua.checkNumber( se, toExpr.eval(lua) ).value();
		double step = lua.checkNumber( se, stepExpr.eval(lua) ).value();
		try {
			while( step > 0.0 && v <= limit || step < 0.0 && v >= limit ) {
				lua.stackSet( iVar, LuaNumber.of(v) );
				block.eval(lua);
				v += step;
			}
		} catch(BreakException e) {
		} finally {
			lua.stackClear(iVar,iVar+1);
		}
	}

}
