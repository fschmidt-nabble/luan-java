package luan;


public abstract class LuanLoader extends LuanFunction {

	protected abstract void load(LuanState luan) throws LuanException;

	@Override public final Object[] call(LuanState luan,Object[] args) throws LuanException {
		load(luan);
		return EMPTY;
	}
}
