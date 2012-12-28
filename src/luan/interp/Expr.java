package luan.interp;

import luan.LuanException;


interface Expr extends Code {
	public Object eval(LuanStateImpl luan) throws LuanException;
}
