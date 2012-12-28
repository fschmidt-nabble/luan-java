package luan.interp;

import luan.Luan;
import luan.LuanFunction;
import luan.LuanException;
import luan.LuanSource;


final class ConcatExpr extends BinaryOpExpr {

	ConcatExpr(LuanSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuanStateImpl lua) throws LuanException {
		Object o1 = op1.eval(lua);
		Object o2 = op2.eval(lua);
		String s1 = Luan.asString(o1);
		String s2 = Luan.asString(o2);
		if( s1 != null && s2 != null )
			return s1 + s2;
		LuanFunction fn = lua.getBinHandler(se,"__concat",o1,o2);
		if( fn != null )
			return Luan.first(lua.call(fn,se,"__concat",o1,o2));
		String type = s1==null ? Luan.type(o1) : Luan.type(o2);
		throw new LuanException( lua, se, "attempt to concatenate a " + type + " value" );
	}
}
