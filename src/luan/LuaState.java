package luan;


public class LuaState {
	private final LuaTable env = new LuaTable();

	public LuaTable env() {
		return env;
	}

}
