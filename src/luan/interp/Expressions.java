package luan.interp;

import luan.LuaException;


interface Expressions {
	public Object[] eval(LuaStateImpl lua) throws LuaException;
}
