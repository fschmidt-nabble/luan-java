package luan.interp;

import luan.LuaException;


interface Expr extends Code {
	public Object eval(LuaStateImpl lua) throws LuaException;
}
