package luan.interp;

import luan.LuaState;
import luan.LuaException;


abstract class Expr {
	abstract Object eval(LuaState lua) throws LuaException;
}
