package luan.interp;

import luan.Lua;
import luan.LuaException;
import luan.LuaTable;
import luan.LuaFunction;


final class IndexExpr extends BinaryOpExpr {

	IndexExpr(Expr op1,Expr op2) {
		super(op1,op2);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		return index(lua,op1.eval(lua),op2.eval(lua));
	}

	private static Object index(LuaStateImpl lua,Object t,Object key) throws LuaException {
		Object h;
		if( t instanceof LuaTable ) {
			LuaTable tbl = (LuaTable)t;
			Object value = tbl.get(key);
			if( value != null )
				return value;
			h = lua.getHandler("__index",t);
			if( h==null )
				return null;
		} else {
			h = lua.getHandler("__index",t);
			if( h==null )
				throw new LuaException( "attempt to index a " + Lua.type(t) + " value" );
		}
		if( h instanceof LuaFunction ) {
			LuaFunction fn = (LuaFunction)h;
			return Utils.first(fn.call(lua,t,key));
		}
		return index(lua,h,key);
	}
}
