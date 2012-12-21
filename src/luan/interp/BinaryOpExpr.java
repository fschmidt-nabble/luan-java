package luan.interp;

import luan.Lua;
import luan.LuaTable;
import luan.LuaFunction;
import luan.LuaException;
import luan.LuaSource;


abstract class BinaryOpExpr extends CodeImpl implements Expr {
	final Expr op1;
	final Expr op2;

	BinaryOpExpr(LuaSource.Element se,Expr op1,Expr op2) {
		super(se);
		this.op1 = op1;
		this.op2 = op2;
	}

	Object arithmetic(LuaStateImpl lua,String op,Object o1,Object o2) throws LuaException {
		return lua.arithmetic(se(),"__mod",o1,o2);
	}

}
