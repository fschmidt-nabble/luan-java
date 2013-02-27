package luan.interp;

import luan.Luan;
import luan.LuanSource;
import luan.LuanException;
import luan.LuanBit;


final class OutputStmt extends CodeImpl implements Stmt {
	private final Expressions expressions;

	OutputStmt(LuanSource.Element se,Expressions expressions) {
		super(se);
		this.expressions = expressions;
	}

	@Override public void eval(LuanStateImpl luan) throws LuanException {
		LuanBit bit = luan.bit(se);
		for( Object obj : expressions.eval(luan) ) {
			luan.out.print( bit.toString(obj) );
		}
	}

}
