package luan.interp;

import java.util.List;
import java.util.ArrayList;
import luan.LuaException;


final class ExpList extends Values {

	private interface Adder {
		public void addTo(List<Object> list) throws LuaException;
	}

	private static class ExprAdder implements Adder {
		private final Expr expr;

		ExprAdder(Expr expr) {
			this.expr = expr;
		}

		public void addTo(List<Object> list) throws LuaException {
			list.add( expr.eval() );
		}

	}

	private static class ValuesAdder implements Adder {
		private final Values values;

		ValuesAdder(Values values) {
			this.values = values;
		}

		public void addTo(List<Object> list) throws LuaException {
			for( Object val : values.eval() ) {
				list.add( val );
			}
		}

	}

	static class Builder {
		private final List<Adder> adders = new ArrayList<Adder>();

		void add(Expr expr) {
			adders.add( new ExprAdder(expr) );
		}

		void add(Values values) {
			adders.add( new ValuesAdder(values) );
		}

		ExpList build() {
			return new ExpList( adders.toArray(new Adder[0]) );
		}
	}

	private final Adder[] adders;

	private ExpList(Adder[] adders) {
		this.adders = adders;
	}

	List eval() throws LuaException {
		List<Object> list = new ArrayList<Object>();
		for( Adder adder : adders ) {
			adder.addTo(list);
		}
		return list;
	}
}
