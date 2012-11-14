package luan.interp;

import java.util.List;
import luan.LuaException;


abstract class Values {
	abstract List eval() throws LuaException;
}
