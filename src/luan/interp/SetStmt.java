package luan.interp;

import luan.Luan;
import luan.LuanException;


final class SetStmt implements Stmt {
	private final Settable[] vars;
	private final Expressions expressions;

	SetStmt(Settable var,Expr expr) {
		this( new Settable[]{var}, expr );
	}

	SetStmt(Settable[] vars,Expressions expressions) {
		this.vars = vars;
		this.expressions = expressions;
	}

	@Override public void eval(LuanStateImpl luan) throws LuanException {
		final Object obj = expressions.eval(luan);
		if( obj instanceof Object[] ) {
			Object[] vals = (Object[])obj;
			for( int i=0; i<vars.length; i++ ) {
				Object val = i < vals.length ? vals[i] : null;
				vars[i].set(luan,val);
			}
		} else {
			vars[0].set(luan,obj);
			for( int i=1; i<vars.length; i++ ) {
				vars[i].set(luan,null);
			}
		}
	}

}
