package luan.interp;

import luan.LuaException;


interface Expr {
	public Object eval(LuaStateImpl lua) throws LuaException;
}
