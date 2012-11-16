package luan.interp;

import java.util.List;
import luan.LuaException;
import luan.LuaState;


abstract class Values {
	abstract List eval(LuaState lua) throws LuaException;
}
