package luan.interp;

import luan.LuaSource;


abstract class UnaryOpExpr extends CodeImpl implements Expr {
	final Expr op;

	UnaryOpExpr(LuaSource.Element se,Expr op) {
		super(se);
		this.op = op;
	}
}
