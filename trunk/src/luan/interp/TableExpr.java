package luan.interp;

import luan.LuanException;
import luan.LuanTable;
import luan.LuanSource;


final class TableExpr extends CodeImpl implements Expr {

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

	TableExpr(LuanSource.Element se,Field[] fields,Expressions expressions) {
		super(se);
		this.fields = fields;
		this.expressions = expressions;
	}

	@Override public Object eval(LuanStateImpl lua) throws LuanException {
		LuanTable table = new LuanTable();
		for( Field field : fields ) {
			table.put( field.key.eval(lua), field.value.eval(lua) );
		}
		Object[] a = expressions.eval(lua);
		for( int i=0; i<a.length; i++ ) {
			table.put( i+1, a[i] );
		}
		return table;
	}
}
