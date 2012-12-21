package luan.interp;

import luan.LuaException;


interface Stmt {
	public void eval(LuaStateImpl lua) throws LuaException;

	static final Stmt EMPTY = new Stmt() {
		@Override public void eval(LuaStateImpl lua) {}
	};
}
