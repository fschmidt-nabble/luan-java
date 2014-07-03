package luan.impl;

import java.util.List;
import luan.Luan;
import luan.LuanException;
import luan.LuanSource;


final class ExpressionsExpr implements Expr {
	private final Expressions expressions;

	ExpressionsExpr(Expressions expressions) {
		if( expressions==null )
			throw new NullPointerException();
		this.expressions = expressions;
	}

	@Override public Object eval(LuanStateImpl luan) throws LuanException {
		return Luan.first( expressions.eval(luan) );
	}

	public LuanSource.Element se() {
		return expressions.se();
	}

}
