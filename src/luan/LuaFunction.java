package luan;


public abstract class LuaFunction {

	public abstract Object[] call(Object... args);

	@Override public String toString() {
		return "function: " + Integer.toHexString(hashCode());
	}

}
