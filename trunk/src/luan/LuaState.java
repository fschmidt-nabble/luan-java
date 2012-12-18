package luan;


public interface LuaState {
	public LuaTable global();
	public String toString(Object obj) throws LuaException;
}
