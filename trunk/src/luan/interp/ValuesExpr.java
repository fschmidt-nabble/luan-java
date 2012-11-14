package luan.interp;

import java.util.List;
import luan.Lua;
import luan.LuaException;


final class ValuesExpr extends Expr {
	private final Values values;

	ValuesExpr(Values values) {
		this.values = values;
	}

	@Override Object eval() throws LuaException {
		List list = values.eval();
		return list.isEmpty() ? null : list.get(0);
	}
}
