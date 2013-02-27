package luan.interp;

import luan.Luan;
import luan.LuanException;
import luan.LuanFunction;
import luan.LuanSource;


final class GenericForStmt extends CodeImpl implements Stmt {
	private final int iVars;
	private final int nVars;
	private final Expr iterExpr;
	private final Stmt block;

	GenericForStmt(LuanSource.Element se,int iVars,int nVars,Expr iterExpr,Stmt block) {
		super(se);
		this.iVars = iVars;
		this.nVars = nVars;
		this.iterExpr = iterExpr;
		this.block = block;
	}

	@Override public void eval(LuanStateImpl luan) throws LuanException {
		LuanFunction iter = luan.bit(se).checkFunction( iterExpr.eval(luan) );
		try {
			while(true) {
				Object[] vals = luan.bit(iterExpr.se()).call(iter,iterExpr.se().text());
				if( vals.length==0 || vals[0]==null )
					break;
				for( int i=0; i<nVars; i++ ) {
					luan.stackSet( iVars+i, i < vals.length ? vals[i] : null );
				}
				block.eval(luan);
			}
		} catch(BreakException e) {
		} finally {
			luan.stackClear(iVars,iVars+nVars);
		}
	}

}
