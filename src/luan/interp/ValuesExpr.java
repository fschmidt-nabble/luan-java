package luan.interp;

import java.util.List;
import luan.Lua;
import luan.LuaException;
import luan.LuaState;


final class ValuesExpr extends Expr {
	private final Values values;

	ValuesExpr(Values values) {
		this.values = values;
	}

	@Override Object eval(LuaState lua) throws LuaException {
		List list = values.eval(lua);
		return list.isEmpty() ? null : list.get(0);
	}
}
