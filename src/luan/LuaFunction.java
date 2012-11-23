package luan;


public abstract class LuaFunction {

	public abstract Object[] call(LuaState lua,Object... args) throws LuaException;

	public static final Object[] EMPTY_RTN = new Object[0];

	@Override public String toString() {
		return "function: " + Integer.toHexString(hashCode());
	}

}
