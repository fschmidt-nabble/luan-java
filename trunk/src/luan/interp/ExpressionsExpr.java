package luan.interp;

import java.util.List;
import luan.Lua;
import luan.LuaException;


final class ExpressionsExpr implements Expr {
	private final Expressions expressions;

	ExpressionsExpr(Expressions expressions) {
		this.expressions = expressions;
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		return Utils.first( expressions.eval(lua) );
	}
}
