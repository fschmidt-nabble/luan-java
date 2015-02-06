package luan.impl;

import luan.LuanException;
import luan.modules.JavaLuan;


final class JavaStmt implements Stmt {

	@Override public void eval(LuanStateImpl luan) throws LuanException {
		JavaLuan.java(luan);
	}
}
