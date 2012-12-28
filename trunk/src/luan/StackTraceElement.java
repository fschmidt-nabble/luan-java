package luan;


final class StackTraceElement {
	final LuanElement call;
	final String fnName;

	StackTraceElement(LuanElement call,String fnName) {
		this.call = call;
		this.fnName = fnName;
	}
}
