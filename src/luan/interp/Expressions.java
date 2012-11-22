package luan.interp;

import java.util.List;
import luan.LuaException;
import luan.LuaState;


public interface Expressions {
	public List eval(LuaState lua) throws LuaException;
}
