package luan.impl;

import luan.Luan;
import luan.LuanFunction;
import luan.LuanException;
import luan.LuanSource;
import luan.LuanBit;


final class LeExpr extends BinaryOpExpr {

	LeExpr(LuanSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuanStateImpl luan) throws LuanException {
		Object o1 = op1.eval(luan);
		Object o2 = op2.eval(luan);
		if( o1 instanceof Number && o2 instanceof Number ) {
			Number n1 = (Number)o1;
			Number n2 = (Number)o2;
			return n1.doubleValue() <= n2.doubleValue();
		}
		if( o1 instanceof String && o2 instanceof String ) {
			String s1 = (String)o1;
			String s2 = (String)o2;
			return s1.compareTo(s2) <= 0;
		}
		LuanBit bit = luan.bit(se);
		LuanFunction fn = bit.getBinHandler("__le",o1,o2);
		if( fn != null )
			return Luan.toBoolean( Luan.first(bit.call(fn,"__le",new Object[]{o1,o2})) );
		fn = bit.getBinHandler("__lt",o1,o2);
		if( fn != null )
			return !Luan.toBoolean( Luan.first(bit.call(fn,"__lt",new Object[]{o2,o1})) );
		throw bit.exception( "attempt to compare " + Luan.type(o1) + " with " + Luan.type(o2) );
	}
}
