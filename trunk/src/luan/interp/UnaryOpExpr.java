package luan.interp;

import luan.LuanSource;


abstract class UnaryOpExpr extends CodeImpl implements Expr {
	final Expr op;

	UnaryOpExpr(LuanSource.Element se,Expr op) {
		super(se);
		this.op = op;
	}
}
