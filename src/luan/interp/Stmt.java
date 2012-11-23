package luan.interp;

import luan.LuaState;
import luan.LuaException;


public interface Stmt {
	public void eval(LuaState lua) throws LuaException;

	static final Stmt EMPTY = new Stmt() {
		public void eval(LuaState lua) {}
	};
}
