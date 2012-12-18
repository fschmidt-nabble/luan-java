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

	static final LuaFunction getBinHandler(String op,Object o1,Object o2) throws LuaException {
		LuaFunction f1 = Utils.getHandlerFunction(op,o1);
		if( f1 != null )
			return f1;
		return Utils.getHandlerFunction(op,o2);
	}

	static final Object arithmetic(LuaStateImpl lua,String op,Object o1,Object o2) throws LuaException {
		LuaFunction fn = getBinHandler(op,o1,o2);
		if( fn != null )
			return Utils.first(fn.call(lua,o1,o2));
		String type = Lua.toNumber(o1)==null ? Lua.type(o1) : Lua.type(o2);
		throw new LuaException("attempt to perform arithmetic on a "+type+" value");
	}
}
