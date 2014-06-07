package luan.interp;

import luan.Luan;
import luan.LuanException;
import luan.LuanFunction;
import luan.LuanSource;
import luan.LuanBit;


final class ForStmt extends CodeImpl implements Stmt {
	private final int iVars;
	private final int nVars;
	private final Expr iterExpr;
	private final Stmt block;

	ForStmt(LuanSource.Element se,int iVars,int nVars,Expr iterExpr,Stmt block) {
		super(se);
		this.iVars = iVars;
		this.nVars = nVars;
		this.iterExpr = iterExpr;
		this.block = block;
	}

	@Override public void eval(LuanStateImpl luan) throws LuanException {
		LuanFunction iter = luan.bit(se).checkFunction( iterExpr.eval(luan) );
		LuanBit bit = luan.bit(iterExpr.se());
		String name = iterExpr.se().text();
		try {
			while(true) {
				Object vals = bit.call(iter,name);
				if( vals==null )
					break;
				if( vals instanceof Object[] ) {
					Object[] a = (Object[])vals;
					if( a.length==0 )
						break;
					for( int i=0; i<nVars; i++ ) {
						luan.stackSet( iVars+i, i < a.length ? a[i] : null );
					}
				} else {
					luan.stackSet( iVars, vals );
					for( int i=1; i<nVars; i++ ) {
						luan.stackSet( iVars+i, null );
					}
				}
				block.eval(luan);
			}
		} catch(BreakException e) {
		} finally {
			luan.stackClear(iVars,iVars+nVars);
		}
	}

}
