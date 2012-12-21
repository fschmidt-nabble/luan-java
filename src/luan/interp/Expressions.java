package luan.interp;

import luan.LuaException;


interface Expressions extends Code {
	public Object[] eval(LuaStateImpl lua) throws LuaException;
}
