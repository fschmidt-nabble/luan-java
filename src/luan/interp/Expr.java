package luan.interp;

import luan.LuaState;
import luan.LuaException;


interface Expr {
	public Object eval(LuaState lua) throws LuaException;
}
