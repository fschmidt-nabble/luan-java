package luan.interp;

import luan.Lua;
import luan.LuaTable;
import luan.LuaFunction;
import luan.LuaException;


abstract class BinaryOpExpr implements Expr {
	final Expr op1;
	final Expr op2;

	BinaryOpExpr(Expr op1,Expr op2) {
		this.op1 = op1;
		this.op2 = op2;
	}

}
