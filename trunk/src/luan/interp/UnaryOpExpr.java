package luan.interp;


abstract class UnaryOpExpr implements Expr {
	final Expr op;

	UnaryOpExpr(Expr op) {
		this.op = op;
	}
}
