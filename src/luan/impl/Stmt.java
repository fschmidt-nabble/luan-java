package luan.impl;

import luan.LuanException;


interface Stmt {
	public void eval(LuanStateImpl luan) throws LuanException;

	static final Stmt EMPTY = new Stmt() {
		@Override public void eval(LuanStateImpl luan) {}
	};
}
