package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaFunction;
import luan.LuaTable;
import luan.LuaException;
import luan.LuaSource;


final class EqExpr extends BinaryOpExpr {

	EqExpr(LuaSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		Object o1 = op1.eval(lua);
		Object o2 = op2.eval(lua);
		if( o1 == o2 || o1 != null && o1.equals(o2) )
			return true;
		if( !o1.getClass().equals(o2.getClass()) )
			return false;
		LuaTable mt1 = lua.getMetatable(o1);
		LuaTable mt2 = lua.getMetatable(o2);
		if( mt1==null || mt2==null )
			return false;
		Object f = mt1.get("__eq");
		if( f == null || !f.equals(mt2.get("__eq")) )
			return null;
		LuaFunction fn = lua.checkFunction(se,f);
		return Lua.toBoolean( Lua.first(lua.call(fn,se,"__eq",o1,o2)) );
	}
}
