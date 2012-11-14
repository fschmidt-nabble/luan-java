package luan.interp;

import luan.LuaException;


abstract class Expr {
	abstract Object eval() throws LuaException;
}
