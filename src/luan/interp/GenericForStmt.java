package luan.interp;

import luan.Lua;
import luan.LuaState;
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

	@Override public void eval(LuaState lua) throws LuaException {
		LuaFunction iter = Lua.checkFunction( iterExpr.eval(lua) );
		Object[] stack = lua.stack();
		try {
			while(true) {
				Object[] vals = iter.call(lua);
				if( vals.length==0 || vals[0]==null )
					break;
				for( int i=0; i<nVars; i++ ) {
					stack[iVars+i] = i < vals.length ? vals[i] : null;
				}
				block.eval(lua);
			}
		} catch(BreakException e) {
		} finally {
			for( int i=iVars; i<iVars+nVars; i++ ) {
				stack[i] = null;
			}
		}
	}

}
