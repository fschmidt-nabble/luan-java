package luan.interp;

import luan.Lua;
import luan.LuaException;
import luan.LuaFunction;


final class GenericForStmt implements Stmt {
	private final int iVars;
	private final int nVars;
	private final Expr iterExpr;
	private final Stmt block;

	GenericForStmt(int iVars,int nVars,Expr iterExpr,Stmt block) {
		this.iVars = iVars;
		this.nVars = nVars;
		this.iterExpr = iterExpr;
		this.block = block;
	}

	@Override public void eval(LuaStateImpl lua) throws LuaException {
		LuaFunction iter = Lua.checkFunction( iterExpr.eval(lua) );
		try {
			while(true) {
				Object[] vals = iter.call(lua);
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
