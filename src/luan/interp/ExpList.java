package luan.interp;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import luan.LuaException;


final class ExpList implements Expressions {

	private interface Adder {
		public void addTo(LuaStateImpl lua,List<Object> list) throws LuaException;
	}

	private static class ExprAdder implements Adder {
		private final Expr expr;

		ExprAdder(Expr expr) {
			this.expr = expr;
		}

		public void addTo(LuaStateImpl lua,List<Object> list) throws LuaException {
			list.add( expr.eval(lua) );
		}

	}

	private static class ExpressionsAdder implements Adder {
		private final Expressions expressions;

		ExpressionsAdder(Expressions expressions) {
			this.expressions = expressions;
		}

		public void addTo(LuaStateImpl lua,List<Object> list) throws LuaException {
			for( Object val : expressions.eval(lua) ) {
				list.add( val );
			}
		}

	}

	static class Builder {
		private final List<Adder> adders = new ArrayList<Adder>();

		void add(Expr expr) {
			adders.add( new ExprAdder(expr) );
		}

		void add(Expressions expressions) {
			adders.add( new ExpressionsAdder(expressions) );
		}

		Expressions build() {
			if( adders.isEmpty() )
				return emptyExpList;
			if( adders.size() == 1 ) {
				Adder adder = adders.get(0);
				if( adder instanceof ExpressionsAdder ) {
					ExpressionsAdder ea = (ExpressionsAdder)adder;
					return ea.expressions;
				}
				ExprAdder ea = (ExprAdder)adder;
				return new SingleExpList(ea.expr);
			}
			return new ExpList( adders.toArray(new Adder[0]) );
		}
	}

	private static final Object[] EMPTY = new Object[0];

	static final Expressions emptyExpList = new Expressions() {
		@Override public Object[] eval(LuaStateImpl lua) {
			return EMPTY;
		}
	};

	static class SingleExpList implements Expressions {
		private final Expr expr;

		SingleExpList(Expr expr) {
			this.expr = expr;
		}

		@Override public Object[] eval(LuaStateImpl lua) throws LuaException {
			return new Object[]{expr.eval(lua)};
		}
	}

	private final Adder[] adders;

	private ExpList(Adder[] adders) {
		this.adders = adders;
	}

	@Override public Object[] eval(LuaStateImpl lua) throws LuaException {
		List<Object> list = new ArrayList<Object>();
		for( Adder adder : adders ) {
			adder.addTo(lua,list);
		}
		return list.toArray();
	}
}
