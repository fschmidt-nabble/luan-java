package luan.interp;

import java.util.List;
import luan.Luan;
import luan.LuanException;
import luan.LuanSource;


final class ExpressionsExpr implements Expr {
	private final Expressions expressions;

	ExpressionsExpr(Expressions expressions) {
		this.expressions = expressions;
	}

	@Override public Object eval(LuanStateImpl lua) throws LuanException {
		return Luan.first( expressions.eval(lua) );
	}

	public LuanSource.Element se() {
		return expressions.se();
	}

}
