package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaException;


final class SubExpr extends BinaryOpExpr {

	SubExpr(Expr op1,Expr op2) {
		super(op1,op2);
	}

	@Override Object eval() throws LuaException {
		double n1 = Lua.toNumber(op1.eval()).value();
		double n2 = Lua.toNumber(op2.eval()).value();
		return new LuaNumber( n1 - n2 );
	}
}
