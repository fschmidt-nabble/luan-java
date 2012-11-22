package luan.interp;

import luan.LuaException;
import luan.LuaState;
import luan.LuaTable;


final class TableExpr implements Expr {

	static class Field {
		final Expr key;
		final Expr value;

		Field(Expr key,Expr value) {
			this.key = key;
			this.value = value;
		}
	}

	private final Field[] fields;

	TableExpr(Field[] fields) {
		this.fields = fields;
	}

	@Override public Object eval(LuaState lua) throws LuaException {
		LuaTable table = new LuaTable();
		for( Field field : fields ) {
			table.set( field.key.eval(lua), field.value.eval(lua) );
		}
		return table;
	}
}
