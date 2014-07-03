package luan.impl;

import luan.LuanException;
import luan.LuanSource;


final class FnDef extends CodeImpl implements Expr {
	final Stmt block;
	final int stackSize;
	final int numArgs;
	final boolean isVarArg;
	final UpValue.Getter[] upValueGetters;

	FnDef(LuanSource.Element se,Stmt block,int stackSize,int numArgs,boolean isVarArg,UpValue.Getter[] upValueGetters) {
		super(se);
		this.block = block;
		this.stackSize = stackSize;
		this.numArgs = numArgs;
		this.isVarArg = isVarArg;
		this.upValueGetters = upValueGetters;
		fixReturns(block);
	}

	private static void fixReturns(Stmt stmt) {
		if( stmt instanceof ReturnStmt ) {
			ReturnStmt rs = (ReturnStmt)stmt;
			rs.throwReturnException = false;
		} else if( stmt instanceof Block ) {
			Block b = (Block)stmt;
			fixReturns( b.stmts[b.stmts.length-1] );
		} else if( stmt instanceof IfStmt ) {
			IfStmt is = (IfStmt)stmt;
			fixReturns( is.thenStmt );
			fixReturns( is.elseStmt );
		}
	}

	@Override public Object eval(LuanStateImpl luan) throws LuanException {
		return new Closure(luan,this,luan.mtGetterLink());
	}

}
