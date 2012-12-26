package luan;


public class LuaException extends Exception {
	private final String stackTrace;

	public LuaException(LuaState lua,LuaElement el,Object msg) {
		super(message(msg),cause(msg));
		stackTrace = stackTrace(lua,el,msg);
	}

	@Override public String getMessage() {
		return super.getMessage() + stackTrace;
	}

	private String message() {
		return super.getMessage();
	}

	private static Throwable cause(Object msg) {
		return msg instanceof Throwable ? (Throwable)msg : null;
	}

	private static String message(Object msg) {
		if( msg instanceof LuaException ) {
			LuaException le = (LuaException)msg;
			return le.message();
/*
		} else if( msg instanceof Throwable ) {
			Throwable t = (Throwable)msg;
			return t.getMessage();
*/
		} else {
			return msg.toString();
		}
	}

	private static String stackTrace(LuaState lua,LuaElement el,Object msg) {
		StringBuilder buf = new StringBuilder();
		for( int i  = lua.stackTrace.size() - 1; i>=0; i-- ) {
			StackTraceElement stackTraceElement = lua.stackTrace.get(i);
			buf.append( "\n\t" ).append( el.toString(stackTraceElement.fnName) );
			el = stackTraceElement.call;
		}
		if( msg instanceof LuaException ) {
			LuaException le = (LuaException)msg;
			buf.append( "\ncaused by:" ).append( le.stackTrace );
		}
		return buf.toString();
	}
}
