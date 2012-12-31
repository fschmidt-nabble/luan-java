package luan.interp;

import luan.Luan;
import luan.LuanSource;
import luan.LuanException;


final class OutputStmt extends CodeImpl implements Stmt {
	private final Expressions expressions;

	OutputStmt(LuanSource.Element se,Expressions expressions) {
		super(se);
		this.expressions = expressions;
	}

	@Override public void eval(LuanStateImpl luan) throws LuanException {
		for( Object obj : expressions.eval(luan) ) {
			luan.out.print( luan.toString(se,obj) );
		}
	}

}
