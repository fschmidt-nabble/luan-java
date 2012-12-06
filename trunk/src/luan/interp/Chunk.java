package luan.interp;

import luan.LuaState;
import luan.LuaException;
import luan.LuaClosure;


public final class Chunk implements Expr {
	public final Stmt block;
	public final int stackSize;
	public final int numArgs;

	Chunk(Stmt block,int stackSize,int numArgs) {
		this.block = block;
		this.stackSize = stackSize;
		this.numArgs = numArgs;
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

	public LuaClosure newClosure(LuaState lua) {
		return new LuaClosure(this,lua);
	}

	@Override public Object eval(LuaState lua) {
		return newClosure(lua);
	}

}
