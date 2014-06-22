package luan.impl;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import luan.LuanException;
import luan.LuanSource;
import luan.LuanFunction;
import luan.Luan;


final class ExpList {

	static final Expressions emptyExpList = new Expressions() {

		@Override public Object[] eval(LuanStateImpl luan) {
			return LuanFunction.NOTHING;
		}

		@Override public LuanSource.Element se() {
			return null;
		}
	};

	static Expr[] toArray(List<Expressions> list) {
		Expr[] a = new Expr[list.size()];
		for( int i=0; i<a.length; i++ ) {
			Expressions exprs = list.get(i);
			if( exprs instanceof Expr ) {
				a[i] = (Expr)exprs;
			} else {
				a[i] = new ExpressionsExpr(exprs);
			}
		}
		return a;
	}

	static Expressions build(List<Expressions> list) {
		switch(list.size()) {
		case 0:
			return emptyExpList;
		case 1:
			return list.get(0);
		default:
			if( list.get(list.size()-1) instanceof Expr ) {
				return new ExprList1( toArray(list) );
			} else {
				Expressions last = list.remove(list.size()-1);
				return new ExprList2( toArray(list), last );
			}
		}
	}

	private static class ExprList1 implements Expressions {
		private final Expr[] exprs;

		private ExprList1(Expr[] exprs) {
			this.exprs = exprs;
		}
	
		@Override public Object eval(LuanStateImpl luan) throws LuanException {
			Object[] a = new Object[exprs.length];
			for( int i=0; i<exprs.length; i++ ) {
				a[i] = exprs[i].eval(luan);
			}
			return a;
		}
	
		@Override public LuanSource.Element se() {
			return new LuanSource.Element(exprs[0].se().source,exprs[0].se().start,exprs[exprs.length-1].se().end);
		}
	}

	private static class ExprList2 implements Expressions {
		private final Expr[] exprs;
		private final Expressions last;
	
		private ExprList2(Expr[] exprs,Expressions last) {
			this.exprs = exprs;
			this.last = last;
		}
	
		@Override public Object eval(LuanStateImpl luan) throws LuanException {
			List<Object> list = new ArrayList<Object>();
			for( Expr expr : exprs ) {
				list.add( expr.eval(luan) );
			}
			list.addAll( Arrays.asList(Luan.array( last.eval(luan) )) );
			return list.toArray();
		}
	
		@Override public LuanSource.Element se() {
			return new LuanSource.Element(exprs[0].se().source,exprs[0].se().start,last.se().end);
		}
	}
}
