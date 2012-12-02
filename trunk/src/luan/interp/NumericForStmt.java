package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaState;
import luan.LuaException;


final class NumericForStmt implements Stmt {
	private final int iVar;
	private final Expr fromExpr;
	private final Expr toExpr;
	private final Expr stepExpr;
	private final Stmt block;

	NumericForStmt(int iVar,Expr fromExpr,Expr toExpr,Expr stepExpr,Stmt block) {
		this.iVar = iVar;
		this.fromExpr = fromExpr;
		this.toExpr = toExpr;
		this.stepExpr = stepExpr;
		this.block = block;
	}

	@Override public void eval(LuaState lua) throws LuaException {
		double v = Lua.checkNumber( fromExpr.eval(lua) ).value();
		double limit = Lua.checkNumber( toExpr.eval(lua) ).value();
		double step = Lua.checkNumber( stepExpr.eval(lua) ).value();
		Object[] stack = lua.stack();
		try {
			while( step > 0.0 && v <= limit || step < 0.0 && v >= limit ) {
				stack[iVar] = new LuaNumber(v);
				block.eval(lua);
				v += step;
			}
		} catch(BreakException e) {
		} finally {
			stack[iVar] = null;
		}
	}

}
