package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaException;
import luan.LuaSource;


final class PowExpr extends BinaryOpExpr {

	PowExpr(LuaSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		Object o1 = op1.eval(lua);
		Object o2 = op2.eval(lua);
		LuaNumber n1 = Lua.toNumber(o1);
		LuaNumber n2 = Lua.toNumber(o2);
		if( n1 != null && n2 != null )
			return LuaNumber.of( Math.pow( n1.value(), n2.value() ) );
		return arithmetic(lua,"__pow",o1,o2);
	}
}
