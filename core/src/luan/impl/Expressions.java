package luan.impl;

import luan.LuanException;


interface Expressions extends Code {
	public Object eval(LuanStateImpl luan) throws LuanException;
}
