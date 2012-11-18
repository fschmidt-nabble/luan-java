package luan.interp;

import luan.LuaState;
import luan.LuaException;


abstract class Stmt {
	abstract void eval(LuaState lua) throws LuaException;
}
