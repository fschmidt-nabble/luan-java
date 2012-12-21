package luan;


public class LuaException extends Exception {

	public LuaException(LuaState lua,LuaElement el,String msg) {
		super(hideNull(msg)+stackTrace(lua,el));
	}

	public LuaException(LuaState lua,LuaElement el,Exception cause) {
		super(hideNull(cause.getMessage())+stackTrace(lua,el),cause);
	}

	private static String hideNull(String s) {
		return s==null ? "" : s;
	}

	private static String stackTrace(LuaState lua,LuaElement el) {
		StringBuilder buf = new StringBuilder();
		int i = lua.stackTrace.size() - 1;
		do {
			StackTraceElement stackTraceElement = lua.stackTrace.get(i);
			buf.append( "\n\t" ).append( el.toString(stackTraceElement.fnName) );
			el = stackTraceElement.call;
		} while( --i >= 0 );
		return buf.toString();
	}
}
