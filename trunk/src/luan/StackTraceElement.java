package luan;


final class StackTraceElement {
	final LuaElement call;
	final String fnName;

	StackTraceElement(LuaElement call,String fnName) {
		this.call = call;
		this.fnName = fnName;
	}
}
