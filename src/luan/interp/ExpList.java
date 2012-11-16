package luan.interp;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import luan.LuaException;
import luan.LuaState;


final class ExpList extends Values {

	private interface Adder {
		public void addTo(LuaState lua,List<Object> list) throws LuaException;
	}

	private static class ExprAdder implements Adder {
		private final Expr expr;

		ExprAdder(Expr expr) {
			this.expr = expr;
		}

		public void addTo(LuaState lua,List<Object> list) throws LuaException {
			list.add( expr.eval(lua) );
		}

	}

	private static class ValuesAdder implements Adder {
		private final Values values;

		ValuesAdder(Values values) {
			this.values = values;
		}

		public void addTo(LuaState lua,List<Object> list) throws LuaException {
			for( Object val : values.eval(lua) ) {
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

		Values build() {
			if( adders.isEmpty() )
				return emptyExpList;
			if( adders.size() == 1 ) {
				Adder adder = adders.get(0);
				if( adder instanceof ValuesAdder ) {
					ValuesAdder va = (ValuesAdder)adder;
					return va.values;
				}
				ExprAdder ea = (ExprAdder)adder;
				return new SingleExpList(ea.expr);
			}
			return new ExpList( adders.toArray(new Adder[0]) );
		}
	}

	private static final Values emptyExpList = new Values() {
		List eval(LuaState lua) {
			return Collections.emptyList();
		}
	};

	private static class SingleExpList extends Values {
		private final Expr expr;

		SingleExpList(Expr expr) {
			this.expr = expr;
		}

		List eval(LuaState lua) throws LuaException {
			return Collections.singletonList( expr.eval(lua) );
		}
	}

	private final Adder[] adders;

	private ExpList(Adder[] adders) {
		this.adders = adders;
	}

	List eval(LuaState lua) throws LuaException {
		List<Object> list = new ArrayList<Object>();
		for( Adder adder : adders ) {
			adder.addTo(lua,list);
		}
		return list;
	}
}
