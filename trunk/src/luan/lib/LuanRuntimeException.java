package luan.lib;

import luan.LuanException;


public final class LuanRuntimeException extends RuntimeException {
	public LuanRuntimeException(LuanException e) {
		super(e);
	}
}
