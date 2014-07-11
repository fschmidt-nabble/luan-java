package luan.modules.logging;

import org.apache.log4j.Logger;
import luan.LuanState;
import luan.LuanException;
import luan.LuanTable;
import luan.LuanJavaFunction;


public final class LuanLogger {
	private final Logger logger;

	public LuanLogger(Logger logger) {
		this.logger = logger;
	}

	public void error(LuanState luan,Object obj) throws LuanException {
		logger.error( luan.toString(obj) );
	}

	public void warn(LuanState luan,Object obj) throws LuanException {
		logger.warn( luan.toString(obj) );
	}

	public void info(LuanState luan,Object obj) throws LuanException {
		logger.info( luan.toString(obj) );
	}

	public void debug(LuanState luan,Object obj) throws LuanException {
		logger.debug( luan.toString(obj) );
	}

	public LuanTable table() {
		LuanTable tbl = new LuanTable();
		try {
			tbl.put( "error", new LuanJavaFunction(
				LuanLogger.class.getMethod( "error", LuanState.class, Object.class ), this
			) );
			tbl.put( "warn", new LuanJavaFunction(
				LuanLogger.class.getMethod( "warn", LuanState.class, Object.class ), this
			) );
			tbl.put( "info", new LuanJavaFunction(
				LuanLogger.class.getMethod( "info", LuanState.class, Object.class ), this
			) );
			tbl.put( "debug", new LuanJavaFunction(
				LuanLogger.class.getMethod( "debug", LuanState.class, Object.class ), this
			) );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return tbl;
	}
}
