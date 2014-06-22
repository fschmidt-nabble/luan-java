package luan.impl;

import java.util.List;
import luan.Luan;
import luan.LuanException;
import luan.LuanSource;


final class ExpressionsExpr implements Expr {
	private final Expressions expressions;

	ExpressionsExpr(Expressions expressions) {
		this.expressions = expressions;
	}

	@Override public Object eval(LuanStateImpl luan) throws LuanException {
		return Luan.first( expressions.eval(luan) );
	}

	public LuanSource.Element se() {
		return expressions.se();
	}

}
