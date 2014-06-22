package luan;


public class LuanException extends Exception {
	private final String stackTrace;

	LuanException(LuanBit bit,Object msg) {
		super(message(msg),cause(msg));
		stackTrace = stackTrace(bit,msg);
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
		if( msg instanceof LuanException ) {
			LuanException le = (LuanException)msg;
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

	private static String stackTrace(LuanBit bit,Object msg) {
		StringBuilder buf = new StringBuilder();
		buf.append( bit.stackTrace() );
		if( msg instanceof LuanException ) {
			LuanException le = (LuanException)msg;
			buf.append( "\ncaused by:" ).append( le.stackTrace );
		}
		return buf.toString();
	}
}
