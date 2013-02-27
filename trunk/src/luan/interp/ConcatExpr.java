package luan.interp;

import luan.Luan;
import luan.LuanFunction;
import luan.LuanException;
import luan.LuanSource;


final class ConcatExpr extends BinaryOpExpr {

	ConcatExpr(LuanSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuanStateImpl luan) throws LuanException {
		Object o1 = op1.eval(luan);
		Object o2 = op2.eval(luan);
		String s1 = luan.bit(op1.se()).toString(o1);
		String s2 = luan.bit(op2.se()).toString(o2);
/*
		if( s1 != null && s2 != null )
			return s1 + s2;
		LuanFunction fn = luan.getBinHandler(se,"__concat",o1,o2);
		if( fn != null )
			return Luan.first(luan.call(fn,se,"__concat",o1,o2));
		String type = s1==null ? Luan.type(o1) : Luan.type(o2);
		throw new LuanException( luan, se, "attempt to concatenate a " + type + " value" );
*/
		return s1 + s2;
	}
}
