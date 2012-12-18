package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaException;


final class ModExpr extends BinaryOpExpr {

	ModExpr(Expr op1,Expr op2) {
		super(op1,op2);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		Object o1 = op1.eval(lua);
		Object o2 = op2.eval(lua);
		LuaNumber n1 = Lua.toNumber(o1);
		LuaNumber n2 = Lua.toNumber(o2);
		if( n1 != null && n2 != null )
			return new LuaNumber( n1.value() % n2.value() );
		return arithmetic(lua,"__mod",o1,o2);
	}
}
