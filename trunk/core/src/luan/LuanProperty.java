package luan;


public abstract class LuanProperty {

	public abstract Object get();

	// return whether handled.  if not handled, then this object will be replaced
	public boolean set(Object value) {
		return false;
	}

}
