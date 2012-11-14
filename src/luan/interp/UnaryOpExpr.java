package luan.interp;


abstract class UnaryOpExpr extends Expr {
	final Expr op;

	UnaryOpExpr(Expr op) {
		this.op = op;
	}
}
