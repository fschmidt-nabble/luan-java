package luan;


public abstract class LuanFunction {

	public abstract Object[] call(LuanState luan,Object[] args) throws LuanException;

	public static final Object[] EMPTY_RTN = new Object[0];

	@Override public String toString() {
		return "function: " + Integer.toHexString(hashCode());
	}

}
