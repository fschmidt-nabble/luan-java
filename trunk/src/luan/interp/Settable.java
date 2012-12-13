package luan.interp;

import luan.LuaException;


interface Settable {
	public void set(LuaStateImpl lua,Object value) throws LuaException;
}
