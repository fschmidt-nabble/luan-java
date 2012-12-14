package luan.interp;

import luan.LuaException;
import luan.LuaTable;
import luan.LuaNumber;


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
	private final Expressions expressions;

	TableExpr(Field[] fields,Expressions expressions) {
		this.fields = fields;
		this.expressions = expressions;
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		LuaTable table = new LuaTable();
		for( Field field : fields ) {
			table.set( field.key.eval(lua), field.value.eval(lua) );
		}
		Object[] a = expressions.eval(lua);
		for( int i=0; i<a.length; i++ ) {
			table.set( new LuaNumber(i+1), a[i] );
		}
		return table;
	}
}