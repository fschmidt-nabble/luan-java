package luan.impl;

import luan.LuanException;
import luan.LuanTable;
import luan.LuanSource;
import luan.Luan;


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

	@Override public Object eval(LuanStateImpl luan) throws LuanException {
		LuanTable table = Luan.newTable();
		for( Field field : fields ) {
			table.put( field.key.eval(luan), field.value.eval(luan) );
		}
		Object obj = expressions.eval(luan);
		if( obj instanceof Object[] ) {
			Object[] a = (Object[])obj;
			for( int i=0; i<a.length; i++ ) {
				table.put( i+1, a[i] );
			}
		} else {
			table.put( 1, obj );
		}
		return table;
	}
}
