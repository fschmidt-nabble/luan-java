package luan.interp;

import luan.LuanException;


interface Settable {
	public void set(LuanStateImpl lua,Object value) throws LuanException;
}
