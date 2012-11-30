package luan.interp;

import luan.LuaState;
import luan.LuaException;


interface Settable {
	public void set(LuaState lua,Object value) throws LuaException;
}
