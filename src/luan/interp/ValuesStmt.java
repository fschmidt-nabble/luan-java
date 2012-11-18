package luan.interp;

import luan.LuaState;
import luan.LuaException;


final class ValuesStmt extends Stmt {
	private final Values values;

	ValuesStmt(Values values) {
		this.values = values;
	}

	@Override void eval(LuaState lua) throws LuaException {
		values.eval(lua);
	}

}
