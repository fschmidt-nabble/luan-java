package luan.interp;

import luan.Lua;
import luan.LuaNumber;
import luan.LuaTable;
import luan.LuaException;


final class LengthExpr extends UnaryOpExpr {

	LengthExpr(Expr op) {
		super(op);
	}

	@Override Object eval() throws LuaException {
		return new LuaNumber( length(op.eval()) );
	}

	private static int length(Object obj) throws LuaException {
		if( obj instanceof String ) {
			String s = (String)obj;
			return s.length();
		}
		if( obj instanceof LuaTable ) {
			LuaTable t = (LuaTable)obj;
			return t.length();
		}
		throw new LuaException( "attempt to get length of a " + Lua.type(obj) + " value" );
	}
}
