package luan.interp;

import java.util.List;
import luan.Lua;
import luan.LuaException;
import luan.LuaState;


final class ExpressionsExpr implements Expr {
	private final Expressions expressions;

	ExpressionsExpr(Expressions expressions) {
		this.expressions = expressions;
	}

	@Override public Object eval(LuaState lua) throws LuaException {
		Object[] a = expressions.eval(lua);
		return a.length==0 ? null : a[0];
	}
}
