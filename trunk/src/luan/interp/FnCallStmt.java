package luan.interp;

import luan.LuanException;


final class FnCallStmt implements Stmt {
	private final FnCall fnCall;

	FnCallStmt(FnCall fnCall) {
		this.fnCall = fnCall;
	}

	@Override public void eval(LuanStateImpl luan) throws LuanException {
		fnCall.eval(luan);
	}

}
