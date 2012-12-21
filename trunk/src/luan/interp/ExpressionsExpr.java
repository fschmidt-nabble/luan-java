package luan.interp;

import java.util.List;
import luan.Lua;
import luan.LuaException;
import luan.LuaSource;


final class ExpressionsExpr implements Expr {
	private final Expressions expressions;

	ExpressionsExpr(Expressions expressions) {
		this.expressions = expressions;
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		return Lua.first( expressions.eval(lua) );
	}

	public LuaSource.Element se() {
		return expressions.se();
	}

}
