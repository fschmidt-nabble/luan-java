package luan.interp;

import luan.Luan;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanException;
import luan.LuanSource;


abstract class BinaryOpExpr extends CodeImpl implements Expr {
	final Expr op1;
	final Expr op2;

	BinaryOpExpr(LuanSource.Element se,Expr op1,Expr op2) {
		super(se);
		this.op1 = op1;
		this.op2 = op2;
	}

	Object arithmetic(LuanStateImpl lua,String op,Object o1,Object o2) throws LuanException {
		return lua.arithmetic(se(),"__mod",o1,o2);
	}

}
