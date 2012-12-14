package luan.interp;

import luan.LuaException;


final class Chunk implements Expr {
	final Stmt block;
	final int stackSize;
	final int numArgs;
	final boolean isVarArg;
	final UpValue.Getter[] upValueGetters;

	Chunk(Stmt block,int stackSize,int numArgs,boolean isVarArg,UpValue.Getter[] upValueGetters) {
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

	LuaClosure newClosure(LuaStateImpl lua) {
		return new LuaClosure(this,lua);
	}

	@Override public Object eval(LuaStateImpl lua) {
		return newClosure(lua);
	}

}
