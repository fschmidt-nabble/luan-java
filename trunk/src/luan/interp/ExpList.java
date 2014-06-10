package luan.interp;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import luan.LuanException;
import luan.LuanSource;


final class ExpList implements Expressions {

	private interface Adder {
		public void addTo(LuanStateImpl luan,List<Object> list) throws LuanException;
		public Code code();
	}

	private static class ExprAdder implements Adder {
		private final Expr expr;

		ExprAdder(Expr expr) {
			this.expr = expr;
		}

		public void addTo(LuanStateImpl luan,List<Object> list) throws LuanException {
			list.add( expr.eval(luan) );
		}

		public Code code() {
			return expr;
		}

	}

	private static class ExpressionsAdder implements Adder {
		private final Expressions expressions;

		ExpressionsAdder(Expressions expressions) {
			this.expressions = expressions;
		}

		public void addTo(LuanStateImpl luan,List<Object> list) throws LuanException {
			Object obj = expressions.eval(luan);
			if( obj instanceof Object[] ) {
				for( Object val : (Object[])obj ) {
					list.add( val );
				}
			} else {
				list.add(obj);
			}
		}

		public Code code() {
			return expressions;
		}

	}

	static class Builder {
		private final List<Adder> adders = new ArrayList<Adder>();

		void add(Expr expr) {
			if( expr==null )
				throw new NullPointerException();
			adders.add( new ExprAdder(expr) );
		}

		void add(Expressions expressions) {
			adders.add( new ExpressionsAdder(expressions) );
		}

		void add(Code code) {
			if( code instanceof Expr ) {
				add((Expr)code);
			} else {
				add((Expressions)code);
			}
		}

		Expressions build() {
			int size = adders.size();
			if( size == 0 )
				return emptyExpList;
			if( size == 1 ) {
				Adder adder = adders.get(0);
				if( adder instanceof ExpressionsAdder ) {
					ExpressionsAdder ea = (ExpressionsAdder)adder;
					return ea.expressions;
				}
				ExprAdder ea = (ExprAdder)adder;
				return new SingleExpList(ea.expr);
			}
			Adder[] a = adders.toArray(new Adder[size]);
			for( int i=0; i<size-1; i++ ) {
				Adder adder = a[i];
				if( adder instanceof ExpressionsAdder ) {
					a[i] = new ExprAdder(new ExpressionsExpr(((ExpressionsAdder)adder).expressions));
				}
			}
			return new ExpList(a);
		}
	}

	private static final Object[] EMPTY = new Object[0];

	static final Expressions emptyExpList = new Expressions() {

		@Override public Object[] eval(LuanStateImpl luan) {
			return EMPTY;
		}

		@Override public LuanSource.Element se() {
			return null;
		}
	};

	static class SingleExpList implements Expressions {
		private final Expr expr;

		SingleExpList(Expr expr) {
			this.expr = expr;
		}

		@Override public Object eval(LuanStateImpl luan) throws LuanException {
//System.out.println("SingleExpList "+expr);
			return expr.eval(luan);
		}

		@Override public LuanSource.Element se() {
			return expr.se();
		}

		@Override public String toString() {
			return "(SingleExpList "+expr+")";
		}
	}

	private final Adder[] adders;

	private ExpList(Adder[] adders) {
		this.adders = adders;
	}

	@Override public Object eval(LuanStateImpl luan) throws LuanException {
		List<Object> list = new ArrayList<Object>();
		for( Adder adder : adders ) {
			adder.addTo(luan,list);
		}
		switch( list.size() ) {
		case 0:
			return EMPTY;
		case 1:
			return list.get(0);
		default:
			return list.toArray();
		}
	}

	@Override public LuanSource.Element se() {
		return new LuanSource.Element(adders[0].code().se().source,adders[0].code().se().start,adders[adders.length-1].code().se().end);
	}
}
