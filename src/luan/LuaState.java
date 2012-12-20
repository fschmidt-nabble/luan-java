package luan;


public interface LuaState {
	public LuaTable global();
	public String toString(Object obj) throws LuaException;
	public LuaTable getMetatable(Object obj);
	public void addMetatableGetter(MetatableGetter mg);
}
