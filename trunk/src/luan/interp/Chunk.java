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
		Stmt stmt = block;
		while( stmt instanceof Block ) {
			Block b = (Block)stmt;
			if( b.stmts.length==0 )
				break;
			stmt = b.stmts[b.stmts.length-1];
		}
		if( stmt instanceof ReturnStmt ) {
			ReturnStmt rs = (ReturnStmt)stmt;
			rs.throwReturnException = false;
		}
	}

	public LuaClosure newClosure(LuaState lua) {
		return new LuaClosure(this,lua);
	}

	@Override public Object eval(LuaState lua) {
		return newClosure(lua);
	}

}
