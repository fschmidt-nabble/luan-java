package luan.lib;

import luan.LuaException;


public final class LuaRuntimeException extends RuntimeException {
	public LuaRuntimeException(LuaException e) {
		super(e);
	}
}
