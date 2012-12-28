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

	@Override public void eval(LuanStateImpl lua) throws LuanException {
		LuanFunction iter = lua.checkFunction( se, iterExpr.eval(lua) );
		try {
			while(true) {
				Object[] vals = lua.call(iter,iterExpr.se(),iterExpr.se().text());
				if( vals.length==0 || vals[0]==null )
					break;
				for( int i=0; i<nVars; i++ ) {
					lua.stackSet( iVars+i, i < vals.length ? vals[i] : null );
				}
				block.eval(lua);
			}
		} catch(BreakException e) {
		} finally {
			lua.stackClear(iVars,iVars+nVars);
		}
	}

}
