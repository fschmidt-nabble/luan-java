package luan.impl;

import luan.Luan;
import luan.LuanFunction;
import luan.LuanTable;
import luan.LuanException;
import luan.LuanSource;
import luan.LuanBit;


final class EqExpr extends BinaryOpExpr {

	EqExpr(LuanSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuanStateImpl luan) throws LuanException {
		return eq(luan);
	}

	private boolean eq(LuanStateImpl luan) throws LuanException {
		Object o1 = op1.eval(luan);
		Object o2 = op2.eval(luan);
		if( o1 == o2 || o1 != null && o1.equals(o2) )
			return true;
		if( o1 instanceof Number && o2 instanceof Number ) {
			Number n1 = (Number)o1;
			Number n2 = (Number)o2;
			return n1.doubleValue() == n2.doubleValue();
		}
		if( o1==null || o2==null || !o1.getClass().equals(o2.getClass()) )
			return false;
		LuanTable mt1 = luan.getMetatable(o1);
		LuanTable mt2 = luan.getMetatable(o2);
		if( mt1==null || mt2==null )
			return false;
		Object f = mt1.get("__eq");
		if( f == null || !f.equals(mt2.get("__eq")) )
			return false;
		LuanBit bit = luan.bit(se);
		LuanFunction fn = bit.checkFunction(f);
		return Luan.toBoolean( Luan.first(bit.call(fn,"__eq",new Object[]{o1,o2})) );
	}

	@Override public String toString() {
		return "(EqExpr "+op1+" "+op2+")";
	}
}
