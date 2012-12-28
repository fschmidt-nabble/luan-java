package luan.interp;

import luan.LuanException;


interface Settable {
	public void set(LuanStateImpl luan,Object value) throws LuanException;
}
