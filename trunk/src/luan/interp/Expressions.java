package luan.interp;

import luan.LuanException;


interface Expressions extends Code {
	public Object[] eval(LuanStateImpl lua) throws LuanException;
}
