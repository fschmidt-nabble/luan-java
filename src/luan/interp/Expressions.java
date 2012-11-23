package luan.interp;

import luan.LuaException;
import luan.LuaState;


public interface Expressions {
	public Object[] eval(LuaState lua) throws LuaException;
}
