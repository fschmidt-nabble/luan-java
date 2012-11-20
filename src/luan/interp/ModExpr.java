package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaException;
import luan.LuaState;


final class ModExpr extends BinaryOpExpr {

	ModExpr(Expr op1,Expr op2) {
		super(op1,op2);
	}

	@Override Object eval(LuaState lua) throws LuaException {
		double n1 = Lua.checkNumber(op1.eval(lua)).value();
		double n2 = Lua.checkNumber(op2.eval(lua)).value();
		return new LuaNumber( n1 % n2 );
	}
}
