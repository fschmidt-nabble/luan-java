package luan.interp;

import luan.Luan;
import luan.LuanException;
import luan.LuanSource;
import luan.LuanBit;


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

	@Override public void eval(LuanStateImpl luan) throws LuanException {
		LuanBit bit = luan.bit(se);
		double v = bit.checkNumber( fromExpr.eval(luan) ).doubleValue();
		double limit = bit.checkNumber( toExpr.eval(luan) ).doubleValue();
		double step = bit.checkNumber( stepExpr.eval(luan) ).doubleValue();
		try {
			while( step > 0.0 && v <= limit || step < 0.0 && v >= limit ) {
				luan.stackSet( iVar, v );
				block.eval(luan);
				v += step;
			}
		} catch(BreakException e) {
		} finally {
			luan.stackClear(iVar,iVar+1);
		}
	}

}
