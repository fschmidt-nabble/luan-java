package luan.interp;

import luan.Lua;
import luan.LuaException;
import luan.LuaTable;
import luan.LuaFunction;
import luan.LuaSource;


final class IndexExpr extends BinaryOpExpr {

	IndexExpr(LuaSource.Element se,Expr op1,Expr op2) {
		super(se,op1,op2);
	}

	@Override public Object eval(LuaStateImpl lua) throws LuaException {
		return index(lua,op1.eval(lua),op2.eval(lua));
	}

	private Object index(LuaStateImpl lua,Object t,Object key) throws LuaException {
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
				throw new LuaException( lua, se, "attempt to index a " + Lua.type(t) + " value" );
		}
		if( h instanceof LuaFunction ) {
			LuaFunction fn = (LuaFunction)h;
			return Lua.first(lua.call(fn,se,"__index",t,key));
		}
		return index(lua,h,key);
	}
}
