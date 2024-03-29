package luan;


public abstract class LuanFunction implements LuanRepr {

	public abstract Object call(LuanState luan,Object[] args) throws LuanException;

	public static final Object[] NOTHING = new Object[0];

	@Override public String toString() {
		return "function: " + Integer.toHexString(hashCode());
	}

	@Override public String repr() {
		return "<" + toString() + ">";
	}

}
